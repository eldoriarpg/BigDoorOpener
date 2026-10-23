/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Moves the doors of a configuration written by 2.x into {@code doors.yml}.
 * <p>
 * 2.x stored the doors with the Bukkit serialization inside {@code config.yml}. Every serialized object carries its
 * alias in a {@code ==} key. The file is read as a plain YAML tree, so none of the old classes are needed:
 * <ul>
 *     <li>a door loses its alias, its {@code conditionBag} (or the older {@code conditionChain}) becomes the list
 *     {@code conditions}</li>
 *     <li>the alias of a condition becomes its {@code type}, which is the type name the condition is registered
 *     with</li>
 *     <li>vectors are written in the format of jackson-bukkit</li>
 *     <li>item stacks keep the Bukkit format, which jackson-bukkit still reads</li>
 * </ul>
 * The original file is kept as {@code config.yml.2.x.bak}. The settings in it are read by {@link
 * de.eldoria.bigdoorsopener.config.elements.Settings} as they are. The message files of 2.x use {@code %KEY%}
 * placeholders and are set aside as {@code messages.2.x.bak}, so current ones are created. This has to run before the
 * localizer reads the message files.
 */
public final class LegacyDoorMigration {
    public static final String BACKUP = "config.yml.2.x.bak";
    public static final String MESSAGES_BACKUP = "messages.2.x.bak";
    private static final String ALIAS = "==";
    private static final Set<String> VECTORS =
            Set.of("Vector", "BlockVector", "org.bukkit.util.Vector", "org.bukkit.util.BlockVector");
    private static final List<String> CHAIN_SLOTS =
            List.of("item", "location", "permission", "time", "weather", "placeholder", "mythicMob");
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private LegacyDoorMigration() {}

    /**
     * Migrates the configuration in the directory if it was written by 2.x.
     *
     * @param directory data folder of the plugin
     * @param logger    logger for the migration summary
     * @return true if a migration happened
     * @throws IOException           when a file can not be read or written
     * @throws IllegalStateException when the configuration was written by 1.x
     */
    public static boolean migrate(Path directory, Logger logger) throws IOException {
        Path config = directory.resolve("config.yml");
        if (!Files.exists(config)) return false;
        JsonNode root = YAML.readTree(config.toFile());
        if (root == null || !root.isObject()) return false;
        if (!root.has("version")) {
            throw new IllegalStateException(
                    "The configuration was written by Big Doors Opener 1.x. Update to 2.5.3 first, start the server once, then update to this version.");
        }
        if (root.get("version").asInt() != 1) return false;

        Files.copy(config, directory.resolve(BACKUP), StandardCopyOption.REPLACE_EXISTING);
        ObjectNode doors = migrateDoors(root.get("doors"));
        YAML.writeValue(directory.resolve("doors.yml").toFile(), doors);
        logger.info("Moved " + doors.get("doors").size()
                + " door(s) from config.yml to doors.yml. The old file is kept as " + BACKUP + ".");
        moveMessages(directory, logger);
        return true;
    }

    private static void moveMessages(Path directory, Logger logger) throws IOException {
        Path messages = directory.resolve("messages");
        Path backup = directory.resolve(MESSAGES_BACKUP);
        if (!Files.isDirectory(messages) || Files.exists(backup)) return;
        Files.move(messages, backup);
        logger.info("The message files of 2.x use another placeholder format and are kept as " + MESSAGES_BACKUP
                + ". New message files are created.");
    }

    /**
     * Converts the doors of a 2.x configuration into the content of {@code doors.yml}.
     *
     * @param doors the {@code doors} list of the old configuration, may be null
     * @return content of the doors file
     */
    public static ObjectNode migrateDoors(JsonNode doors) {
        ObjectNode file = JsonNodeFactory.instance.objectNode();
        ArrayNode list = file.putArray("doors");
        if (doors == null || !doors.isArray()) return file;
        for (JsonNode door : doors) {
            if (door.isObject()) list.add(migrateDoor((ObjectNode) door));
        }
        return file;
    }

    private static ObjectNode migrateDoor(ObjectNode door) {
        ObjectNode result = door.deepCopy();
        result.remove(ALIAS);
        ArrayNode conditions = JsonNodeFactory.instance.arrayNode();
        JsonNode bag = result.remove("conditionBag");
        if (bag != null && bag.get("conditions") instanceof ArrayNode bagConditions) {
            bagConditions.forEach(condition -> addCondition(conditions, condition));
        }
        JsonNode chain = result.remove("conditionChain");
        if (chain != null && chain.isObject()) {
            CHAIN_SLOTS.forEach(slot -> addCondition(conditions, chain.get(slot)));
        }
        result.set("conditions", conditions);
        convertVectors(result);
        return result;
    }

    private static void addCondition(ArrayNode conditions, JsonNode condition) {
        if (condition == null || !condition.isObject()) return;
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JsonNode alias = condition.get(ALIAS);
        if (alias != null) result.put("type", alias.asString());
        for (Map.Entry<String, JsonNode> property : condition.properties()) {
            if (!property.getKey().equals(ALIAS))
                result.set(property.getKey(), property.getValue().deepCopy());
        }
        JsonNode form = result.get("proximityForm");
        if (form != null && form.isString()) {
            result.put("proximityForm", form.asString().replaceAll("(?i)elipsoid", "ellipsoid"));
        }
        conditions.add(convertVectors(result));
    }

    private static ObjectNode convertVectors(ObjectNode node) {
        for (String name : List.copyOf(node.propertyNames())) {
            JsonNode value = node.get(name);
            if (value instanceof ObjectNode object) {
                if (isVector(object)) {
                    node.set(name, vector(object));
                } else {
                    convertVectors(object);
                }
            } else if (value instanceof ArrayNode array) {
                for (JsonNode element : array) {
                    if (element instanceof ObjectNode object) convertVectors(object);
                }
            }
        }
        return node;
    }

    private static boolean isVector(ObjectNode node) {
        JsonNode alias = node.get(ALIAS);
        return alias != null && VECTORS.contains(alias.asString());
    }

    private static ObjectNode vector(ObjectNode vector) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("xCoord", vector.path("x").asDouble());
        result.put("yCoord", vector.path("y").asDouble());
        result.put("zCoord", vector.path("z").asDouble());
        return result;
    }
}

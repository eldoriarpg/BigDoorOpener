/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.conditions.location.Region;
import de.eldoria.bigdoorsopener.conditions.location.SimpleRegion;
import de.eldoria.bigdoorsopener.conditions.permission.DoorPermission;
import de.eldoria.bigdoorsopener.conditions.permission.PermissionNode;
import de.eldoria.bigdoorsopener.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs.MythicMob;
import de.eldoria.bigdoorsopener.conditions.standalone.weather.Weather;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldProximity;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldRegion;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldSimpleRegion;
import de.eldoria.bigdoorsopener.config.elements.Settings;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDoorMigrationTest {
    private static final Logger LOGGER = Logger.getLogger("test");
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    @TempDir
    Path dir;

    @BeforeAll
    static void registerConditions() {
        List.of(ItemBlock.getConditionContainer(), ItemClick.getConditionContainer(), ItemHolding.getConditionContainer(),
                        ItemOwning.getConditionContainer(), Proximity.getConditionContainer(), Region.getConditionContainer(),
                        SimpleRegion.getConditionContainer(), WorldProximity.getConditionContainer(),
                        WorldRegion.getConditionContainer(), WorldSimpleRegion.getConditionContainer(),
                        PermissionNode.getConditionContainer(), DoorPermission.getConditionContainer(),
                        Time.getConditionContainer(), Weather.getConditionContainer(), Placeholder.getConditionContainer(),
                        MythicMob.getConditionContainer())
                .forEach(container -> {
                    if (ConditionRegistrar.getContainerByClass(container.getClazz()).isEmpty()) {
                        ConditionRegistrar.registerCondition(container);
                    }
                });
    }

    @Test
    void movesDoorsIntoDoorsFile() throws IOException {
        fixture("config.yml");

        assertTrue(LegacyDoorMigration.migrate(dir, LOGGER));

        assertTrue(Files.exists(dir.resolve(LegacyDoorMigration.BACKUP)));
        JsonNode doors = YAML.readTree(dir.resolve("doors.yml").toFile()).get("doors");
        assertEquals(2, doors.size());

        JsonNode door = doors.get(0);
        assertFalse(door.has("=="));
        assertFalse(door.has("conditionBag"));
        assertEquals(10.5, door.get("position").get("xCoord").asDouble());
        assertEquals(13, door.get("conditions").size());

        JsonNode time = door.get("conditions").get(0);
        assertEquals("timeCondition", time.get("type").asString());
        assertFalse(time.has("=="));

        JsonNode proximity = door.get("conditions").get(4);
        assertEquals("ellipsoid", proximity.get("proximityForm").asString().toLowerCase());
        assertEquals(3.0, proximity.get("dimensions").get("yCoord").asDouble());

        JsonNode simpleRegion = door.get("conditions").get(8);
        assertEquals(6.0, simpleRegion.get("maximum").get("zCoord").asDouble());

        JsonNode chainDoor = doors.get(1);
        assertEquals(2, chainDoor.get("conditions").size());
    }

    @Test
    void migratedDoorsLoad() throws IOException {
        fixture("config.yml");
        LegacyDoorMigration.migrate(dir, LOGGER);

        Map<Long, ConditionalDoor> doors = new JacksonConfiguration(plugin()).doors().byId();

        ConditionalDoor door = doors.get(1L);
        assertEquals(12, door.conditionBag().getConditions().size(), "the condition of the unknown type is skipped");
        assertEquals(ConditionalDoor.EvaluationType.CUSTOM, door.evaluationType());
        assertEquals("time && permission", door.evaluator());
        assertTrue(door.isInvertOpen());
        assertEquals(5, door.stayOpen());
        assertEquals(-3.5, door.position().getZ());
        assertInstanceOf(Time.class, door.conditionBag().getConditions("time").getFirst());
        assertEquals(2, door.conditionBag().getConditions("permission").size());
        assertEquals(3, door.conditionBag().getConditions("location").size());
        assertEquals(3, door.conditionBag().getConditions("worldLocation").size());

        ConditionalDoor chainDoor = doors.get(2L);
        assertEquals(ConditionalDoor.EvaluationType.OR, chainDoor.evaluationType());
        assertTrue(chainDoor.isEnabled());
        assertEquals(2, chainDoor.conditionBag().getConditions().size());
    }

    @Test
    void settingsKeepTheirValues() throws IOException {
        fixture("config.yml");
        LegacyDoorMigration.migrate(dir, LOGGER);

        Settings settings = new JacksonConfiguration(plugin()).main();

        assertEquals(40, settings.refreshRate());
        assertEquals("de_DE", settings.language());
        assertEquals(600, settings.jsCacheSize());
        assertEquals(150, settings.playerCheckRadius());
        assertFalse(settings.enableMetrics());
        assertFalse(settings.updates().notice());
    }

    @Test
    void savedDoorsLoadUnchanged() throws IOException {
        fixture("config.yml");
        LegacyDoorMigration.migrate(dir, LOGGER);
        JacksonConfiguration configuration = new JacksonConfiguration(plugin());
        configuration.doors();
        configuration.save();
        String saved = Files.readString(dir.resolve("doors.yml"));

        new JacksonConfiguration(plugin()).doors();
        JacksonConfiguration reloaded = new JacksonConfiguration(plugin());
        reloaded.doors();
        reloaded.save();

        assertEquals(saved, Files.readString(dir.resolve("doors.yml")));
        assertFalse(saved.contains("someAddonCondition"));
    }

    @Test
    void itemsKeepTheBukkitFormat() throws IOException {
        fixture("items.yml");

        LegacyDoorMigration.migrate(dir, LOGGER);

        JsonNode condition = YAML.readTree(dir.resolve("doors.yml").toFile()).get("doors").get(0).get("conditions").get(0);
        assertEquals("itemBlockCondition", condition.get("type").asString());
        assertEquals("org.bukkit.inventory.ItemStack", condition.get("item").get("==").asString());
        assertEquals("ItemMeta", condition.get("item").get("meta").get("==").asString());
        assertEquals(65.0, condition.get("position").get("yCoord").asDouble());
    }

    @Test
    void refusesConfigurationOfFirstVersion() throws IOException {
        Files.writeString(dir.resolve("config.yml"), "refreshRate: 20\ndoors: []\n");

        assertThrows(IllegalStateException.class, () -> LegacyDoorMigration.migrate(dir, LOGGER));
    }

    @Test
    void leavesCurrentConfigurationAlone() throws IOException {
        Files.writeString(dir.resolve("config.yml"), "version: 2\nrefreshRate: 20\n");

        assertFalse(LegacyDoorMigration.migrate(dir, LOGGER));
        assertFalse(Files.exists(dir.resolve("doors.yml")));
    }

    @Test
    void conditionsKeepTheirSettings() throws IOException {
        fixture("config.yml");
        LegacyDoorMigration.migrate(dir, LOGGER);
        ConditionalDoor door = new JacksonConfiguration(plugin()).doors().byId().get(1L);

        DoorCondition weather = door.conditionBag().getConditions("weather").getFirst();
        assertTrue(weather.getCreationCommand(door).endsWith("weather clear true"));
        DoorCondition time = door.conditionBag().getConditions("time").getFirst();
        assertTrue(time.getCreationCommand(door).endsWith("time 1000 13000 true"));
    }

    private void fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/migration/" + name)) {
            Files.copy(in, dir.resolve("config.yml"));
        }
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Plugin.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getDataFolder" -> dir.toFile();
            case "getLogger" -> LOGGER;
            case "getName" -> "BigDoorsOpener";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test plugin";
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.functions.TriFunction;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import de.eldoria.eldoutilities.utils.EnumUtil;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;

/**
 * Opens the door for players within a distance of the door.
 */
@JsonTypeName("proximityCondition")
public class Proximity implements Location {
    private final Vector dimensions;
    private final ProximityForm proximityForm;

    @JsonCreator
    public Proximity(@JsonProperty("dimensions") Vector dimensions, @JsonProperty("proximityForm") ProximityForm proximityForm) {
        this.dimensions = dimensions;
        this.proximityForm = proximityForm;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Proximity.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.dimensions$> [$syntax.proximityForm$]");
                    Vector vector = parseDimensions(args.asString(0));
                    ProximityForm form = parseForm(args);
                    conditionBag.accept(new Proximity(vector, form));
                    messageSender.sendMessage(sender, "setCondition.proximity");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.dimensions") + ">", "<x,y,z>");
                    }
                    if (args.sizeIs(2)) {
                        return Completion.complete(args.asString(1), ProximityForm.class, true, false);
                    }
                    return Collections.emptyList();
                })
                .withMeta("proximity", "location", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    /**
     * Parses the dimensions of a proximity from a single size or from {@code x,y,z}.
     *
     * @param input dimensions as entered
     * @return dimensions between 1 and 100 on every axis
     * @throws CommandException when the input is no valid size
     */
    public static Vector parseDimensions(String input) throws CommandException {
        String[] coords = input.split(",");
        Vector vector;
        if (coords.length == 1) {
            double size = Parser.parseDouble(input).orElseThrow(() -> CommandException.message("error.invalidNumber"));
            vector = new Vector(size, size, size);
        } else if (coords.length == 3) {
            vector = new Vector(
                    Parser.parseDouble(coords[0]).orElseThrow(() -> CommandException.message("error.invalidNumber")),
                    Parser.parseDouble(coords[1]).orElseThrow(() -> CommandException.message("error.invalidNumber")),
                    Parser.parseDouble(coords[2]).orElseThrow(() -> CommandException.message("error.invalidNumber")));
        } else {
            throw CommandException.message("error.invalidVector");
        }
        if (vector.getX() < 1 || vector.getX() > 100
            || vector.getY() < 1 || vector.getY() > 100
            || vector.getZ() < 1 || vector.getZ() > 100) {
            throw CommandException.message("error.invalidRange", Replacement.create("MIN", 1), Replacement.create("MAX", 100));
        }
        return vector;
    }

    /**
     * Parses the optional form of a proximity.
     *
     * @param args arguments of the condition
     * @return the form, cuboid when none is given
     * @throws CommandException when the form is unknown
     */
    public static ProximityForm parseForm(Arguments args) throws CommandException {
        if (!args.hasArg(1)) return ProximityForm.CUBOID;
        return EnumUtil.parse(args.asString(1), ProximityForm.class).orElseThrow(() -> CommandException.message("error.invalidForm"));
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return contains(door, player);
    }

    private boolean contains(ConditionalDoor door, Player player) {
        Vector vector = player.getLocation().toVector();
        return proximityForm.check.apply(door.position(),
                new Vector(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ()),
                dimensions);
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.proximity", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.size") + " ", C.baseColor))
                .append(Component.text(dimensions.toString(), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.proximityForm") + " ", C.baseColor))
                .append(Component.text(localizer.getMessage(proximityForm.localKey), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " proximity "
               + dimensions.getX() + "," + dimensions.getY() + "," + dimensions.getZ()
               + " " + proximityForm.name().toLowerCase();
    }

    @Override
    public Proximity clone() {
        return new Proximity(dimensions, proximityForm);
    }

    public enum ProximityForm {
        CUBOID("conditionDesc.proximityForm.cuboid",
                (point, target, dimensions) -> {
                    if (Math.abs(point.getX() - target.getX()) > dimensions.getX()) return false;
                    if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
                    return !(Math.abs(point.getZ() - target.getZ()) > dimensions.getZ());
                }),
        ELLIPSOID("conditionDesc.proximityForm.ellipsoid",
                (point, target, dimensions) ->
                        Math.pow((target.getX() - point.getX()) / dimensions.getX(), 2)
                        + Math.pow((target.getY() - point.getY()) / dimensions.getY(), 2)
                        + Math.pow((target.getZ() - point.getZ()) / dimensions.getZ(), 2) <= 1),
        CYLINDER("conditionDesc.proximityForm.cylinder",
                (point, target, dimensions) -> {
                    if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
                    return Math.pow(target.getX() - point.getX(), 2) / Math.pow(dimensions.getX(), 2)
                           + Math.pow(target.getZ() - point.getZ(), 2) / Math.pow(dimensions.getZ(), 2) <= 1;
                });

        public final String localKey;
        /**
         * Checks whether a target is inside the form around a point: point, target, dimensions.
         */
        public final TriFunction<Vector, Vector, Vector, Boolean> check;

        ProximityForm(String localKey, TriFunction<Vector, Vector, Vector, Boolean> check) {
            this.localKey = localKey;
            this.check = check;
        }
    }
}

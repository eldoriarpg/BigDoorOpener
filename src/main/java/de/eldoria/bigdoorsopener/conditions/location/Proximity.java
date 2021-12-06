/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.location;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.functions.TriFunction;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import de.eldoria.eldoutilities.utils.EnumUtil;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A condition which opens the door when the player is within a specific range of defined by geometric form
 */
@SerializableAs("proximityCondition")
public class Proximity implements Location {
    private final Vector dimensions;
    private final ProximityForm proximityForm;

    public Proximity(Vector dimensions, ProximityForm proximityForm) {
        this.dimensions = dimensions;
        this.proximityForm = proximityForm;
    }

    public Proximity(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        dimensions = resolvingMap.getValue("dimensions");
        String formString = resolvingMap.getValue("proximityForm");
        formString = formString.replaceAll("(?i)elipsoid", "ellipsoid");
        proximityForm = EnumUtil.parse(formString, ProximityForm.class, ProximityForm.CUBOID);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Proximity.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("tabcomplete.dimensions") + "> ["
                                    + localizer.getMessage("syntax.proximityForm") + "]")) {
                        return;
                    }

                    Vector vector;
                    String[] coords = arguments[0].split(",");

                    // parse the size.
                    if (coords.length == 1) {
                        Optional<Double> size = Parser.parseDouble(arguments[0]);
                        if (!size.isPresent()) {
                            messageSender.sendError(player, localizer.getMessage("error.invalidNumber"));
                            return;
                        }
                        vector = new Vector(size.get(), size.get(), size.get());
                    } else if (coords.length == 3) {
                        Optional<Double> x = Parser.parseDouble(coords[0]);
                        Optional<Double> y = Parser.parseDouble(coords[1]);
                        Optional<Double> z = Parser.parseDouble(coords[2]);
                        if (x.isPresent() && y.isPresent() && z.isPresent()) {
                            vector = new Vector(x.get(), y.get(), z.get());
                        } else {
                            messageSender.sendError(player, localizer.getMessage("error.invalidNumber"));
                            return;
                        }
                    } else {
                        messageSender.sendError(player, localizer.getMessage("error.invalidVector"));
                        return;
                    }

                    // check if vector is inside bounds.
                    if (vector.getX() < 1 || vector.getX() > 100
                            || vector.getY() < 1 || vector.getY() > 100
                            || vector.getZ() < 1 || vector.getZ() > 100) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                                Replacement.create("MIN", 1).addFormatting('6'),
                                Replacement.create("MAX", 100).addFormatting('6')));
                        return;
                    }

                    Proximity.ProximityForm form = ArgumentUtils.getOptionalParameter(arguments, 1, Optional.of(Proximity.ProximityForm.CUBOID), (s) -> EnumUtil.parse(s, Proximity.ProximityForm.class)).orElse(null);

                    if (form == null) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidForm"));
                        return;
                    }

                    conditionBag.accept(new Proximity(vector, form));

                    // TODO: display region Maybe some day. In a far future...

                    messageSender.sendMessage(player, localizer.getMessage("setCondition.proximity"));
                })
                .onTabComplete((sender, localizer, args) -> {
                    final String[] proximityForm = Arrays.stream(Proximity.ProximityForm.values())
                            .map(v -> v.name().toLowerCase())
                            .toArray(String[]::new);

                    if (args.length == 1) {
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.dimensions") + ">", "<x,y,z>");
                    }
                    if (args.length == 2) {
                        return ArrayUtil.startingWithInArray(args[1], proximityForm).collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                })
                .withMeta("proximity", "location", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        Vector vector = player.getLocation().toVector();
        return proximityForm.check.apply(door.position(),
                new Vector(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ()),
                dimensions);
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.proximity",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
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

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("dimensions", dimensions)
                .add("proximityForm", proximityForm)
                .build();
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
         * point, target, dimension
         */
        public TriFunction<Vector, Vector, Vector, Boolean> check;

        ProximityForm(String localKey, TriFunction<Vector, Vector, Vector, Boolean> check) {
            this.localKey = localKey;
            this.check = check;
        }
    }
}

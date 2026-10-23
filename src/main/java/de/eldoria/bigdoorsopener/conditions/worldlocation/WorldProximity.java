/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.worldlocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;

/**
 * Opens the door while any player of the world is within a distance of the door.
 */
@JsonTypeName("worldProximityCondition")
public class WorldProximity implements WorldLocation {
    private final Vector dimensions;
    private final Proximity.ProximityForm proximityForm;

    @JsonCreator
    public WorldProximity(@JsonProperty("dimensions") Vector dimensions, @JsonProperty("proximityForm") Proximity.ProximityForm proximityForm) {
        this.dimensions = dimensions;
        this.proximityForm = proximityForm;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldProximity.class, Scope.WORLD)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.dimensions$> [$syntax.proximityForm$]");
                    Vector vector = Proximity.parseDimensions(args.asString(0));
                    Proximity.ProximityForm form = Proximity.parseForm(args);
                    conditionBag.accept(new WorldProximity(vector, form));
                    messageSender.sendMessage(sender, "setCondition.proximity");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.dimensions") + ">", "<x,y,z>");
                    }
                    if (args.sizeIs(2)) {
                        return Completion.complete(args.asString(1), Proximity.ProximityForm.class, true, false);
                    }
                    return Collections.emptyList();
                })
                .withMeta("worldProximity", "worldLocation", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        for (Player worldPlayer : world.getPlayers()) {
            if (contains(door, worldPlayer)) return true;
        }
        return false;
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
        return SET_COMMAND + door.doorUID() + " worldProximity "
               + dimensions.getX() + "," + dimensions.getY() + "," + dimensions.getZ()
               + " " + proximityForm.name().toLowerCase();
    }

    @Override
    public WorldProximity clone() {
        return new WorldProximity(dimensions, proximityForm);
    }
}

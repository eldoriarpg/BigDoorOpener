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
import de.eldoria.bigdoorsopener.conditions.location.WorldGuardRegions;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * Opens the door while any player of the world is inside a WorldGuard region.
 * <p>
 * Only the names of world and region are kept, the region is looked up through {@link WorldGuardRegions}.
 */
@JsonTypeName("worldRegionCondition")
public class WorldRegion implements WorldLocation {
    @JsonProperty("world")
    private final String worldName;

    @JsonProperty("region")
    private final String regionId;

    @JsonCreator
    public WorldRegion(@JsonProperty("world") String worldName, @JsonProperty("region") String regionId) {
        this.worldName = worldName;
        this.regionId = regionId;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldRegion.class, Scope.WORLD)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    if (!WorldGuardRegions.available()) throw CommandException.message("error.wgNotEnabled");
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.regionName$>");
                    Player player = ConditionInput.player(sender);
                    String regionId = args.asString(0);
                    if (!WorldGuardRegions.exists(player.getWorld(), regionId)) {
                        throw CommandException.message("error.regionNotFound");
                    }
                    conditionBag.accept(new WorldRegion(player.getWorld().getName(), regionId));
                    messageSender.sendMessage(sender, "setCondition.region");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.regionName") + ">");
                    }
                    return Collections.emptyList();
                })
                .withMeta("worldRegion", "worldLocation", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (!world.getName().equals(worldName)) return false;
        if (!WorldGuardRegions.available() || !WorldGuardRegions.exists(world, regionId)) return null;
        for (Player worldPlayer : world.getPlayers()) {
            if (Boolean.TRUE.equals(WorldGuardRegions.contains(world, regionId, worldPlayer))) return true;
        }
        return false;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.region", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.region") + " ", C.baseColor))
                .append(Component.text(regionId, C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.world") + " ", C.baseColor))
                .append(Component.text(worldName, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " worldRegion " + regionId;
    }

    @Override
    public WorldRegion clone() {
        return new WorldRegion(worldName, regionId);
    }
}

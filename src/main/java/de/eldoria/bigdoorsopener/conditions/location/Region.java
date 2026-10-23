/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Opens the door for players inside a WorldGuard region.
 * <p>
 * Only the names of world and region are kept, the region is looked up through {@link WorldGuardRegions}.
 */
@JsonTypeName("regionCondition")
public class Region implements Location {
    @JsonProperty("world")
    private final String worldName;

    @JsonProperty("region")
    private final String regionId;

    @JsonIgnore
    private final Cache<org.bukkit.Location, Boolean> cache = C.getExpiringCache();

    @JsonCreator
    public Region(@JsonProperty("world") String worldName, @JsonProperty("region") String regionId) {
        this.worldName = worldName;
        this.regionId = regionId;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Region.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    if (!WorldGuardRegions.available()) throw CommandException.message("error.wgNotEnabled");
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.regionName$>");
                    Player player = ConditionInput.player(sender);
                    String regionId = args.asString(0);
                    if (!WorldGuardRegions.exists(player.getWorld(), regionId)) {
                        throw CommandException.message("error.regionNotFound");
                    }
                    conditionBag.accept(new Region(player.getWorld().getName(), regionId));
                    messageSender.sendMessage(sender, "setCondition.region");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.regionName") + ">");
                    }
                    return Collections.emptyList();
                })
                .withMeta("region", "location", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (!world.getName().equals(worldName)) return false;
        if (!WorldGuardRegions.available() || !WorldGuardRegions.exists(world, regionId)) return null;
        try {
            return cache.get(
                    player.getLocation(),
                    () -> Boolean.TRUE.equals(WorldGuardRegions.contains(world, regionId, player)));
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute value", e);
        }
        return null;
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
        return SET_COMMAND + door.doorUID() + " region " + regionId;
    }

    @Override
    public Region clone() {
        return new Region(worldName, regionId);
    }
}

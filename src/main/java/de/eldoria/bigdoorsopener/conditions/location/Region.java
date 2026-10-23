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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
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
 * The WorldGuard region is looked up on first use, since WorldGuard and the world may not be loaded when the doors are
 * read.
 */
@JsonTypeName("regionCondition")
public class Region implements Location {
    @JsonProperty("world")
    private final String worldName;

    @JsonProperty("region")
    private final String regionId;

    @JsonIgnore
    private ProtectedRegion region;

    @JsonIgnore
    private final Cache<org.bukkit.Location, Boolean> cache = C.getExpiringCache();

    @JsonCreator
    public Region(@JsonProperty("world") String worldName, @JsonProperty("region") String regionId) {
        this.worldName = worldName;
        this.regionId = regionId;
    }

    public Region(ProtectedRegion region, World world) {
        this(world.getName(), region.getId());
        this.region = region;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Region.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    RegionContainer regionContainer = BigDoorsOpener.regionContainer();
                    if (regionContainer == null) throw CommandException.message("error.wgNotEnabled");
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.regionName$>");
                    Player player = ConditionInput.player(sender);
                    RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                    if (rm == null) throw CommandException.message("error.regionNotFound");
                    ProtectedRegion region = rm.getRegion(args.asString(0));
                    if (region == null) throw CommandException.message("error.regionNotFound");
                    conditionBag.accept(new Region(region, player.getWorld()));
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
        ProtectedRegion region = region(world);
        if (region == null) return null;
        try {
            return cache.get(
                    player.getLocation(), () -> region.contains(BukkitAdapter.asBlockVector(player.getLocation())));
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute value", e);
        }
        return null;
    }

    private ProtectedRegion region(World world) {
        if (region == null && BigDoorsOpener.regionContainer() != null) {
            RegionManager manager = BigDoorsOpener.regionContainer().get(BukkitAdapter.adapt(world));
            region = manager == null ? null : manager.getRegion(regionId);
        }
        return region;
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

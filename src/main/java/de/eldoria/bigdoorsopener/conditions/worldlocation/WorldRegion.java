/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.worldlocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

/**
 * Opens the door while any player of the world is inside a WorldGuard region.
 * <p>
 * The WorldGuard region is looked up on first use, since WorldGuard and the world may not be loaded when the doors are
 * read.
 */
@JsonTypeName("worldRegionCondition")
public class WorldRegion implements WorldLocation {
    @JsonProperty("world")
    private final String worldName;

    @JsonProperty("region")
    private final String regionId;

    @JsonIgnore
    private ProtectedRegion region;

    @JsonCreator
    public WorldRegion(@JsonProperty("world") String worldName, @JsonProperty("region") String regionId) {
        this.worldName = worldName;
        this.regionId = regionId;
    }

    public WorldRegion(ProtectedRegion region, World world) {
        this(world.getName(), region.getId());
        this.region = region;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldRegion.class, Scope.WORLD)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    RegionContainer regionContainer = BigDoorsOpener.regionContainer();
                    if (regionContainer == null) throw CommandException.message("error.wgNotEnabled");
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.regionName$>");
                    Player player = ConditionInput.player(sender);
                    RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                    if (rm == null) throw CommandException.message("error.regionNotFound");
                    ProtectedRegion region = rm.getRegion(args.asString(0));
                    if (region == null) throw CommandException.message("error.regionNotFound");
                    conditionBag.accept(new WorldRegion(region, player.getWorld()));
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
        ProtectedRegion region = region(world);
        if (region == null) return null;
        for (Player worldPlayer : world.getPlayers()) {
            if (region.contains(BukkitAdapter.asBlockVector(worldPlayer.getLocation()))) return true;
        }
        return false;
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
        return SET_COMMAND + door.doorUID() + " worldRegion " + regionId;
    }

    @Override
    public WorldRegion clone() {
        return new WorldRegion(worldName, regionId);
    }
}

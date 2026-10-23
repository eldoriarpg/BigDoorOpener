/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Access to WorldGuard regions for the region conditions.
 * <p>
 * The region conditions only store world and region names. Every WorldGuard type stays in this class, which is only
 * loaded once WorldGuard is known to be present. Jackson inspects the fields and constructors of the conditions, so a
 * WorldGuard type there would fail every server without WorldGuard.
 */
public final class WorldGuardRegions {
    private WorldGuardRegions() {}

    /**
     * Checks whether WorldGuard is hooked.
     *
     * @return true if regions can be used
     */
    public static boolean available() {
        return BigDoorsOpener.regionContainer() != null;
    }

    /**
     * Checks whether a region exists in a world. Requires {@link #available()}.
     *
     * @param world    world of the region
     * @param regionId id of the region
     * @return true if the region exists
     */
    public static boolean exists(World world, String regionId) {
        return region(world, regionId) != null;
    }

    /**
     * Checks whether a player is inside a region. Requires {@link #available()}.
     *
     * @param world    world of the region
     * @param regionId id of the region
     * @param player   player to check
     * @return true if the player is inside, null if the region does not exist
     */
    public static @Nullable Boolean contains(World world, String regionId, Player player) {
        ProtectedRegion region = region(world, regionId);
        if (region == null) return null;
        return region.contains(BukkitAdapter.asBlockVector(player.getLocation()));
    }

    private static @Nullable ProtectedRegion region(World world, String regionId) {
        RegionContainer container = BigDoorsOpener.regionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        return manager == null ? null : manager.getRegion(regionId);
    }
}

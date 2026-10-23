/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;

import java.util.List;

/**
 * Access to MythicMobs for the mythic mob condition.
 * <p>
 * Every MythicMobs type stays in this class, which is only loaded once MythicMobs is known to be present. Jackson
 * inspects the members of the conditions, so a MythicMobs type there would fail every server without MythicMobs.
 */
public final class MythicMobsAccess {
    private MythicMobsAccess() {}

    /**
     * Checks whether a mob type exists.
     *
     * @param mobType internal name of the type
     * @return true if MythicMobs knows the type
     */
    public static boolean exists(String mobType) {
        return MythicBukkit.inst().getAPIHelper().getMythicMob(mobType) != null;
    }

    /**
     * Lists the internal names of all mob types.
     *
     * @return names of the mob types
     */
    public static List<String> mobTypes() {
        return MythicBukkit.inst().getMobManager().getMobTypes().stream()
                .map(MythicMob::getInternalName)
                .toList();
    }
}

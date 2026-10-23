/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class MythicMobsListener implements Listener {

    private final Config config;

    public MythicMobsListener(Config config) {
        this.config = config;
    }

    @EventHandler
    public void onMobDeath(MythicMobDeathEvent event) {
        config.getDoors().forEach(d -> {
            List<DoorCondition> mythicMobs = d.conditionBag().getConditions("mythicMob");
            if (mythicMobs.isEmpty()) return;
            mythicMobs.forEach(
                    m -> ((MythicMob) m).killed(event.getMob().getType().getInternalName()));
        });
    }
}

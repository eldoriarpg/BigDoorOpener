/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ModificationListener implements Listener {
    private final Config config;

    public ModificationListener(Config config) {
        this.config = config;
    }

    @EventHandler
    public void onConditionBagModification(ConditionBagModifiedEvent event) {
        BigDoorsOpener.logger().fine("Config was modified. Saving config.");
        config.safeConfig();
    }

    @EventHandler
    public void onDoorModifiedEvent(DoorModifiedEvent event) {
        BigDoorsOpener.logger().fine("Config was modified. Saving config.");
        config.safeConfig();
    }

    @EventHandler
    public void onDoorRegisteredEvent(DoorRegisteredEvent event) {
        BigDoorsOpener.logger().fine("Config was modified. Saving config.");
        config.safeConfig();
    }

    @EventHandler
    public void onDoorUnregisteredEvent(DoorUnregisteredEvent event) {
        BigDoorsOpener.logger().fine("Config was modified. Saving config.");
        config.safeConfig();
    }
}

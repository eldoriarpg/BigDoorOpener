/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.events.DoorEventToggle;
import nl.pim16aap2.bigDoors.events.DoorEventToggleEnd;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DoorOpenedListener implements Listener {
    private final Config config;

    public DoorOpenedListener(Config config) {
        this.config = config;
    }

    @EventHandler
    public void doorOpened(DoorEventToggleEnd toggleEnd) {
        Door toggledDoor = toggleEnd.getDoor();
        ConditionalDoor conDoor = config.getDoor(toggledDoor.getDoorUID());
        if (conDoor == null) return;

        if (toggleEnd.getToggleType() == DoorEventToggle.ToggleType.STATIC) return;

        if (conDoor.isInvertOpen()) {
            if (toggleEnd.getToggleType() == DoorEventToggle.ToggleType.OPEN) {
                return;
            }
        } else {
            if (toggleEnd.getToggleType() == DoorEventToggle.ToggleType.CLOSE) {
                return;
            }
        }

        conDoor.opened();
    }
}

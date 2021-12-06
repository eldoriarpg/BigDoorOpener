/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConditionalDoorEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ConditionalDoor door;

    public ConditionalDoorEvent(ConditionalDoor door) {
        this.door = door;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public ConditionalDoor door() {
        return door;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}

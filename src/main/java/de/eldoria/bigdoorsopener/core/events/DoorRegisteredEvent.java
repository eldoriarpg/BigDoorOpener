package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DoorRegisteredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final ConditionalDoor door;

    /**
     * Create a new Door Registered Event.
     *
     * @param door world where the blood night has ended.
     */
    public DoorRegisteredEvent(ConditionalDoor door) {
        this.door = door;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}

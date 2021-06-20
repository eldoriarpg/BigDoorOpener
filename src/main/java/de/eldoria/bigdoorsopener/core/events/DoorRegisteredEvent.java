package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DoorRegisteredEvent extends ConditionalDoorEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Create a new Door Registered Event.
     *
     * @param door world where the blood night has ended.
     */
    public DoorRegisteredEvent(ConditionalDoor door) {
        super(door);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}

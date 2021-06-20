package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConditionBagModifiedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ConditionBag conditionBag;

    /**
     * Create a new Door Unegistered Event.
     *
     * @param conditionBag bag which was modified.
     */
    public ConditionBagModifiedEvent(ConditionBag conditionBag) {
        this.conditionBag = conditionBag;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public ConditionBag conditionBag() {
        return conditionBag;
    }
}

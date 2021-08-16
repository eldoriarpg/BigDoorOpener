package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConditionBagModifiedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ConditionalDoor door;
    private final ConditionBag conditionBag;
    private final DoorCondition condition;

    /**
     * Create a new ConditionBagModified Event.
     *
     * @param door the door which owns the condition bag
     * @param conditionBag bag which was modified.
     * @param condition condition which was affected by this event
     */
    public ConditionBagModifiedEvent(ConditionalDoor door, ConditionBag conditionBag, DoorCondition condition) {
        this.door = door;
        this.conditionBag = conditionBag;
        this.condition = condition;
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

    /**
     * The condition which was affected by this event
     *
     * @return door condition
     */
    public DoorCondition condition() {
        return condition;
    }

    public ConditionalDoor door() {
        return door;
    }
}

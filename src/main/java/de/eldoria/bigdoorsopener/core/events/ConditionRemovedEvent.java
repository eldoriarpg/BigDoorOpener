package de.eldoria.bigdoorsopener.core.events;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;

public class ConditionRemovedEvent extends ConditionBagModifiedEvent {
    /**
     * Create a new ConditionBagModified Event.
     *
     * @param conditionBag bag which was modified.
     * @param condition condition which was affected by this event
     */
    public ConditionRemovedEvent(ConditionalDoor door, ConditionBag conditionBag, DoorCondition condition) {
        super(door, conditionBag, condition);
    }
}

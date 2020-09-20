package de.eldoria.bigdoorsopener.conditions.location;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;

public interface Location extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " location";
    }

    @Override
    Location clone();
}

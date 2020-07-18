package de.eldoria.bigdoorsopener.doors.conditions.location;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;

public interface Location extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " location";

    }
}

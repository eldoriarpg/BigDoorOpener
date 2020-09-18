package de.eldoria.bigdoorsopener.doors.conditions.permission;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;

public interface Permission extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " permission";
    }

    @Override
    Permission clone();
}

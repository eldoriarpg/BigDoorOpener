package de.eldoria.bigdoorsopener.conditions.permission;

import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;

public interface Permission extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " permission";
    }

    @Override
    Permission clone();
}

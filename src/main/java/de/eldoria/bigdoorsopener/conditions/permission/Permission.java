package de.eldoria.bigdoorsopener.conditions.permission;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;

public interface Permission extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " permission";
    }

    @Override
    Permission clone();
}

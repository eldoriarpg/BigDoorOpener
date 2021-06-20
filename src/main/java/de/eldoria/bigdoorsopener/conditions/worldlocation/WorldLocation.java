package de.eldoria.bigdoorsopener.conditions.worldlocation;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;

public interface WorldLocation extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " worldlocation";
    }

    @Override
    WorldLocation clone();
}

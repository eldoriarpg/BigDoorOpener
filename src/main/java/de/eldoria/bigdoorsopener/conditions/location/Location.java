/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.location;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;

public interface Location extends DoorCondition {
    @Override
    default String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " location";
    }

    @Override
    Location clone();
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.exceptions;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;

public class ConditionCreationException extends RuntimeException {
    public ConditionCreationException(Class<? extends DoorCondition> con) {
        super("Could not create condition of type " + con.getName() + ".");
    }

    public ConditionCreationException(Class<? extends DoorCondition> con, Throwable cause) {
        super("Could not create condition of type " + con.getName() + ".", cause);
    }

    public ConditionCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConditionCreationException(String message) {
        super(message);
    }
}

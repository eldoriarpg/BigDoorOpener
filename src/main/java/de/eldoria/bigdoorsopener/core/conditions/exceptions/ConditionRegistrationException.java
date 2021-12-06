/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.conditions.exceptions;

public class ConditionRegistrationException extends RuntimeException {
    public ConditionRegistrationException(Class<?> clazz) {
        super("Registration of condition " + clazz.getName() + " failed.");
    }
}

package de.eldoria.bigdoorsopener.core.conditions.exceptions;

public class ConditionRegistrationException extends RuntimeException {
    public ConditionRegistrationException(Class<?> clazz) {
        super("Registration of condition " + clazz.getName() + " failed.");
    }
}

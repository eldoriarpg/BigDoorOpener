package de.eldoria.bigdoorsopener.doors.conditions;

public class ConditionCreationFailedException extends RuntimeException {
    public ConditionCreationFailedException(String localeCode) {
        super(localeCode);
    }
}

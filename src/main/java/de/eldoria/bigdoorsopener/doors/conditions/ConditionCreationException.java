package de.eldoria.bigdoorsopener.doors.conditions;

public class ConditionCreationException extends RuntimeException {
    public ConditionCreationException(String localeCode) {
        super(localeCode);
    }
}

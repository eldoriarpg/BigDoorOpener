package de.eldoria.bigdoorsopener.doors.conditions;

public class CreationNotImplementedException extends RuntimeException {
    public CreationNotImplementedException() {
        super("No creating routine is implemented, but a creation was requested.");
    }
}

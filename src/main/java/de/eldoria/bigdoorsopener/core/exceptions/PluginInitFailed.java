package de.eldoria.bigdoorsopener.core.exceptions;

public class PluginInitFailed extends RuntimeException {
    public PluginInitFailed(String message) {
        super(message);
    }
}

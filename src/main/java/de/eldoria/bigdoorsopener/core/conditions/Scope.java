package de.eldoria.bigdoorsopener.core.conditions;

public enum Scope {
    /**
     * This condition requires world evaluation.
     * <p>It will only be called once in a evaluation cyclus.
     */
    WORLD,
    /**
     * This condition requires per player evaluation.
     * <p>It will be called for each player in a evaluation cyclus.
     */
    PLAYER
}

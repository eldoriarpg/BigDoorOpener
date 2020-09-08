package de.eldoria.bigdoorsopener.doors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Condition {
    /**
     * Name of the condition group. Not unique. Multiple conditions can be grouped together by using the same group name.
     *
     * @return group
     */

    String group();

    /**
     * Name of the condition. Must be unique globally.
     *
     * @return name
     */
    String name();

    /**
     * The serialized name of the condition like used in {@link org.bukkit.configuration.serialization.SerializableAs}
     *
     * @return serialized name
     */
    String serializedName();

    /**
     * Get the scope of the condition.
     *
     * @return scope
     */
    Scope scope();

    /**
     * Cost of the condition. Lower number means lower costs.
     *
     * @return costs.
     */
    int cost();

    public enum Scope {
        WORLD, PLAYER
    }
}

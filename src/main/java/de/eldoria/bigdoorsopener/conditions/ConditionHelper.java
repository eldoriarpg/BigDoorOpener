package de.eldoria.bigdoorsopener.conditions;

import org.bukkit.configuration.serialization.SerializableAs;

public final class ConditionHelper {
    private ConditionHelper() {
    }

    /**
     * Get the serialized of the class based on {@link SerializableAs}
     *
     * @param condition condition to check
     * @return serialized name of class
     * @throws IllegalStateException when the annotation is not present.
     */
    public static String serializedName(Class<? extends DoorCondition> condition) throws IllegalStateException {
        if (!condition.isAnnotationPresent(SerializableAs.class)) {
            throw new IllegalStateException("Missing serialization annotation in class " + condition.getName());
        }
        return condition.getAnnotation(SerializableAs.class).value();
    }
}

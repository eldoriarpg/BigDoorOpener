package de.eldoria.bigdoorsopener.doors.conditions;

import org.bukkit.configuration.serialization.SerializableAs;

public final class ConditionHelper {
    private ConditionHelper() {
    }

    /**
     * Get the name of the condition. this is the name which should always be used to identify the condition.
     *
     * @return name of class as string in lower case
     */
    public static <T extends DoorCondition> String getName(Class<T> condition) {
        return condition.getSimpleName().toLowerCase();
    }

    /**
     * Get the name of the condition info key for localization.
     *
     * @return name of class as string in lower case
     */
    public static <T extends DoorCondition> String getInfoKey(Class<T> condition) {
        return "info." + getName(condition);
    }

    public static String serializedName(Class<? extends DoorCondition> condition) {
        if (!condition.isAnnotationPresent(SerializableAs.class)) {
            throw new IllegalStateException("Missing serialization annotation in class " + condition.getName());
        }
        return condition.getAnnotation(SerializableAs.class).value();
    }
}

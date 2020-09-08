package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.Condition;

import java.util.logging.Level;

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

    public static String getGroup(Class<? extends DoorCondition> conClazz) {
        return conClazz.getAnnotation(Condition.class).group();
    }

    private static String name(Class<? extends DoorCondition> conClazz) {
        return conClazz.getAnnotation(Condition.class).name();
    }

    public static String serializedName(Class<? extends DoorCondition> condition) {
        return condition.getAnnotation(Condition.class).serializedName();
    }
    public static Condition.Scope getScope(Class<? extends DoorCondition> condition) {
        return condition.getAnnotation(Condition.class).scope();
    }

    public static boolean isCondition(Class<? extends DoorCondition> conClazz) {
        if (conClazz.isAnnotationPresent(Condition.class)) {
            BigDoorsOpener.logger().log(Level.WARNING, "Condition " + ConditionHelper.getName(conClazz)
                            + " is a condition but has no condition annotation.",
                    new RuntimeException("Failed to register condition."));
            return false;
        }
        return true;
    }
}

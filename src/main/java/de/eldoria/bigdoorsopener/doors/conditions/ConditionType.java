package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.Permissions;

public enum ConditionType {
    ITEM_CLICK(ConditionGroup.ITEM),
    ITEM_BLOCK(ConditionGroup.ITEM),
    ITEM_HOLDING(ConditionGroup.ITEM),
    ITEM_OWNING(ConditionGroup.ITEM),
    PROXIMITY(ConditionGroup.LOCATION),
    REGION(ConditionGroup.LOCATION),
    PERMISSION(ConditionGroup.PERMISSION),
    TIME(ConditionGroup.TIME),
    WEATHER(ConditionGroup.WEATHER);

    public final ConditionGroup conditionGroup;
    public final String conditionName;

    ConditionType(ConditionGroup keyParameter) {
        this.conditionGroup = keyParameter;
        this.conditionName = name().replace("_", "").toLowerCase();
    }

    /**
     * Get the type from a string
     * @param keyType string as type
     * @return
     */
    public static ConditionType getType(String keyType) {
        for (ConditionType value : values()) {
            if (value.conditionName.equalsIgnoreCase(keyType)) {
                return value;
            }
        }
        return null;
    }

    public enum ConditionGroup {
        ITEM("item", Item.class, Permissions.ITEM_CONDITION),
        LOCATION("location", Location.class, Permissions.LOCATION_CONDITION),
        PERMISSION("permission", Permission.class, Permissions.PERMISSION_CONDITION),
        TIME("time", Time.class, Permissions.TIME_CONDITION),
        WEATHER("weather", Weather.class, Permissions.WEATHER_CONDITION);

        public final String conditionParameter;
        public final Class<? extends DoorCondition> keyClass;
        public final String permission;

        <T extends DoorCondition> ConditionGroup(String conditionParameter, Class<T> keyClass, String permission) {
            this.conditionParameter = conditionParameter;
            this.keyClass = keyClass;
            this.permission = permission;
        }
    }

    public static <T extends DoorCondition> ConditionGroup getType(Class<T> keyClass) {
        for (ConditionGroup value : ConditionGroup.values()) {
            if (value.keyClass.isAssignableFrom(keyClass)) {
                return value;
            }
        }
        return null;
    }
}

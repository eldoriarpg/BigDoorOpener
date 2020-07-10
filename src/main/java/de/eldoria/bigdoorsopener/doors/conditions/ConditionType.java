package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.Permissions;

public enum ConditionType {
    ITEM_CLICK(ConditionGroup.ITEM, Permissions.ITEM_CLICK_CONDITION),
    ITEM_BLOCK(ConditionGroup.ITEM, Permissions.ITEM_BLOCK_CONDITION),
    ITEM_HOLDING(ConditionGroup.ITEM, Permissions.ITEM_HOLDING_CONDITION),
    ITEM_OWNING(ConditionGroup.ITEM, Permissions.ITEM_OWNING_CONDITION),
    PROXIMITY(ConditionGroup.LOCATION, Permissions.PROXIMITY_CONDITION),
    REGION(ConditionGroup.LOCATION, Permissions.REGION_CONDITION),
    PERMISSION(ConditionGroup.PERMISSION, Permissions.PERMISSION_CONDITION),
    TIME(ConditionGroup.TIME, Permissions.TIME_CONDITION),
    WEATHER(ConditionGroup.WEATHER, Permissions.WEATHER_CONDITION);

    public final ConditionGroup conditionGroup;
    public final String permission;
    public final String conditionName;

    ConditionType(ConditionGroup keyParameter, String permission) {
        this.conditionGroup = keyParameter;
        this.permission = permission;
        this.conditionName = name().replace("_", "").toLowerCase();
    }

    public static ConditionType getType(String keyType) {
        for (ConditionType value : values()) {
            if (value.conditionName.equalsIgnoreCase(keyType)) {
                return value;
            }
        }
        return null;
    }

    public enum ConditionGroup {
        ITEM("item", Item.class),
        LOCATION("location", Location.class),
        PERMISSION("permission", Permission.class),
        TIME("time", Time.class),
        WEATHER("weather", Weather.class);

        public final String conditionParameter;
        public final Class<? extends DoorCondition> keyClass;

        <T extends DoorCondition> ConditionGroup(String conditionParameter, Class<T> keyClass) {
            this.conditionParameter = conditionParameter;
            this.keyClass = keyClass;
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

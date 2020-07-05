package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.conditions.item.ItemCondition;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;

public enum ConditionType {
    ITEM_CLICK(ConditionGroup.ITEM), ITEM_BLOCK(ConditionGroup.ITEM),
    ITEM_HOLDING(ConditionGroup.ITEM), ITEM_OWNING(ConditionGroup.ITEM),
    PROXIMITY(ConditionGroup.LOCATION), REGION(ConditionGroup.LOCATION),
    PERMISSION(ConditionGroup.PERMISSION),
    TIME(ConditionGroup.TIME),
    WEATHER(ConditionGroup.WEATHER);

    public final ConditionGroup conditionGroup;
    public final String keyName;

    ConditionType(ConditionGroup keyParameter) {
        this.conditionGroup = keyParameter;
        this.keyName = name().replace("_", "").toLowerCase();
    }

    public static ConditionType getType(String keyType) {
        for (ConditionType value : values()) {
            if(value.keyName.equalsIgnoreCase(keyType)){
                return value;
            }
        }
        return null;
    }

    public enum ConditionGroup {
        ITEM("itemKey", ItemCondition.class),
        LOCATION("locationKey", Location.class),
        PERMISSION("permissionKey", Permission.class),
        TIME("timeKey", Time.class),
        WEATHER("weatherKey", Weather.class);

        public final String keyParameter;
        public final Class<? extends DoorCondition> keyClass;

        <T extends DoorCondition> ConditionGroup(String keyParameter, Class<T> keyClass) {
            this.keyParameter = keyParameter;
            this.keyClass = keyClass;
        }
    }

    public static <T extends DoorCondition> ConditionGroup getType(Class<T> keyClass) {
        for (ConditionGroup value : ConditionGroup.values()) {
            if(value.keyClass.isAssignableFrom(keyClass)){
                return value;
            }
        }
        return null;
    }
}

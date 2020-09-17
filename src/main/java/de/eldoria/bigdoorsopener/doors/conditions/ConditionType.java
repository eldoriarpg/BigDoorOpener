package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.permission.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.MythicMob;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.Permissions;

@Deprecated
public enum ConditionType {
    ITEM_CLICK(ConditionGroup.ITEM),
    ITEM_BLOCK(ConditionGroup.ITEM),
    ITEM_HOLDING(ConditionGroup.ITEM),
    ITEM_OWNING(ConditionGroup.ITEM),
    PROXIMITY(ConditionGroup.LOCATION),
    REGION(ConditionGroup.LOCATION),
    SIMPLE_REGION(ConditionGroup.LOCATION),
    PERMISSION_NODE(ConditionGroup.PERMISSION),
    DOOR_PERMISSION(ConditionGroup.PERMISSION),
    TIME(ConditionGroup.TIME),
    WEATHER(ConditionGroup.WEATHER),
    PLACEHOLDER(ConditionGroup.PLACEHOLDER),
    MYTHIC_MOBS(ConditionGroup.MYTHIC_MOB);

    public final ConditionGroup conditionGroup;
    public final String conditionName;

    ConditionType(ConditionGroup keyParameter) {
        this.conditionGroup = keyParameter;
        this.conditionName = name().replace("_", "").toLowerCase();
    }

    /**
     * Get the type from a string
     *
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

    public static <T extends DoorCondition> ConditionGroup getType(Class<T> keyClass) {
        for (ConditionGroup value : ConditionGroup.values()) {
            if (value.keyClass.isAssignableFrom(keyClass)) {
                return value;
            }
        }
        return null;
    }

    @Deprecated
    public enum ConditionGroup {
        ITEM("item", "item", "info.item", Item.class, Permissions.ITEM_CONDITION),
        LOCATION("location", "", "info.location", Location.class, Permissions.LOCATION_CONDITION),
        PERMISSION("permission", "permission", "info.permission", Permission.class, Permissions.PERMISSION_CONDITION),
        TIME("time", "time", "info.time", Time.class, Permissions.TIME_CONDITION),
        WEATHER("weather", "weather", "info.weather", Weather.class, Permissions.WEATHER_CONDITION),
        PLACEHOLDER("placeholder", "placeholder", "info.placeholder", Placeholder.class, Permissions.PLACEHOLDER_CONDITION),
        MYTHIC_MOB("mythicMobs", "mythicMobs", "info.mythicmob", MythicMob.class, Permissions.MYTHIC_MOBS);

        public final String conditionParameter;
        public final String infoKey;
        public final Class<? extends DoorCondition> keyClass;
        public final String permission;
        private final String baseSetCommand;

        <T extends DoorCondition> ConditionGroup(String conditionParameter, String baseSetCommand, String infoKey, Class<T> keyClass, String permission) {
            this.conditionParameter = conditionParameter;
            this.baseSetCommand = baseSetCommand;
            this.infoKey = infoKey;
            this.keyClass = keyClass;
            this.permission = permission;
        }

        public String getBaseSetCommand(ConditionalDoor door) {
            return "/bdo setCondition " + door.getDoorUID() + " " + baseSetCommand + " ";
        }
    }
}

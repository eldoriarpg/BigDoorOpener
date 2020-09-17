package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;

/**
 * Class to store permission values.
 * We dont like to store permissions where we need it. Otheriwse changing them would be a real pain.
 */
public final class Permissions {

    // permission to use all commands except reload
    public static final String USE = "bdo.command.use";
    // permission to reload the plugin
    public static final String RELOAD = "bdo.command.reload";
    // permission to use a custom js evaluator
    public static final String CUSTOM_EVALUATOR = "bdo.customEvaluator";
    // permission to access door not owned by the player
    public static final String ACCESS_ALL = "bdo.accessAll";
    // permission to access the item conditions
    public static final String ITEM_CONDITION = "bdo.condition.item";
    // permission to access the location condition
    public static final String LOCATION_CONDITION = "bdo.condition.location";
    // permission to access the permission condition
    public static final String PERMISSION_CONDITION = "bdo.condition.permission";
    // permission to access the time condition
    public static final String TIME_CONDITION = "bdo.condition.time";
    // permission to access the weather condition
    public static final String WEATHER_CONDITION = "bdo.condition.weather";
    // permission to access the placeholder condition
    public static final String PLACEHOLDER_CONDITION = "bdo.condition.placeholder";
    // permission to acces the mythicMobs condition
    public static final String MYTHIC_MOBS = "bdo.condition.mythicMobs";
    // permission to access all conditions
    public static final String ALL_CONDITION = "bdo.condition.all";

    private Permissions() {
    }

    public static String getConditionPermission(ConditionContainer container) {
        return getConditionPermission(container.getGroup());
    }

    public static String getConditionPermission(String group) {
        return "bdo.condition." + group;
    }
}

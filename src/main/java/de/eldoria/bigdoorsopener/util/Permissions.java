/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;

/**
 * Class to store permission values. We dont like to store permissions where we need it. Otheriwse changing them would
 * be a real pain.
 */
public final class Permissions {
    public static final String USE = "bdo.command.use";
    public static final String GIVE_KEY = "bdo.command.givekey";
    public static final String RELOAD = "bdo.command.reload";
    public static final String CUSTOM_EVALUATOR = "bdo.customEvaluator";
    public static final String ACCESS_ALL = "bdo.accessAll";
    public static final String ITEM_CONDITION = "bdo.condition.item";
    public static final String LOCATION_CONDITION = "bdo.condition.location";
    public static final String PERMISSION_CONDITION = "bdo.condition.permission";
    public static final String TIME_CONDITION = "bdo.condition.time";
    public static final String WEATHER_CONDITION = "bdo.condition.weather";
    public static final String PLACEHOLDER_CONDITION = "bdo.condition.placeholder";
    public static final String MYTHIC_MOBS = "bdo.condition.mythicMob";
    public static final String ALL_CONDITION = "bdo.condition.all";
    public static final String UPDATES = "bdo.updates";

    private Permissions() {
    }

    public static String getConditionPermission(ConditionContainer container) {
        return getConditionPermission(container.getGroup());
    }

    public static String getConditionPermission(String group) {
        return "bdo.condition." + group;
    }
}

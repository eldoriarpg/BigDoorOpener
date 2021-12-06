/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.door.conditioncollections;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.Item;
import de.eldoria.bigdoorsopener.conditions.location.Location;
import de.eldoria.bigdoorsopener.conditions.permission.Permission;
import de.eldoria.bigdoorsopener.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs.MythicMob;
import de.eldoria.bigdoorsopener.conditions.standalone.weather.Weather;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.ConditionChainEvaluator;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * A condition chain represents a set of multiple conditions.
 *
 * @deprecated This class is only present for serialization reasons and will be removed in the next major release. Use
 * {@link ConditionBag} instead
 */
@SerializableAs("conditionChain")
@Deprecated
public class ConditionChain implements ConditionCollection {
    private Item item = null;
    private Location location = null;
    private Permission permission = null;
    private Time time = null;
    private Weather weather = null;
    private Placeholder placeholder = null;
    private MythicMob mythicMob = null;

    public ConditionChain() {
    }

    private ConditionChain(Item item, Location location, Permission permission, Time time, Weather weather, Placeholder placeholder, MythicMob mythicMob) {
        this.item = item;
        this.location = location;
        this.permission = permission;
        this.time = time;
        this.weather = weather;
        this.mythicMob = mythicMob;
    }

    public static ConditionChain deserialize(Map<String, Object> objectMap) {
        TypeResolvingMap map = SerializationUtil.mapOf(objectMap);
        Item item = map.getValue("item");
        Location location = map.getValue("location");
        Permission permission = map.getValue("permission");
        Time time = map.getValue("time");
        Weather weather = map.getValue("weather");
        Placeholder placeholder = map.getValue("placeholder");
        MythicMob mythicMob = map.getValue("mythicMob");
        return new ConditionChain(item, location, permission, time, weather, placeholder, mythicMob);
    }

    /**
     * Evaluates the conditions with an or operator.
     *
     * @param player       player which should be checked
     * @param world        world of the door
     * @param door         door which is checked
     * @param currentState the current state of the door
     * @return result of the conditions.
     */
    @Override
    public boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        // the conditions should be evaluated from the simpelest to the most expensive computation.
        return ConditionChainEvaluator.or(player, world, door, currentState,
                getConditions());
    }

    /**
     * Evaluates the conditions with an and operator.
     *
     * @param player       player which should be checked
     * @param world        world of the door
     * @param door         door which is checked
     * @param currentState the current state of the door
     * @return result of the conditions.
     */
    @Override
    public boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        // the conditions should be evaluated from the simpelest to the most expensive computation.
        return ConditionChainEvaluator.and(player, world, door, currentState,
                getConditions());
    }

    /**
     * Evaluates the chain with a custom evaluation string.
     *
     * @param string       evaluator.
     * @param player       player which should be checked
     * @param world        world of the door
     * @param door         door which is checked
     * @param currentState the current state of the door
     * @return string with the values replaced.
     */
    @Override
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        return "null";
    }

    /**
     * Checks if a key is present which needs a player lookup.
     *
     * @return true if a player key is present.
     */
    @Override
    public boolean requiresPlayerEvaluation() {
        return item != null || permission != null || location != null || placeholder != null;
    }

    /**
     * Called when the door was evaluated and a new evaluation cycle begins.
     */
    @Override

    public void evaluated() {
        if (item != null) {
            item.evaluated();
        }
    }

    /**
     * Called when the chain was true and the door was opened.
     *
     * @param player player which opened the door.
     */
    @Override

    public void opened(Player player) {
        if (item != null) {
            item.opened(player);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("item", item)
                .add("permission", permission)
                .add("location", location)
                .add("time", time)
                .add("weather", weather)
                .add("placeholder", placeholder)
                .add("mythicMob", mythicMob)
                .build();
    }

    /**
     * Checks if all conditions are null.
     *
     * @return true if all conditions are nulkl
     */
    @Override

    public boolean isEmpty() {
        for (DoorCondition condition : getConditions()) {
            if (condition != null) return false;
        }
        return true;
    }

    /**
     * Get a mutable new condition chain with the same conditions like this condition chain.
     *
     * @return new condition chain.
     */
    @Override

    public ConditionChain copy() {
        return new ConditionChain(C.nonNullOrElse(item, Item::clone, null), C.nonNullOrElse(location, Location::clone, null),
                C.nonNullOrElse(permission, Permission::clone, null), C.nonNullOrElse(time, Time::clone, null),
                C.nonNullOrElse(weather, Weather::clone, null), C.nonNullOrElse(placeholder, Placeholder::clone, null),
                C.nonNullOrElse(mythicMob, MythicMob::clone, null)
        );
    }

    /**
     * Get the conditions in a order from the less expensive to the most expensive computation time
     *
     * @return array of conditions. May contain null values.
     */
    @Override

    public Collection<DoorCondition> getConditions() {
        return Arrays.asList(location, time, weather, mythicMob, permission, item, placeholder);
    }

    public Item item() {
        return item;
    }

    public void item(Item item) {
        this.item = item;
    }

    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;
    }

    public Permission permission() {
        return permission;
    }

    public void permission(Permission permission) {
        this.permission = permission;
    }

    public Time time() {
        return time;
    }

    public void time(Time time) {
        this.time = time;
    }

    public Weather weather() {
        return weather;
    }

    public void weather(Weather weather) {
        this.weather = weather;
    }

    public Placeholder placeholder() {
        return placeholder;
    }

    public void placeholder(Placeholder placeholder) {
        this.placeholder = placeholder;
    }

    public MythicMob mythicMob() {
        return mythicMob;
    }

    public void mythicMob(MythicMob mythicMob) {
        this.mythicMob = mythicMob;
    }
}

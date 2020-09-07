package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.permission.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.MythicMob;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.ConditionChainEvaluator;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A condition chain represents a set of multiple conditions.
 */
@Setter
@Getter
@SerializableAs("conditionChain")
public class ConditionChain implements ConfigurationSerializable, Cloneable {
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

    public static ConditionChain deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        Item item = resolvingMap.getValue("item");
        Location location = resolvingMap.getValue("location");
        Permission permission = resolvingMap.getValue("permission");
        Time time = resolvingMap.getValue("time");
        Weather weather = resolvingMap.getValue("weather");
        Placeholder placeholder = resolvingMap.getValueOrDefault("placeholder", null);
        MythicMob mythicMob = resolvingMap.getValueOrDefault("mythicMob", null);
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
    public boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        // the conditions should be evaluated from the simpelest to the most expensive computation.
        return ConditionChainEvaluator.or(player, world, door, currentState,
                this);
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
    public boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        // the conditions should be evaluated from the simpelest to the most expensive computation.
        return ConditionChainEvaluator.and(player, world, door, currentState,
                this);
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
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorCondition doorCondition : getConditions()) {
            if (doorCondition == null) {
                continue;
            }
            ConditionType.ConditionGroup condition = ConditionType.getType(doorCondition.getClass());
            if (condition == null) {
                BigDoorsOpener.logger().warning("Class " + doorCondition.getClass().getSimpleName() + " is not registered as condition type."
                        + doorCondition.getClass().getSimpleName());
                continue;
            }
            Boolean state;

            if (doorCondition.getScope() == ConditionScope.Scope.PLAYER
                    && player == null) {
                state = false;
            } else {
                state = doorCondition.isOpen(player, world, door, currentState);
            }
            evaluationString = evaluationString.replaceAll("(?i)" + condition.conditionParameter,
                    String.valueOf(state));
        }

        evaluationString = evaluationString.replaceAll("(?i)currentState",
                String.valueOf(currentState));

        // make sure that calculation does not fail even when the condition is not set.
        for (ConditionType.ConditionGroup value : ConditionType.ConditionGroup.values()) {
            evaluationString = evaluationString.replaceAll("(?i)" + value.conditionParameter,
                    "null");
        }

        return evaluationString;
    }

    /**
     * Checks if a key is present which needs a player lookup.
     *
     * @return true if a player key is present.
     */
    public boolean requiresPlayerEvaluation() {
        return item != null || permission != null || location != null || placeholder != null;
    }

    /**
     * Called when the door was evaluated and a new evaluation cycle begins.
     */
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
    public void opened(Player player) {
        if (item != null) {
            item.used(player);
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
    public DoorCondition[] getConditions() {
        return new DoorCondition[] {location, time, weather, mythicMob, permission, item, placeholder};
    }

    /**
     * Get the conditions wrapped to identify them.
     *
     * @return List of wrappet conditions. conditions may be null.
     */
    public List<Pair<DoorCondition, ConditionType.ConditionGroup>> getConditionsWrapped() {
        return Arrays.asList(
                new Pair<>(location, ConditionType.ConditionGroup.LOCATION),
                new Pair<>(permission, ConditionType.ConditionGroup.PERMISSION),
                new Pair<>(time, ConditionType.ConditionGroup.TIME),
                new Pair<>(weather, ConditionType.ConditionGroup.WEATHER),
                new Pair<>(item, ConditionType.ConditionGroup.ITEM),
                new Pair<>(placeholder, ConditionType.ConditionGroup.PLACEHOLDER),
                new Pair<>(mythicMob, ConditionType.ConditionGroup.MYTHIC_MOB));
    }

    /**
     * Get a condition via enum value
     *
     * @param group group to get
     * @return condition
     */
    public DoorCondition getCondition(ConditionType.ConditionGroup group) {
        switch (group) {
            case ITEM:
                return item;
            case LOCATION:
                return location;
            case PERMISSION:
                return permission;
            case TIME:
                return time;
            case WEATHER:
                return weather;
            case PLACEHOLDER:
                return placeholder;
            case MYTHIC_MOB:
                return mythicMob;
            default:
                throw new IllegalStateException("Unexpected value: " + group);
        }
    }

    /**
     * Set a condition via enum value.
     *
     * @param group     group to set
     * @param condition condition to set
     */
    public void setCondition(ConditionType.ConditionGroup group, @Nullable DoorCondition condition) {
        if (condition == null) {
            removeCondition(group);
            return;
        }

        switch (group) {
            case ITEM:
                item = (Item) condition;
                break;
            case LOCATION:
                location = (Location) condition;
                break;
            case PERMISSION:
                permission = (Permission) condition;
                break;
            case TIME:
                time = (Time) condition;
                break;
            case WEATHER:
                weather = (Weather) condition;
                break;
            case PLACEHOLDER:
                placeholder = (Placeholder) condition;
                break;
            case MYTHIC_MOB:
                mythicMob = (MythicMob) condition;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + group);
        }
    }

    /**
     * Remove a condition by enum value.
     *
     * @param group condition group to remove
     */
    public void removeCondition(ConditionType.ConditionGroup group) {
        switch (group) {
            case ITEM:
                item = null;
                break;
            case LOCATION:
                location = null;
                break;
            case PERMISSION:
                permission = null;
                break;
            case TIME:
                time = null;
                break;
            case WEATHER:
                weather = null;
                break;
            case PLACEHOLDER:
                placeholder = null;
                break;
            case MYTHIC_MOB:
                mythicMob = null;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + group);
        }
    }


}

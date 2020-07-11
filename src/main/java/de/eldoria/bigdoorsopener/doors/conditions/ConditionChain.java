package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.ConditionChainEvaluator;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * A key chain represents a set of multiple keys.
 */
@Setter
@Getter
public class ConditionChain implements ConfigurationSerializable {
    private Item item = null;
    private Location location = null;
    private Permission permission = null;
    private Time time = null;
    private Weather weather = null;

    public ConditionChain() {
    }

    private ConditionChain(Item item, Location location, Permission permission, Time time, Weather weather) {
        this.item = item;
        this.location = location;
        this.permission = permission;
        this.time = time;
        this.weather = weather;
    }

    public boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        return ConditionChainEvaluator.or(player, world, door, currentState,
                item, permission, location, time, weather);
    }

    public boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        return ConditionChainEvaluator.and(player, world, door, currentState,
                item, permission, location, time, weather);
    }

    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorCondition doorCondition : Arrays.asList(item, permission, location, time, weather)) {
            if (doorCondition == null) {
                continue;
            }
            ConditionType.ConditionGroup condition = ConditionType.getType(doorCondition.getClass());
            if (condition == null) {
                BigDoorsOpener.logger().warning("Class " + doorCondition.getClass().getSimpleName() + " is not registered as condition type."
                        + doorCondition.getClass().getSimpleName());
                continue;
            }
            evaluationString = evaluationString.replaceAll("(?i)" + condition.conditionParameter,
                    String.valueOf(item.isOpen(player, world, door, currentState)));
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

    public boolean requiresPlayerEvaluation() {
        return item != null || permission != null || location != null;
    }

    public void evaluated() {
        if (item != null) {
            item.evaluated();
        }
    }

    public void opened(Player player) {
        item.used(player);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("item", item)
                .add("permission", permission)
                .add("location", location)
                .add("time", time)
                .add("weather", weather)
                .build();

    }

    public static ConditionChain deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        Item item = resolvingMap.getValue("item");
        Location location = resolvingMap.getValue("location");
        Permission permission = resolvingMap.getValue("permission");
        Time time = resolvingMap.getValue("time");
        Weather weather = resolvingMap.getValue("weather");
        return new ConditionChain(item, location, permission, time, weather);
    }

    public boolean isEmpty() {
        return item == null && location == null && permission == null && time == null && weather == null;
    }
}

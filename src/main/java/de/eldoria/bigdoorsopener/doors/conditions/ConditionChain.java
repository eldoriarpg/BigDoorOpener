package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemCondition;
import de.eldoria.bigdoorsopener.doors.conditions.location.Location;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.util.KeyChainEvaluator;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * A key chain represents a set of multiple keys.
 */
@Setter
@Getter
public class ConditionChain {
    private ItemCondition itemKey;
    private Permission permission;
    private Location location;
    private Time time;
    private Weather weather;

    public boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        return KeyChainEvaluator.or(player, world, door, currentState,
                itemKey, permission, location, time, weather);
    }

    public boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        return KeyChainEvaluator.and(player, world, door, currentState,
                itemKey, permission, location, time, weather);
    }

    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorCondition doorCondition : Arrays.asList(itemKey, permission, location, time, weather)) {
            if (doorCondition == null) continue;
            ConditionType.ConditionGroup key = ConditionType.getType(doorCondition.getClass());
            if (key == null) {
                BigDoorsOpener.logger().warning("Class " + doorCondition.getClass().getSimpleName() + " is not registered as key type."
                        + doorCondition.getClass().getSimpleName());
                continue;
            }
            evaluationString = evaluationString.replaceAll("(?i)" + key.keyParameter,
                    String.valueOf(itemKey.isOpen(player, world, door, currentState)));
        }

        evaluationString = evaluationString.replaceAll("(?i)currentState",
                String.valueOf(currentState));

        return evaluationString;
    }

    public boolean requiresPlayerEvaluation() {
        return itemKey != null || permission != null || location != null;
    }

    public void evaluated() {
        if (itemKey != null) {
            itemKey.clear();
        }
    }

    public void opened(Player player) {
        itemKey.used(player);
    }
}

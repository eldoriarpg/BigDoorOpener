package de.eldoria.bigdoorsopener.doors.doorkey;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.item.ItemKey;
import de.eldoria.bigdoorsopener.doors.doorkey.location.LocationKey;
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
public class KeyChain {
    private ItemKey itemKey;
    private PermissionKey permissionKey;
    private LocationKey locationKey;
    private TimeKey timeKey;
    private WeatherKey weatherKey;

    public boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        return KeyChainEvaluator.or(player, world, door, currentState,
                itemKey, permissionKey, locationKey, timeKey, weatherKey);
    }

    public boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        return KeyChainEvaluator.and(player, world, door, currentState,
                itemKey, permissionKey, locationKey, timeKey, weatherKey);
    }

    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorKey doorKey : Arrays.asList(itemKey, permissionKey, locationKey, timeKey, weatherKey)) {
            if (doorKey == null) continue;
            if (!doorKey.getClass().isAnnotationPresent(KeyParameter.class)) {
                BigDoorsOpener.logger().warning("Key Parameter annotation is missing on class: "
                        + doorKey.getClass().getSimpleName());
            }
            String key = doorKey.getClass().getAnnotation(KeyParameter.class).value();
            evaluationString = evaluationString.replaceAll("(?i)" + key,
                    String.valueOf(itemKey.isOpen(player, world, door, currentState)));
        }

        evaluationString = evaluationString.replaceAll("(?i)currentState",
                String.valueOf(currentState));

        return evaluationString;
    }

    public boolean requiresPlayerEvaluation() {
        return itemKey != null || permissionKey != null || locationKey != null;
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

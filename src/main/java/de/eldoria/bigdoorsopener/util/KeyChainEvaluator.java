package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.DoorKey;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class KeyChainEvaluator {
    private Boolean current;

    public KeyChainEvaluator or(DoorKey doorKey, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorKey == null) return this;

        if (current == null) {
            current = doorKey.isOpen(player, world, door, currentState);
        }

        current = current && doorKey.isOpen(player, world, door, currentState);
        return this;
    }

    public KeyChainEvaluator and(DoorKey doorKey, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorKey == null) return this;

        if (current == null) {
            current = doorKey.isOpen(player, world, door, currentState);
        }

        current = current && doorKey.isOpen(player, world, door, currentState);
        return this;
    }

    public boolean result(boolean currentState) {
        return current == null ? currentState : current;
    }

    public static boolean or(Player player, World world, ConditionalDoor door, boolean currentState, DoorKey... keys) {
        KeyChainEvaluator evaluator = new KeyChainEvaluator();
        for (DoorKey doorKey : keys) {
            evaluator.or(doorKey, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }

    public static boolean and(Player player, World world, ConditionalDoor door, boolean currentState, DoorKey... keys) {
        KeyChainEvaluator evaluator = new KeyChainEvaluator();
        for (DoorKey doorKey : keys) {
            evaluator.and(doorKey, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }
}

package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ConditionChainEvaluator {
    private Boolean current;

    public ConditionChainEvaluator or(DoorCondition doorCondition, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorCondition == null) return this;

        Boolean open = doorCondition.isOpen(player, world, door, currentState);
        if (open == null) {
            return this;
        }

        if (current == null) {
            current = open;
            return this;
        }

        if (current) return this;

        current = open;
        return this;
    }

    public ConditionChainEvaluator and(DoorCondition doorCondition, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorCondition == null) return this;

        Boolean open = doorCondition.isOpen(player, world, door, currentState);
        if (open == null) {
            return this;
        }
        if (current == null) {
            current = open;
            return this;
        }

        if (!current) return this;

        current = open;
        return this;
    }

    public boolean result(boolean currentState) {
        return current == null ? currentState : current;
    }

    public static boolean or(Player player, World world, ConditionalDoor door, boolean currentState, DoorCondition... keys) {
        ConditionChainEvaluator evaluator = new ConditionChainEvaluator();
        for (DoorCondition doorCondition : keys) {
            evaluator.or(doorCondition, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }

    public static boolean and(Player player, World world, ConditionalDoor door, boolean currentState, DoorCondition... keys) {
        ConditionChainEvaluator evaluator = new ConditionChainEvaluator();
        for (DoorCondition doorCondition : keys) {
            evaluator.and(doorCondition, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }
}

package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.doors.Condition;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionHelper;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ConditionChainEvaluator {
    private Boolean current;

    /**
     * Evaluates the conditions with an or operator.
     *
     * @param player       player to check the condition for
     * @param world        world the door is in
     * @param door         the acutal door which is checked
     * @param currentState the current state of the door
     * @param conditions   conditions to evaluate
     * @return the result of the chain evaluator
     */
    public static boolean or(Player player, World world, ConditionalDoor door, boolean currentState, Collection<DoorCondition> conditions) {
        ConditionChainEvaluator evaluator = new ConditionChainEvaluator();
        for (DoorCondition doorCondition : conditions) {
            evaluator.or(doorCondition, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }

    /**
     * Evaluates the conditions with an AND operator.
     *
     * @param player       player to check the condition for
     * @param world        world the door is in
     * @param door         the acutal door which is checked
     * @param currentState the current state of the door
     * @param conditions   conditions to evaluate
     * @return the result of the chain evaluator
     */
    public static boolean and(Player player, World world, ConditionalDoor door, boolean currentState, Collection<DoorCondition> conditions) {
        ConditionChainEvaluator evaluator = new ConditionChainEvaluator();
        for (DoorCondition doorCondition : conditions) {
            evaluator.and(doorCondition, player, world, door, currentState);
        }

        return evaluator.result(currentState);
    }

    /**
     * Evaluates the condition of a door with an OR operator.
     *
     * @param doorCondition condition to evaluate
     * @param player        player to check the condition for
     * @param world         world the door is in
     * @param door          the acutal door which is checked
     * @param currentState  the current state of the door
     * @return chain evaluator with result evaluated.
     */
    public ConditionChainEvaluator or(DoorCondition doorCondition, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorCondition == null) return this;

        if (current != null && current) return this;

        Boolean open;

        if (ConditionHelper.getScope(doorCondition.getClass()) == Condition.Scope.PLAYER && player == null) {
            open = false;
        } else {
            open = doorCondition.isOpen(player, world, door, currentState);
        }

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

    /**
     * Evaluates the condition of a door with an AND operator.
     *
     * @param doorCondition condition to evaluate
     * @param player        player to check the condition for
     * @param world         world the door is in
     * @param door          the acutal door which is checked
     * @param currentState  the current state of the door
     * @return chain evaluator with result evaluated.
     */
    public ConditionChainEvaluator and(DoorCondition doorCondition, Player player, World world, ConditionalDoor door, boolean currentState) {
        if (doorCondition == null) return this;

        if (current != null && !current) return this;

        Boolean open;

        if (ConditionHelper.getScope(doorCondition.getClass()) == Condition.Scope.PLAYER && player == null) {
            open = false;
        } else {
            open = doorCondition.isOpen(player, world, door, currentState);
        }

        if (open == null) {
            return this;
        }

        current = open;
        return this;
    }

    /**
     * Get the result of the chain
     *
     * @param currentState the current state of the door
     * @return the result or the current state if the result is null.
     */
    public boolean result(boolean currentState) {
        return current == null ? currentState : current;
    }
}

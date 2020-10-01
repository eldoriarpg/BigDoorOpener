package de.eldoria.bigdoorsopener.door.conditioncollections;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.ConditionChainEvaluator;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface ConditionCollection extends ConfigurationSerializable, Cloneable {
    /**
     * Evaluates the conditions with an or operator.
     *
     * @param player       player which should be checked
     * @param world        world of the door
     * @param door         door which is checked
     * @param currentState the current state of the door
     * @return result of the conditions.
     */
    default boolean or(Player player, World world, ConditionalDoor door, boolean currentState) {
        return ConditionChainEvaluator.or(player, world, door, currentState, getConditions());
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
    default boolean and(Player player, World world, ConditionalDoor door, boolean currentState) {
        return ConditionChainEvaluator.and(player, world, door, currentState, getConditions());
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
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState);

    /**
     * Called when the door was evaluated and a new evaluation cycle begins.
     */
    public void evaluated();

    /**
     * Called when the chain was true and the door was opened.
     *
     * @param player player which opened the door.
     */
    public void opened(Player player);

    /**
     * Checks if a key is present which needs a player lookup.
     *
     * @return true if a player key is present.
     */
    public boolean requiresPlayerEvaluation();

    /**
     * Checks if all conditions are null.
     *
     * @return true if all conditions are nulkl
     */
    public boolean isEmpty();

    /**
     * Get a mutable new condition chain with the same conditions like this condition chain.
     *
     * @return new condition chain.
     */
    public ConditionCollection copy();

    /**
     * Get the conditions in a order from the less expensive to the most expensive computation time
     *
     * @return array of conditions. May contain null values.
     */
    public Collection<DoorCondition> getConditions();
}

package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

/**
 * A interface which represents a condition which opens a door under specific circumstances.
 */
public interface DoorCondition extends ConfigurationSerializable, Cloneable {
    String SET_COMMAND = "/bdo setCondition ";
    String REMOVE_COMMAND = "/bdo removeCondition ";

    /**
     * Indicates if the key would open the door under the current circumstances.
     *
     * @param player       player which should be checked.
     * @param world        world of the door
     * @param door         door data
     * @param currentState the current state of the door.
     * @return true if the key settings are matched.
     */
    Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState);

    /**
     * Get the description of the door condition.
     *
     * @param localizer localizer instance for translation
     * @return text component with description.
     */
    TextComponent getDescription(Localizer localizer);

    /**
     * Get the command to set this condition with the current settings.
     *
     * @param door door of condition
     * @return creation command as string.
     */
    String getCreationCommand(ConditionalDoor door);

    /**
     * Get the command to remove this condition
     *
     * @param door door of condition
     * @return creation command as string.
     */
    String getRemoveCommand(ConditionalDoor door);

    /**
     * This method is called after the check for the door of this condition is done and a new evaluation cycle starts.
     * Deletes any internal data in this key.
     */
    void evaluated();

    DoorCondition clone();

    /**
     * Returns the scope of the condition.
     * @return condition scope
     */
    default ConditionScope.Scope getScope() {
        return getClass().getAnnotation(ConditionScope.class).value();
    }
}

package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

public interface DoorCondition extends ConfigurationSerializable {
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

    TextComponent getDescription(Localizer localizer);
}

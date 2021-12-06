/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A interface which represents a condition which opens a door under specific circumstances.
 */
public interface DoorCondition extends ConfigurationSerializable, Cloneable {
    String SET_COMMAND = "/bdo setCondition ";
    String REMOVE_COMMAND = "/bdo removeCondition ";

    static ILocalizer localizer() {
        return BigDoorsOpener.localizer();
    }

    /**
     * Indicates if the key would open the door under the current circumstances.
     *
     * @param player       player which should be checked. Is null if the condition is {@link Scope#WORLD}
     * @param world        world of the door
     * @param door         door data
     * @param currentState the current state of the door.
     * @return true if the key settings are matched.
     */
    Boolean isOpen(@Nullable Player player, @NotNull World world, @NotNull ConditionalDoor door, @NotNull boolean currentState);

    /**
     * Get the description of the door condition.
     *
     * @param localizer localizer instance for translation
     * @return text component with description.
     */
    Component getDescription(ILocalizer localizer);

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
     * Deletes any internal data in this condition
     */
    default void evaluated() {
    }

    DoorCondition clone();

    /**
     * This method will be called when a door with this key was opened. Only once. This method will only be called, when
     * the {@link ConditionContainer#getScope()} is set to {@link Scope#PLAYER}.
     *
     * @param player player which opened the door.
     */
    default void opened(Player player) {
    }
}

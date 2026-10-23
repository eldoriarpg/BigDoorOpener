/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.ConditionTypeResolver;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * A interface which represents a condition which opens a door under specific circumstances.
 * <p>
 * Conditions are stored with Jackson. The {@code type} property holds the type name of the registered
 * {@link ConditionContainer}. Fields which only hold runtime state have to be ignored.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeIdResolver(ConditionTypeResolver.class)
public interface DoorCondition extends Cloneable {
    String SET_COMMAND = "/bdo setCondition ";
    String REMOVE_COMMAND = "/bdo removeCondition ";

    static ILocalizer localizer() {
        return BigDoorsOpener.localizer();
    }

    /**
     * Builds the headline of a condition description from a message with a {@code <name>} placeholder, which is
     * replaced by the registered name of the condition.
     *
     * @param localizer localizer for the message
     * @param key       message key of the headline
     * @param clazz     class of the condition
     * @return headline component
     */
    static Component headline(ILocalizer localizer, String key, Class<? extends DoorCondition> clazz) {
        String name = ConditionRegistrar.getContainerByClass(clazz)
                .map(ConditionContainer::getName)
                .orElse("undefined");
        return MiniMessage.miniMessage()
                .deserialize(localizer.getMessage(key), Placeholder.unparsed("name", name))
                .colorIfAbsent(NamedTextColor.AQUA);
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
    Boolean isOpen(
            @Nullable Player player,
            @NotNull World world,
            @NotNull ConditionalDoor door,
            @NotNull boolean currentState);

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
    default void evaluated() {}

    DoorCondition clone();

    /**
     * This method will be called when a door with this key was opened. Only once. This method will only be called, when
     * the {@link ConditionContainer#getScope()} is set to {@link Scope#PLAYER}.
     *
     * @param player player which opened the door.
     */
    default void opened(Player player) {}
}

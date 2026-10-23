/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.conditions;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

/**
 * Creates a condition from the arguments of a set or add command.
 * <p>
 * A factory must follow these rules:
 * <ul>
 *     <li>Hand the created condition to the consumer and confirm it to the sender via the message sender.</li>
 *     <li>Throw a {@link CommandException} when the condition can not be created. Its message is sent as error.</li>
 *     <li>Never remove a condition from the bag and never save the configuration.</li>
 *     <li>The arguments only contain the input for the condition itself and may be empty.</li>
 * </ul>
 */
@FunctionalInterface
public interface ConditionFactory {
    /**
     * Creates the condition.
     *
     * @param sender        sender of the command
     * @param messageSender message sender of the plugin
     * @param args          arguments for the condition
     * @param conditionBag  receives the created condition
     * @throws CommandException when the input is invalid or the sender can not create this condition
     */
    void create(CommandSender sender, MessageSender messageSender, Arguments args, Consumer<DoorCondition> conditionBag)
            throws CommandException;
}

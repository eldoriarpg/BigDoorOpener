/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.conditions;

import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Checks shared by the factories of the conditions.
 */
public final class ConditionInput {
    private static final String CONDITION_SYNTAX = "<$syntax.doorId$> <$syntax.condition$> ";

    private ConditionInput() {
    }

    /**
     * Requires a minimum amount of condition arguments.
     *
     * @param args      arguments of the condition
     * @param length    minimum amount of arguments
     * @param localizer localizer for the syntax
     * @param syntax    syntax of the condition arguments, with message keys enclosed in {@code $}
     * @throws CommandException with the full syntax of the command when arguments are missing
     */
    public static void require(Arguments args, int length, ILocalizer localizer, String syntax) throws CommandException {
        if (args.size() >= length) return;
        throw CommandException.message("error.invalidArguments",
                Replacement.unparsed("SYNTAX", localize(localizer, CONDITION_SYNTAX + syntax)));
    }

    /**
     * Requires the sender to be a player.
     *
     * @param sender sender of the command
     * @return the sender as player
     * @throws CommandException when the sender is not a player
     */
    public static Player player(CommandSender sender) throws CommandException {
        if (sender instanceof Player player) return player;
        throw CommandException.message("error.notAllowedFromConsole");
    }

    /**
     * Replaces message keys enclosed in {@code $} with their message.
     *
     * @param localizer localizer for the messages
     * @param template  text with message keys
     * @return localized text
     */
    public static String localize(ILocalizer localizer, String template) {
        StringBuilder result = new StringBuilder();
        String[] parts = template.split("\\$", -1);
        for (int i = 0; i < parts.length; i++) {
            result.append(i % 2 == 1 ? localizer.getMessage(parts[i]) : parts[i]);
        }
        return result.toString();
    }
}

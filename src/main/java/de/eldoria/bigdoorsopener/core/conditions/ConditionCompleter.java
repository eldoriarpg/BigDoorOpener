/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.conditions;

import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.localization.ILocalizer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Completes the arguments of a condition in the set and add commands.
 */
@FunctionalInterface
public interface ConditionCompleter {
    /**
     * Completes the last argument.
     *
     * @param sender    sender of the command
     * @param localizer localizer for argument hints
     * @param args      arguments for the condition, including the partial argument to complete
     * @return possible completions of the last argument
     */
    List<String> complete(CommandSender sender, ILocalizer localizer, Arguments args);
}

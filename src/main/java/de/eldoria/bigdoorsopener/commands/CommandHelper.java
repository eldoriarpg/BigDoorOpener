/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.commands;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandHelper {

    private CommandHelper() {
    }

    /**
     * Checks if the provided arguments are invalid.
     *
     * @param sender user which executed the command.
     * @param args   arguments to check
     * @param length min amount of arguments.
     * @param syntax correct syntax
     * @return true if the arguments are invalid
     */
    public static boolean argumentsInvalid(CommandSender sender, String[] args, int length, String syntax) {
        return argumentsInvalid(sender, BigDoorsOpener.getPluginMessageSender(), BigDoorsOpener.localizer(), args, length, syntax);
    }

    /**
     * Checks if the provided arguments are invalid.
     *
     * @param sender        user which executed the command.
     * @param messageSender message sender for calling home.
     * @param localizer     localizer for localization stuff.
     * @param args          arguments to check
     * @param length        min amount of arguments.
     * @param syntax        correct syntax
     * @return true if the arguments are invalid
     */
    public static boolean argumentsInvalid(CommandSender sender, MessageSender messageSender, ILocalizer localizer, String[] args, int length, String syntax) {
        if (args.length < length) {
            messageSender.sendError(sender, localizer.getMessage("error.invalidArguments",
                    Replacement.create("SYNTAX", syntax).addFormatting('6')));
            return true;
        }
        return false;
    }


    public static boolean denyAccess(CommandSender sender, String... permissions) {
        return denyAccess(sender, false, permissions);
    }

    public static boolean denyAccess(CommandSender sender, boolean silent, String... permissions) {
        if (sender == null) {
            return false;
        }

        Player player = null;

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (player == null) {
            return false;
        }
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return false;
            }
        }
        if (!silent) {
            BigDoorsOpener.getPluginMessageSender().sendLocalizedError(player,
                    "error.permission",
                    Replacement.create("PERMISSION", String.join(", ", permissions)).addFormatting('6'));
        }
        return true;
    }

    public static Player getPlayerFromSender(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }
}

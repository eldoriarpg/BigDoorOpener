package de.eldoria.bigdoorsopener.util;

import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.entity.Player;

public final class ArgumentHelper {
    private ArgumentHelper() {
    }

    /**
     * Checks if the provided arguments are invalid.
     *
     * @param player        player which executed the command.
     * @param messageSender message sender for calling home.
     * @param localizer     localizer for localization stuff.
     * @param args          arguments to check
     * @param length        min amount of arguments.
     * @param syntax        correct syntax
     * @return true if the arguments are invalid
     */
    public static boolean argumentsInvalid(Player player, MessageSender messageSender, Localizer localizer, String[] args, int length, String syntax) {
        if (args.length < length) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments",
                    Replacement.create("SYNTAX", syntax).addFormatting('6')));
            return true;
        }
        return false;
    }

}

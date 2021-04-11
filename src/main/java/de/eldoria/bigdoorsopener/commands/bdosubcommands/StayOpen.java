package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.utils.Parser;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class StayOpen extends BigDoorsAdapterCommand {

    public StayOpen(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<$syntax.doorId$> <$syntax.seconds$>")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);

        if (door == null) {
            return true;
        }

        OptionalInt optionalInt = Parser.parseInt(args[1]);
        if (!optionalInt.isPresent()) {
            messageSender().sendLocalizedError(sender, "error.invalidAmount");
            return true;
        }
        door.first.setStayOpen(optionalInt.getAsInt());
        messageSender().sendLocalizedMessage(sender, "stayOpen.message",
                Replacement.create("SECONDS", optionalInt.getAsInt()));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return Collections.singletonList("<" + localizer().getMessage("syntax.amount") + ">");
        }
        return Collections.emptyList();
    }
}

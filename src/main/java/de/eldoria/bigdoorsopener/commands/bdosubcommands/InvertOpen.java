package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class InvertOpen extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final MessageSender messageSender;

    public InvertOpen(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 1, "<" + localizer.getMessage("syntax.doorId") + ">")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);
        if (door == null) {
            return true;
        }

        door.first.invertOpen();
        messageSender.sendMessage(sender, localizer.getMessage("invertOpen.inverted"));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        return Collections.emptyList();
    }
}

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
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

public class CloneDoor extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final MessageSender messageSender;

    public CloneDoor(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) return true;
        if (denyAccess(sender, Permissions.ALL_CONDITION)) return true;

        if (argumentsInvalid(sender, args, 2,
                "<" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + ">")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Door playerSourceDoor = getPlayerDoor(args[0], playerFromSender);

        if (playerSourceDoor == null) return true;

        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor, playerFromSender);

        if (sourceDoor == null) return true;

        Door playerTargetDoor = getPlayerDoor(args[1], playerFromSender);

        if (playerTargetDoor == null) return true;

        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor, playerFromSender);

        if (targetDoor == null) return true;

        targetDoor.setConditionBag(sourceDoor.getConditionBag().copy());

        targetDoor.setStayOpen(sourceDoor.getStayOpen());

        if (sourceDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            targetDoor.setEvaluator(sourceDoor.getEvaluator());
        } else {
            targetDoor.setEvaluator(sourceDoor.getEvaluationType());
        }

        targetDoor.setInvertOpen(sourceDoor.isInvertOpen());

        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(targetDoor));

        messageSender.sendMessage(sender, localizer.getMessage("cloneDoor.message",
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return getDoorCompletion(sender, args[1]);
        }
        return Collections.emptyList();
    }
}
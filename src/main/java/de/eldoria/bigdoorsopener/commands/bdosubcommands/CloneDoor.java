/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Replacement;
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

public class CloneDoor extends BigDoorsAdapterCommand {

    public CloneDoor(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) return true;
        if (denyAccess(sender, Permissions.ALL_CONDITION)) return true;

        if (argumentsInvalid(sender, args, 2, "<$syntax.sourceDoor$> <$syntax.targetDoor$>")) {
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

        targetDoor.conditionBag(sourceDoor.conditionBag().copy());

        targetDoor.setStayOpen(sourceDoor.stayOpen());

        if (sourceDoor.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            targetDoor.setEvaluator(sourceDoor.evaluator());
        } else {
            targetDoor.setEvaluator(sourceDoor.evaluationType());
        }

        targetDoor.invertOpen(sourceDoor.isInvertOpen());

        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(targetDoor));

        messageSender().sendLocalizedMessage(sender, "cloneDoor.message",
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6'));
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

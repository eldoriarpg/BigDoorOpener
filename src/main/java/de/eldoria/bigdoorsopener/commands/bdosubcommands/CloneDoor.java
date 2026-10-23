/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.command.util.CommandAssertions;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.messages.Replacement;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Copies conditions, evaluator, stay open time and inversion from a door to another door.
 */
public class CloneDoor extends BigDoorsAdapterCommand {

    public CloneDoor(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder("cloneDoor")
                .withPermission(Permissions.USE)
                .addArgument("syntax.sourceDoor", true)
                .addArgument("syntax.targetDoor", true)
                .build(), bigDoors, config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        CommandAssertions.permission(sender, false, Permissions.ALL_CONDITION);

        Door playerSourceDoor = getPlayerDoor(args.asString(0), sender);
        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor);
        Door playerTargetDoor = getPlayerDoor(args.asString(1), sender);
        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor);

        targetDoor.conditionBag(sourceDoor.conditionBag().copy());
        targetDoor.setStayOpen(sourceDoor.stayOpen());

        if (sourceDoor.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            targetDoor.setEvaluator(sourceDoor.evaluator());
        } else {
            targetDoor.setEvaluator(sourceDoor.evaluationType());
        }

        targetDoor.invertOpen(sourceDoor.isInvertOpen());

        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(targetDoor));

        messageSender().sendMessage(sender, "cloneDoor.message",
                Replacement.unparsed("SOURCE", playerSourceDoor.getName()),
                Replacement.unparsed("TARGET", playerTargetDoor.getName()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        if (args.sizeIs(2)) {
            return getDoorCompletion(sender, args.asString(1));
        }
        return Collections.emptyList();
    }
}

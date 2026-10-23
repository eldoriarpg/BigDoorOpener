/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.command.util.CommandAssertions;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.messages.Replacement;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Copies all conditions or the conditions of one group from a door to another door.
 */
public class CopyCondition extends BigDoorsAdapterCommand {

    public CopyCondition(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder("copyCondition")
                .withPermission(Permissions.USE)
                .addArgument("syntax.sourceDoor", true)
                .addArgument("syntax.targetDoor", true)
                .addArgument("syntax.condition", false)
                .addArgument("tabcomplete.conditionId", false)
                .build(), bigDoors, config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        Door playerSourceDoor = getPlayerDoor(args.asString(0), sender);
        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor);
        Door playerTargetDoor = getPlayerDoor(args.asString(1), sender);
        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor);

        ConditionBag sourceBag = sourceDoor.conditionBag();

        if (args.sizeIs(2)) {
            CommandAssertions.permission(sender, false, Permissions.ALL_CONDITION);
            targetDoor.conditionBag(sourceBag.copy());
            messageSender().sendMessage(sender, "copyCondition.copiedAll",
                    Replacement.unparsed("SOURCE", playerSourceDoor.getName()),
                    Replacement.unparsed("TARGET", playerTargetDoor.getName()));
            return;
        }

        ConditionGroup conditionGroup = ConditionRegistrar.getConditionGroup(args.asString(2))
                .orElseThrow(() -> CommandException.message("error.invalidConditionType"));

        CommandAssertions.permission(sender, false, Permissions.getConditionPermission(conditionGroup.name()), Permissions.ALL_CONDITION);

        int index = args.asInt(3, 0);
        List<DoorCondition> condition = sourceBag.getConditions(conditionGroup);
        CommandAssertions.isTrue(index >= 0 && index < condition.size(), "error.conditionNotSet");

        targetDoor.conditionBag().setCondition(targetDoor, condition.get(index).clone());

        messageSender().sendMessage(sender, "copyCondition.copiedSingle",
                Replacement.create("CONDITION", conditionGroup.name()),
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
        if (args.sizeIs(3)) {
            return Completion.complete(args.asString(2), ConditionRegistrar.getGroups());
        }
        if (args.sizeIs(4)) {
            return Collections.singletonList(localizer().getMessage("tabcomplete.conditionId"));
        }
        return Collections.emptyList();
    }
}

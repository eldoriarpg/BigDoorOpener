/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

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
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes a condition of a door by group and index within the group.
 */
public class RemoveCondition extends BigDoorsAdapterCommand {

    public RemoveCondition(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder("removeCondition")
                .withPermission(Permissions.USE)
                .addArgument("syntax.doorId", true)
                .addArgument("syntax.condition", true)
                .addArgument("tabcomplete.conditionId", false)
                .build(), bigDoors, config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        ConditionalDoor door = getOrRegister(getPlayerDoor(args.asString(0), sender));

        ConditionGroup container = ConditionRegistrar.getConditionGroup(args.asString(1))
                .orElseThrow(() -> CommandException.message("error.invalidConditionType"));
        String group = container.name();

        CommandAssertions.permission(sender, false, Permissions.getConditionPermission(group), Permissions.ALL_CONDITION);

        ConditionBag conditionBag = door.conditionBag();
        CommandAssertions.isTrue(conditionBag.isConditionSet(container), "error.conditionNotSet");
        CommandAssertions.isTrue(conditionBag.removeCondition(door, container, args.asInt(2, 0)), "error.conditionNotSet");

        messageSender().sendMessage(sender, "removeCondition." + group);

        if (door.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM
            && Pattern.compile(group, Pattern.CASE_INSENSITIVE).matcher(door.evaluator()).find()) {
            messageSender().sendError(sender, "warning.valueStillUsed", Replacement.create("VALUE", group));
        }

        if (conditionBag.isEmpty()) {
            messageSender().sendMessage(sender, "warning.chainIsEmpty");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        if (args.sizeIs(2)) {
            return Completion.complete(args.asString(1), ConditionRegistrar.getGroups());
        }
        if (args.sizeIs(3)) {
            return Collections.singletonList(localizer().getMessage("tabcomplete.conditionId"));
        }
        return Collections.emptyList();
    }
}

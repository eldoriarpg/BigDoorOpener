/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
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
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Creates a condition from the command arguments and hands it to a door. Registers the door if it is not registered
 * yet. Warns when the door has a custom evaluator which does not use the group of the condition.
 */
public abstract class ConditionCreationCommand extends BigDoorsAdapterCommand {

    protected ConditionCreationCommand(Plugin plugin, String name, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder(name)
                .withPermission(Permissions.USE)
                .addArgument("syntax.doorId", true)
                .addArgument("syntax.condition", true)
                .addArgument("syntax.conditionValues", false)
                .build(), bigDoors, config);
    }

    /**
     * Hands the created condition to the door.
     *
     * @param door      door which receives the condition
     * @param condition created condition
     */
    protected abstract void apply(ConditionalDoor door, DoorCondition condition);

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        ConditionalDoor door = getOrRegister(getPlayerDoor(args.asString(0), sender));

        ConditionContainer condition = ConditionRegistrar.getConditionByName(args.asString(1))
                .orElseThrow(() -> CommandException.message("error.invalidConditionType"));
        String group = condition.getGroup();

        CommandAssertions.permission(sender, false, Permissions.getConditionPermission(condition), Permissions.ALL_CONDITION);

        condition.create(sender, messageSender(), args.subArguments(2), c -> apply(door, c));

        if (door.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM
            && !Pattern.compile(group, Pattern.CASE_INSENSITIVE).matcher(door.evaluator()).find()) {
            messageSender().sendError(sender, "warning.valueNotInEvaluator", Replacement.create("VALUE", group));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        if (args.sizeIs(2)) {
            return Completion.complete(args.asString(1), ConditionRegistrar.getConditions());
        }

        Optional<ConditionContainer> conditionByName = ConditionRegistrar.getConditionByName(args.asString(1));
        if (conditionByName.isEmpty()) {
            return Collections.singletonList(localizer().getMessage("error.invalidConditionType"));
        }

        ConditionContainer container = conditionByName.get();
        String permission = Permissions.getConditionPermission(container.getGroup());
        if (!sender.hasPermission(permission) && !sender.hasPermission(Permissions.ALL_CONDITION)) {
            return Collections.singletonList(messageSender().translatePlain("error.permission",
                    Replacement.create("PERMISSION", permission + ", " + Permissions.ALL_CONDITION)));
        }
        return container.onTabComplete(sender, localizer(), args.subArguments(2));
    }
}

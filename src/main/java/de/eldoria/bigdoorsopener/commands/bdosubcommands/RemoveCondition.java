/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.ArrayUtil;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RemoveCondition extends BigDoorsAdapterCommand {

    public RemoveCondition(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<$syntax.doorId$> <$syntax.condition$>")) {
            return false;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Door playerDoor = getPlayerDoor(args[0], playerFromSender);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor cDoor = getOrRegister(playerDoor, playerFromSender);

        if (cDoor == null) {
            return true;
        }

        Optional<ConditionGroup> optionalGroup = ConditionRegistrar.getConditionGroup(args[1]);

        if (!optionalGroup.isPresent()) {
            messageSender().sendLocalizedError(sender, "error.invalidConditionType");
            return true;
        }
        ConditionGroup container = optionalGroup.get();

        String group = container.name();

        if (denyAccess(sender, Permissions.getConditionPermission(group), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag conditionBag = cDoor.conditionBag();

        if (!conditionBag.isConditionSet(container)) {
            messageSender().sendLocalizedError(sender, "error.conditionNotSet");
            return true;
        } else {
            String id = ArgumentUtils.getOrDefault(args, 2, "0");
            Optional<Integer> optionalInt = Parser.parseInt(id);
            if (!optionalInt.isPresent()) {
                messageSender().sendLocalizedError(sender, "error.invalidNumber");
                return true;
            }
            if (!conditionBag.removeCondition(cDoor, container, optionalInt.get())) {
                messageSender().sendLocalizedError(sender, "error.conditionNotSet");
                return true;
            }
        }

        messageSender().sendLocalizedMessage(sender, "removeCondition." + group);

        // check if condition is in evaluator if a custom evaluator is present.
        if (cDoor.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(group, Pattern.CASE_INSENSITIVE);
            if (compile.matcher(cDoor.evaluator()).find()) {
                messageSender().sendLocalizedError(sender, "warning.valueStillUsed",
                        Replacement.create("VALUE", group).addFormatting('6'));
            }
        }

        if (conditionBag.isEmpty()) {
            messageSender().sendLocalizedMessage(sender, "warning.chainIsEmpty");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return ArrayUtil.startingWithInArray(args[1], ConditionRegistrar.getGroups().toArray(new String[0])).collect(Collectors.toList());
        }
        if (args.length == 3) {
            return Collections.singletonList(localizer().getMessage("tabcomplete.conditionId"));
        }
        return Collections.emptyList();
    }
}

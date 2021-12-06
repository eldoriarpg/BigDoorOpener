/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import de.eldoria.eldoutilities.utils.EnumUtil;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetEvaluator extends BigDoorsAdapterCommand {

    private static final String[] EVALUATOR_TYPES;

    static {
        EVALUATOR_TYPES = Arrays.stream(ConditionalDoor.EvaluationType.values())
                .map(v -> v.name().toLowerCase())
                .toArray(String[]::new);
    }

    public SetEvaluator(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<$syntax.doorId$> <$syntax.evaluationType$> [$syntax.customEvaluator$]")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);

        if (door == null) {
            return true;
        }

        ConditionalDoor.EvaluationType type = EnumUtil.parse(args[1], ConditionalDoor.EvaluationType.class).orElse(null);
        if (type == null) {
            messageSender().sendLocalizedError(sender, "error.invalidEvaluationType");
            return true;
        }

        if (type != ConditionalDoor.EvaluationType.CUSTOM) {
            door.first.setEvaluator(type);
            if (type == ConditionalDoor.EvaluationType.AND) {
                messageSender().sendLocalizedMessage(sender, "setEvaluator.and");
            } else {
                messageSender().sendLocalizedMessage(sender, "setEvaluator.or");
            }
            return true;
        }

        if (denyAccess(sender, Permissions.CUSTOM_EVALUATOR)) {
            return true;
        }

        if (args.length < 3) {
            messageSender().sendLocalizedError(sender, "error.noEvaluatorFound");
            return true;
        }
        String evaluator = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.validateEvaluator(evaluator, BigDoorsOpener.JS());

        switch (result.first) {
            case UNBALANCED_PARENTHESIS:
                messageSender().sendLocalizedError(sender, "error.unbalancedParenthesis");
                return true;
            case INVALID_VARIABLE:
                messageSender().sendLocalizedError(sender, "error.invalidVariable",
                        Replacement.create("ERROR", result.second).addFormatting('6'));
                return true;
            case INVALID_OPERATOR:
                messageSender().sendLocalizedError(sender, "error.invalidOperator",
                        Replacement.create("ERROR", result.second).addFormatting('6'));
                return true;
            case INVALID_SYNTAX:
                messageSender().sendLocalizedError(sender, "error.invalidSyntax",
                        Replacement.create("ERROR", result.second).addFormatting('6'));
                return true;
            case EXECUTION_FAILED:
                messageSender().sendLocalizedError(sender, "error.executionFailed",
                        Replacement.create("ERROR", result.second).addFormatting('6'));
                return true;
            case NON_BOOLEAN_RESULT:
                messageSender().sendLocalizedError(sender, "error.nonBooleanResult",
                        Replacement.create("ERROR", result.second).addFormatting('6'));
                return true;
            case FINE:
                door.first.setEvaluator(JsSyntaxHelper.translateEvaluator(evaluator));
                break;
        }
        messageSender().sendLocalizedMessage(sender, "setEvaluator.custom",
                Replacement.create("EVALUATOR", door.first.evaluator()).addFormatting('6'));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return ArrayUtil.startingWithInArray(args[1], EVALUATOR_TYPES).collect(Collectors.toList());
        }

        ConditionalDoor.EvaluationType parse = EnumUtil.parse(args[1], ConditionalDoor.EvaluationType.class).orElse(null);

        if (parse == null) {
            return Collections.singletonList(localizer().getMessage("error.invalidEvaluationType"));
        }

        if (parse == ConditionalDoor.EvaluationType.CUSTOM) {
            if (denyAccess(sender, true, Permissions.CUSTOM_EVALUATOR)) {
                return Collections.singletonList(localizer().getMessage("error.permission",
                        Replacement.create("PERMISSION", Permissions.CUSTOM_EVALUATOR)));
            }
            ArrayList<String> list = new ArrayList<>(ConditionRegistrar.getGroups());
            list.add("currentState");
            list.add("<" + localizer().getMessage("tabcomplete.validValues") + ">");
            return list;
        }
        return Collections.emptyList();
    }
}

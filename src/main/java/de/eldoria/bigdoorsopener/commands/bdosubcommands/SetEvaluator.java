/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.command.util.CommandAssertions;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.messages.Replacement;
import de.eldoria.eldoutilities.utils.EnumUtil;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sets how the conditions of a door are combined: all, any or a custom JavaScript expression.
 */
public class SetEvaluator extends BigDoorsAdapterCommand {
    private static final String[] EVALUATOR_TYPES = Arrays.stream(ConditionalDoor.EvaluationType.values())
            .map(v -> v.name().toLowerCase())
            .toArray(String[]::new);

    public SetEvaluator(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder("setEvaluator")
                .withPermission(Permissions.USE)
                .addArgument("syntax.doorId", true)
                .addArgument("syntax.evaluationType", true)
                .addArgument("syntax.customEvaluator", false)
                .build(), bigDoors, config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args.asString(0), sender);

        ConditionalDoor.EvaluationType type = EnumUtil.parse(args.asString(1), ConditionalDoor.EvaluationType.class)
                .orElseThrow(() -> CommandException.message("error.invalidEvaluationType"));

        if (type != ConditionalDoor.EvaluationType.CUSTOM) {
            door.first.setEvaluator(type);
            messageSender().sendMessage(sender, type == ConditionalDoor.EvaluationType.AND ? "setEvaluator.and" : "setEvaluator.or");
            return;
        }

        CommandAssertions.permission(sender, false, Permissions.CUSTOM_EVALUATOR);

        String[] raw = args.raw();
        CommandAssertions.isTrue(raw.length > 2, "error.noEvaluatorFound");
        String evaluator = String.join(" ", Arrays.copyOfRange(raw, 2, raw.length));
        Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.validateEvaluator(evaluator, BigDoorsOpener.JS());

        switch (result.first) {
            case UNBALANCED_PARENTHESIS -> throw CommandException.message("error.unbalancedParenthesis");
            case INVALID_VARIABLE -> throw CommandException.message("error.invalidVariable", Replacement.unparsed("ERROR", result.second));
            case INVALID_OPERATOR -> throw CommandException.message("error.invalidOperator", Replacement.unparsed("ERROR", result.second));
            case INVALID_SYNTAX -> throw CommandException.message("error.invalidSyntax", Replacement.unparsed("ERROR", result.second));
            case EXECUTION_FAILED -> throw CommandException.message("error.executionFailed", Replacement.unparsed("ERROR", result.second));
            case NON_BOOLEAN_RESULT -> throw CommandException.message("error.nonBooleanResult", Replacement.unparsed("ERROR", result.second));
            case FINE -> door.first.setEvaluator(JsSyntaxHelper.translateEvaluator(evaluator));
        }
        messageSender().sendMessage(sender, "setEvaluator.custom", Replacement.unparsed("EVALUATOR", door.first.evaluator()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        if (args.sizeIs(2)) {
            return Completion.complete(args.asString(1), EVALUATOR_TYPES);
        }

        ConditionalDoor.EvaluationType parse = EnumUtil.parse(args.asString(1), ConditionalDoor.EvaluationType.class).orElse(null);

        if (parse == null) {
            return Collections.singletonList(localizer().getMessage("error.invalidEvaluationType"));
        }

        if (parse == ConditionalDoor.EvaluationType.CUSTOM) {
            if (!sender.hasPermission(Permissions.CUSTOM_EVALUATOR)) {
                return Collections.singletonList(messageSender().translatePlain("error.permission",
                        Replacement.create("PERMISSION", Permissions.CUSTOM_EVALUATOR)));
            }
            List<String> list = new ArrayList<>(ConditionRegistrar.getGroups());
            list.add("currentState");
            list.add("<" + localizer().getMessage("tabcomplete.validValues") + ">");
            return list;
        }
        return Collections.emptyList();
    }
}

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
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

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class SetEvaluator extends BigDoorsAdapterCommand {

    private final Localizer localizer;
    private final MessageSender messageSender;

    private static final String[] EVALUATOR_TYPES;


    public SetEvaluator(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    static {
        EVALUATOR_TYPES = Arrays.stream(ConditionalDoor.EvaluationType.values())
                .map(v -> v.name().toLowerCase())
                .toArray(String[]::new);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.evaluationType") + "> ["
                        + localizer.getMessage("syntax.customEvaluator") + "]")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);

        if (door == null) {
            return true;
        }

        ConditionalDoor.EvaluationType type = EnumUtil.parse(args[1], ConditionalDoor.EvaluationType.class, false);
        if (type == null) {
            messageSender.sendMessage(sender, localizer.getMessage("error.invalidEvaluationType"));
            return true;
        }

        if (type != ConditionalDoor.EvaluationType.CUSTOM) {
            door.first.setEvaluator(type);
            if (type == ConditionalDoor.EvaluationType.AND) {
                messageSender.sendMessage(sender, localizer.getMessage("setEvaluator.and"));
            } else {
                messageSender.sendMessage(sender, localizer.getMessage("setEvaluator.or"));
            }
            return true;
        }

        if (denyAccess(sender, Permissions.CUSTOM_EVALUATOR)) {
            return true;
        }

        if (args.length < 3) {
            messageSender.sendError(sender, localizer.getMessage("error.noEvaluatorFound"));
            return true;
        }
        String evaluator = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.validateEvaluator(evaluator, BigDoorsOpener.JS());

        switch (result.first) {
            case UNBALANCED_PARENTHESIS:
                messageSender.sendError(sender, localizer.getMessage("error.unbalancedParenthesis"));
                return true;
            case INVALID_VARIABLE:
                messageSender.sendError(sender, localizer.getMessage("error.invalidVariable",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case INVALID_OPERATOR:
                messageSender.sendError(sender, localizer.getMessage("error.invalidOperator",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case INVALID_SYNTAX:
                messageSender.sendError(sender, localizer.getMessage("error.invalidSyntax",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case EXECUTION_FAILED:
                messageSender.sendError(sender, localizer.getMessage("error.executionFailed",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case NON_BOOLEAN_RESULT:
                messageSender.sendError(sender, localizer.getMessage("error.nonBooleanResult",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case FINE:
                door.first.setEvaluator(JsSyntaxHelper.translateEvaluator(evaluator));
                break;
        }
        messageSender.sendMessage(sender, localizer.getMessage("setEvaluator.custom",
                Replacement.create("EVALUATOR", door.first.getEvaluator()).addFormatting('6')));
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

        ConditionalDoor.EvaluationType parse = EnumUtil.parse(args[1], ConditionalDoor.EvaluationType.class);

        if (parse == null) {
            return Collections.singletonList(localizer.getMessage("error.invalidEvaluationType"));
        }

        if (parse == ConditionalDoor.EvaluationType.CUSTOM) {
            if (denyAccess(sender, true, Permissions.CUSTOM_EVALUATOR)) {
                return Collections.singletonList(localizer.getMessage("error.permission",
                        Replacement.create("PERMISSION", Permissions.CUSTOM_EVALUATOR)));
            }
            ArrayList<String> list = new ArrayList<>(ConditionRegistrar.getGroups());
            list.add("currentState");
            list.add("<" + localizer.getMessage("tabcomplete.validValues") + ">");
            return list;
        }
        return Collections.emptyList();
    }
}

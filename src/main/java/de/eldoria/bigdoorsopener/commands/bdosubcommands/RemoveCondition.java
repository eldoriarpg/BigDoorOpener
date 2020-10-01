package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArrayUtil;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class RemoveCondition extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final MessageSender messageSender;

    public RemoveCondition(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.condition") + ">")) {
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
            messageSender.sendError(sender, localizer.getMessage("error.invalidConditionType"));
            return true;
        }
        ConditionGroup container = optionalGroup.get();

        String group = container.getName();

        if (denyAccess(sender, Permissions.getConditionPermission(group), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag conditionBag = cDoor.getConditionBag();

        if (!conditionBag.isConditionSet(container)) {
            messageSender.sendError(sender, localizer.getMessage("error.conditionNotSet"));
            return true;
        } else {
            conditionBag.removeCondition(container);
        }

        messageSender.sendMessage(sender, localizer.getMessage("removeCondition." + group));

        // check if condition is in evaluator if a custom evaluator is present.
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(group, Pattern.CASE_INSENSITIVE);
            if (compile.matcher(cDoor.getEvaluator()).find()) {
                messageSender.sendError(sender, localizer.getMessage("warning.valueStillUsed",
                        Replacement.create("VALUE", group).addFormatting('6')));
            }
        }

        if (conditionBag.isEmpty()) {
            messageSender.sendMessage(sender, localizer.getMessage("warning.chainIsEmpty"));
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
        return Collections.emptyList();
    }
}

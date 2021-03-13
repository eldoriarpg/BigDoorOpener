package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class SetCondition extends BigDoorsAdapterCommand {

    public SetCondition(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<$syntax.doorId$> <$syntax.condition$> [$syntax.conditionValues$]")) {
            return true;
        }

        Player player = getPlayerFromSender(sender);

        Door playerDoor = getPlayerDoor(args[0], player);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor conditionalDoor = getOrRegister(playerDoor, player);

        if (conditionalDoor == null) {
            return true;
        }

        Optional<ConditionContainer> conditionByName = ConditionRegistrar.getConditionByName(args[1]);

        if (!conditionByName.isPresent()) {
            messageSender().sendLocalizedError(sender, "error.invalidConditionType");
            return true;
        }

        ConditionContainer condition = conditionByName.get();

        String group = condition.getGroup();

        if (denyAccess(player, Permissions.getConditionPermission(condition),
                Permissions.ALL_CONDITION)) {
            return true;
        }

        String[] conditionArgs = new String[0];
        if (args.length > 2) {
            conditionArgs = Arrays.copyOfRange(args, 2, args.length);
        }

        condition.create(player, messageSender(), c -> conditionalDoor.getConditionBag().setCondition(c), conditionArgs);

        if (conditionalDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(group, Pattern.CASE_INSENSITIVE);
            if (!compile.matcher(conditionalDoor.getEvaluator()).find()) {
                messageSender().sendLocalizedError(player, "warning.valueNotInEvaluator",
                        Replacement.create("VALUE", group).addFormatting('6'));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return ArrayUtil.startingWithInArray(args[1], ConditionRegistrar.getConditions().toArray(new String[0])).collect(Collectors.toList());
        }

        Optional<ConditionContainer> conditionByName = ConditionRegistrar.getConditionByName(args[1]);

        if (!conditionByName.isPresent()) {
            return Collections.singletonList(localizer().getMessage("error.invalidConditionType"));
        }

        ConditionContainer container = conditionByName.get();

        if (denyAccess(sender, true, Permissions.getConditionPermission(container.getGroup()), Permissions.ALL_CONDITION)) {
            return Collections.singletonList(localizer().getMessage("error.permission",
                    Replacement.create("PERMISSION", Permissions.getConditionPermission(container.getGroup()) + ", " + Permissions.ALL_CONDITION)));
        }
        return container.onTabComplete(sender, localizer(), Arrays.copyOfRange(args, 2, args.length));
    }
}

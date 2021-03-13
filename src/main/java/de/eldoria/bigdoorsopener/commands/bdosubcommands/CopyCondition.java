package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
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
import java.util.stream.Collectors;

public class CopyCondition extends BigDoorsAdapterCommand {

    public CopyCondition(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 2,
                "<$syntax.sourceDoor$> <$syntax.targetDoor$> [$syntax.condition$]")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Door playerSourceDoor = getPlayerDoor(args[0], playerFromSender);

        if (playerSourceDoor == null) {
            return true;
        }

        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor, playerFromSender);

        if (sourceDoor == null) {
            return true;
        }

        Door playerTargetDoor = getPlayerDoor(args[1], playerFromSender);

        if (playerTargetDoor == null) {
            return true;
        }

        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor, playerFromSender);

        if (targetDoor == null) {
            return true;
        }

        ConditionBag sourceBag = sourceDoor.getConditionBag();

        if (args.length == 2) {
            if (denyAccess(sender, Permissions.ALL_CONDITION)) {
                return true;
            }

            targetDoor.setConditionBag(sourceBag.copy());
            messageSender().sendLocalizedMessage(sender, "copyCondition.copiedAll",
                    Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                    Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6'));
            return true;
        }

        Optional<ConditionGroup> optionalGroup = ConditionRegistrar.getConditionGroup(args[2]);

        if (!optionalGroup.isPresent()) {
            messageSender().sendLocalizedError(sender, "error.invalidConditionType");
            return true;
        }

        ConditionGroup conditionGroup = optionalGroup.get();

        if (denyAccess(sender, Permissions.getConditionPermission(conditionGroup.getName()), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag targetBag = targetDoor.getConditionBag();


        String id = ArgumentUtils.getOrDefault(args, 2, "0");
        OptionalInt optionalInt = Parser.parseInt(id);
        if (!optionalInt.isPresent()) {
            messageSender().sendLocalizedError(sender, "error.invalidNumber");
            return true;
        }

        List<DoorCondition> condition = sourceBag.getConditions(conditionGroup);

        if (condition.isEmpty()) {
            messageSender().sendLocalizedError(sender, "error.conditionNotSet");
            return true;
        }

		if (condition.size() < optionalInt.getAsInt()) {
			messageSender().sendLocalizedError(sender, "error.conditionNotSet");
			return true;
		}

		targetBag.setCondition(condition.get(optionalInt.getAsInt()).clone());

        messageSender().sendLocalizedMessage(sender, "copyCondition.copiedSingle",
                Replacement.create("CONDITION", conditionGroup.getName()).addFormatting('6'),
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6'));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }
        if (args.length == 2) {
            return getDoorCompletion(sender, args[1]);
        }
        if (args.length == 3) {
            return ArrayUtil.startingWithInArray(args[2], ConditionRegistrar.getGroups().toArray(new String[0])).collect(Collectors.toList());
        }
        if (args.length == 4) {
            return Collections.singletonList(localizer().getMessage("tabcomplete.conditionId"));
        }
        return Collections.emptyList();
    }
}

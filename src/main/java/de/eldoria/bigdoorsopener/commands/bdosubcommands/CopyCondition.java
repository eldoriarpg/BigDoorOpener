package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
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
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class CopyCondition extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final MessageSender messageSender;

    public CopyCondition(BigDoors bigDoors, Config config) {
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
                "<" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + "> ["
                        + localizer.getMessage("syntax.condition") + "]")) {
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
            messageSender.sendMessage(sender, localizer.getMessage("copyCondition.copiedAll",
                    Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                    Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
            return true;
        }

        Optional<ConditionGroup> optionalGroup = ConditionRegistrar.getConditionGroup(args[2]);

        if (!optionalGroup.isPresent()) {
            messageSender.sendError(sender, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        ConditionGroup conditionGroup = optionalGroup.get();

        if (denyAccess(sender, Permissions.getConditionPermission(conditionGroup.getName()), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag targetBag = targetDoor.getConditionBag();

        Optional<DoorCondition> condition = sourceBag.getCondition(conditionGroup);

        if (!condition.isPresent()) {
            messageSender.sendError(sender, localizer.getMessage("error.conditionNotSet"));
            return true;
        }

        targetBag.putCondition(condition.get().clone());

        messageSender.sendMessage(sender, localizer.getMessage("copyCondition.copiedSingle",
                Replacement.create("CONDITION", conditionGroup.getName()).addFormatting('6'),
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
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
        return Collections.emptyList();
    }
}

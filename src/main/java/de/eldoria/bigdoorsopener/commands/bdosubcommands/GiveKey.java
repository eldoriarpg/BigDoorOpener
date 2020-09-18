package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.Parser;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class GiveKey extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final MessageSender messageSender;

    public GiveKey(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        if (playerFromSender == null) {
            if (argumentsInvalid(null, args, 3,
                    "<" + localizer.getMessage("syntax.doorId") + "> <"
                            + localizer.getMessage("syntax.amount") + "> <"
                            + localizer.getMessage("syntax.player") + ">")) {
                return true;
            }
        } else {
            if (argumentsInvalid(sender, args, 1,
                    "<" + localizer.getMessage("syntax.doorId") + "> ["
                            + localizer.getMessage("syntax.amount") + "] ["
                            + localizer.getMessage("syntax.player") + "]")) {
                return true;
            }
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);

        if (door == null) {
            return true;
        }

        Optional<DoorCondition> condition = door.first.getConditionBag().getCondition("item");

        if (!condition.isPresent()) {
            messageSender.sendError(sender, localizer.getMessage("error.noItemConditionSet"));
            return true;
        }

        ItemStack item = ((Item) condition.get()).getItem();

        OptionalInt amount = ArgumentUtils.getOptionalParameter(args, 1, OptionalInt.of(64), Parser::parseInt);

        if (!amount.isPresent()) {
            messageSender.sendError(sender, localizer.getMessage("error.invalidAmount"));
            return true;
        }

        Player target = ArgumentUtils.getOptionalParameter(args, 2, playerFromSender, Bukkit::getPlayer);

        if (target == null) {
            messageSender.sendError(sender, localizer.getMessage("error.playerNotFound"));
            return true;
        }

        ItemStack clone = item.clone();
        clone.setAmount(amount.getAsInt());
        target.getInventory().addItem(clone);
        if (target != playerFromSender) {
            messageSender.sendMessage(playerFromSender, localizer.getMessage("giveKey.send",
                    Replacement.create("AMOUNT", amount.getAsInt()),
                    Replacement.create("ITEMNAME", item.hasItemMeta() ? (item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : item.getType().name().toLowerCase())
                            : item.getType().name().toLowerCase()),
                    Replacement.create("TARGET", target.getDisplayName())));
        }
        messageSender.sendMessage(target, localizer.getMessage("giveKey.received",
                Replacement.create("AMOUNT", amount.getAsInt()),
                Replacement.create("ITEMNAME", item.hasItemMeta() ? (item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name().toLowerCase())
                        : item.getType().name().toLowerCase())));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getDoorCompletion(sender, args[0]);
        }

        if (args.length == 2) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
        }

        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().toLowerCase().startsWith(args[2]))
                    .map(HumanEntity::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
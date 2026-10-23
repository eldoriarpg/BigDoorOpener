/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.Item;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.command.util.CommandAssertions;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.messages.Replacement;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Gives the item of an item condition of a door to a player.
 */
public class GiveKey extends BigDoorsAdapterCommand {

    public GiveKey(Plugin plugin, BigDoors bigDoors, Config config) {
        super(
                plugin,
                CommandMeta.builder("giveKey")
                        .withPermission(Permissions.GIVE_KEY)
                        .addArgument("syntax.doorId", true)
                        .addArgument("syntax.amount", false)
                        .addArgument("syntax.player", false)
                        .addArgument("tabcomplete.conditionId", false)
                        .build(),
                bigDoors,
                config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args)
            throws CommandException {
        Player playerFromSender = getPlayerFromSender(sender);
        if (playerFromSender == null) {
            args.assertLength(3);
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args.asString(0), sender);

        List<DoorCondition> condition = door.first.conditionBag().getConditions("item");
        CommandAssertions.isFalse(condition.isEmpty(), "error.noItemConditionSet");

        int index = args.asInt(3, 0);
        CommandAssertions.isTrue(index < condition.size(), "error.conditionNotSet");

        ItemStack item = ((Item) condition.get(index)).item();
        int amount = args.asInt(1, 64);
        Player target = args.asPlayer(2, playerFromSender);

        ItemStack clone = item.clone();
        clone.setAmount(amount);
        target.getInventory().addItem(clone);
        String itemName = PlainTextComponentSerializer.plainText().serialize(item.effectiveName());
        if (target != sender) {
            messageSender()
                    .sendMessage(
                            sender,
                            "giveKey.send",
                            Replacement.create("AMOUNT", amount),
                            Replacement.unparsed("ITEMNAME", itemName),
                            Replacement.unparsed("TARGET", target.getName()));
        }
        messageSender()
                .sendMessage(
                        target,
                        "giveKey.received",
                        Replacement.create("AMOUNT", amount),
                        Replacement.unparsed("ITEMNAME", itemName));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        if (args.sizeIs(2)) {
            return Collections.singletonList("<" + localizer().getMessage("syntax.amount") + ">");
        }
        if (args.sizeIs(3)) {
            return Completion.completeOnlinePlayers(args.asString(2));
        }
        if (args.sizeIs(4)) {
            return Collections.singletonList(localizer().getMessage("tabcomplete.conditionId"));
        }
        return Collections.emptyList();
    }
}

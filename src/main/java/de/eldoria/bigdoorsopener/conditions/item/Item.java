/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.item;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base of the conditions which require an item, the key. The amount of the key matters.
 */
public abstract class Item implements DoorCondition {
    /**
     * Syntax of the arguments shared by all item conditions.
     */
    public static final String SYNTAX = "<$syntax.amount$> [$tabcomplete.consumed$]";
    private static final int MAX_AMOUNT = 64;
    private final ItemStack item;
    private final boolean consumed;

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public Item(ItemStack item, boolean consumed) {
        this.item = Objects.requireNonNullElseGet(item, () -> new ItemStack(Material.AIR));
        this.consumed = consumed;
    }

    public static List<String> onTabComplete(CommandSender sender, ILocalizer localizer, Arguments args) {
        if (args.sizeIs(1)) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
        }
        if (args.sizeIs(2)) {
            if (args.asString(1).isEmpty()) {
                return Arrays.asList("true", "false");
            }
            return Completion.completeBoolean(args.asString(1));
        }
        return Collections.emptyList();
    }

    /**
     * Takes the key of a new item condition from the main hand of the player, with the amount from the arguments.
     *
     * @param sender sender of the command
     * @param args   arguments of the condition: amount and the optional consumed flag
     * @return the key
     * @throws CommandException when the sender is no player or the amount is invalid
     */
    protected static ItemStack keyFromHand(CommandSender sender, Arguments args) throws CommandException {
        Player player = ConditionInput.player(sender);
        ConditionInput.require(args, 1, DoorCondition.localizer(), SYNTAX);
        int amount = args.asInt(0);
        if (amount > MAX_AMOUNT || amount < 1) {
            throw CommandException.message("error.invalidRange", Replacement.create("MIN", 1), Replacement.create("MAX", MAX_AMOUNT));
        }
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand().clone();
        itemInMainHand.setAmount(amount);
        return itemInMainHand;
    }

    /**
     * Reads whether the key of a new item condition is consumed.
     *
     * @param args arguments of the condition
     * @return the consumed flag, false when not given
     * @throws CommandException when the flag is no boolean
     */
    protected static boolean consumed(Arguments args) throws CommandException {
        return args.asBoolean(1, false);
    }

    /**
     * This method will be called when a door with this key was opened. Only once.
     *
     * @param player player which opened the door.
     */
    @Override
    public abstract void opened(Player player);

    /**
     * Checks if a player has a item in the off or main hand.
     *
     * @param player player to check
     * @return true if the player has the item in one of his hands.
     */
    protected boolean hasPlayerItemInHand(Player player) {
        return hasPlayerItemInMainHand(player) || hasPlayerItemInOffHand(player);
    }

    /**
     * Checks if a player has a item in the main hand.
     *
     * @param player player to check
     * @return true if the player has the item in his main hand.
     */
    private boolean hasPlayerItemInMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() < this.item.getAmount()) {
            return false;
        }
        return item.isSimilar(item());
    }

    /**
     * Checks if a player has a item in the off hand.
     *
     * @param player player to check
     * @return true if the player has the item in his main hands.
     */
    private boolean hasPlayerItemInOffHand(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();
        if (item.getAmount() < item().getAmount()) {
            return false;
        }
        return item.isSimilar(item());
    }

    /**
     * Checks if a player has a item in his inventory.
     *
     * @param player player to check
     * @return true if the player has the item in his inventory.
     */
    protected boolean hasPlayerItemInInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.containsAtLeast(item(), item().getAmount());
    }

    /**
     * Takes the item from the off hand.
     *
     * @param player player to take items from
     */
    private void takeFromOffHand(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();
        item.setAmount(item.getAmount() - item().getAmount());
        player.getInventory().setItemInOffHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the main hand.
     *
     * @param player player to take items from
     */
    private void takeFromMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - item().getAmount());
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the inventory.
     *
     * @param player player to take items from
     */
    protected void takeFromInventory(Player player) {
        player.getInventory().removeItem(item());
        player.updateInventory();
    }

    /**
     * Takes the item from the main or off hand.
     *
     * @param player player to take items from
     * @return true if the item was taken
     */
    protected boolean tryTakeFromHands(Player player) {
        if (hasPlayerItemInMainHand(player)) {
            takeFromMainHand(player);
            return true;
        } else if (hasPlayerItemInOffHand(player)) {
            takeFromOffHand(player);
            return true;
        }
        return false;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        ItemMeta meta = item.getItemMeta();
        TextComponent.Builder builder = Component.text();
        if (meta != null) {
            builder.append(item.effectiveName().colorIfAbsent(NamedTextColor.AQUA));
            List<Component> lore = meta.lore();
            if (lore != null) {
                for (Component line : lore) {
                    builder.append(Component.newline())
                            .append(line.colorIfAbsent(NamedTextColor.LIGHT_PURPLE));
                }
            }
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                builder.append(Component.newline())
                        .append(Component.text(entry.getKey().getKey().getKey() + " "
                                               + entry.getValue().toString(), NamedTextColor.GRAY));
            }
        }

        return Component.text()
                .append(Component.text(localizer.getMessage("conditionDesc.item") + " ", C.baseColor))
                .append(Component.text("[" + item.getType().name().toLowerCase() + "] x" + item.getAmount(), C.highlightColor)
                        .hoverEvent(HoverEvent.showText(builder.build())))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.consumed") + " ", C.baseColor))
                .append(Component.text(Boolean.toString(isConsumed()), C.highlightColor))
                .build();
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " item";
    }

    @Override
    public abstract Item clone();

    public ItemStack item() {
        return item;
    }

    public boolean isConsumed() {
        return consumed;
    }
}

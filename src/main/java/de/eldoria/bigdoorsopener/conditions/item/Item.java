/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.item;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.crossversion.ServerVersion;
import de.eldoria.eldoutilities.crossversion.builder.VersionFunctionBuilder;
import de.eldoria.eldoutilities.crossversion.function.VersionFunction;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.utils.ObjUtil;
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
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class Item implements DoorCondition {
    private final ItemStack item;
    private final boolean consumed;

    private final VersionFunction<Player, Boolean> handCheck = VersionFunctionBuilder.functionBuilder(Player.class, Boolean.class)
            .addVersionFunctionBetween(
                    ServerVersion.MC_1_9, ServerVersion.MC_1_20,
                    p -> hasPlayerItemInMainHand(p) || hasPlayerItemInOffHand(p))
            .addVersionFunction((p) -> {
                ItemStack item = p.getItemInHand();
                if (item.getAmount() < item().getAmount()) {
                    return false;
                }
                return item.isSimilar(item());
            }, ServerVersion.MC_1_8).build();

    private final VersionFunction<Player, Boolean> takeFromHand = VersionFunctionBuilder.functionBuilder(Player.class, Boolean.class)
            .addVersionFunctionBetween(
                    ServerVersion.MC_1_9, ServerVersion.MC_1_20,
                    (p) -> {
                        if (hasPlayerItemInMainHand(p)) {
                            takeFromMainHand(p);
                            return true;
                        } else if (hasPlayerItemInOffHand(p)) {
                            takeFromOffHand(p);
                            return true;
                        }
                        return false;
                    }).addVersionFunction(
                    p -> {
                        if (handCheck.apply(p)) {
                            ItemStack item = p.getItemInHand();
                            item.setAmount(item.getAmount() - item().getAmount());
                            p.setItemInHand(item);
                            p.updateInventory();
                            return true;
                        }
                        return false;
                    }, ServerVersion.MC_1_8).build();

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public Item(ItemStack item, boolean consumed) {
        this.item = ObjUtil.nonNull(item, new ItemStack(Material.AIR));
        this.consumed = consumed;
    }

    public static List<String> onTabComplete(CommandSender sender, ILocalizer localizer, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
        }
        if (args.length == 2) {
            if (args[1].isEmpty()) {
                return Arrays.asList("true", "false");
            }
            return Arrays.asList("[" + localizer.getMessage("tabcomplete.consumed") + "]", "true", "false");
        }
        return Collections.emptyList();
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
        return handCheck.apply(player);
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
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - item().getAmount());
        player.getInventory().setItemInMainHand(item);
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
     */
    protected boolean tryTakeFromHands(Player player) {
        return takeFromHand.apply(player);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("item", item)
                .add("consumed", consumed)
                .build();
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        ItemMeta meta = item.getItemMeta();
        TextComponent.Builder builder = Component.text();
        if (meta != null) {
            builder.append(Component.text(meta.getDisplayName(), NamedTextColor.AQUA));
            if (meta.getLore() != null) {
                for (String s : meta.getLore()) {
                    builder.append(Component.newline())
                            .append(Component.text(s, NamedTextColor.LIGHT_PURPLE));
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

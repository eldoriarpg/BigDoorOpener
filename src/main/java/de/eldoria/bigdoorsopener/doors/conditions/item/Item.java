package de.eldoria.bigdoorsopener.doors.conditions.item;

import com.google.gson.Gson;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import lombok.Getter;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public abstract class Item implements DoorCondition {
    private final ItemStack item;
    private final boolean consumed;
    private static final Gson GSON;

    static {
        GSON = new Gson();
    }

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public Item(ItemStack item, boolean consumed) {
        this.item = item;
        this.consumed = consumed;
    }

    /**
     * This method will be called when a door with this key was opened. Only once.
     *
     * @param player player which opened the door.
     */
    public abstract void used(Player player);

    /**
     * Checks if a player has a item in the off or main hand.
     *
     * @param player player to check
     * @param stack  item stack to check
     * @return true if the player has the item in one of his hands.
     */
    protected boolean hasPlayerItemInHand(Player player, ItemStack stack) {
        return hasPlayerItemInMainHand(player, stack) || hasPlayerItemInOffHand(player, stack);
    }

    /**
     * Checks if a player has a item in the main hand.
     *
     * @param player player to check
     * @param stack  item stack to check
     * @return true if the player has the item in his main hand.
     */
    protected boolean hasPlayerItemInMainHand(Player player, ItemStack stack) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() < stack.getAmount()) {
            return false;
        }
        return item.isSimilar(stack);
    }

    /**
     * Checks if a player has a item in the off hand.
     *
     * @param player player to check
     * @param stack  item stack to check
     * @return true if the player has the item in his main hands.
     */
    protected boolean hasPlayerItemInOffHand(Player player, ItemStack stack) {
        ItemStack item = player.getInventory().getItemInOffHand();
        if (item.getAmount() < stack.getAmount()) {
            return false;
        }
        return item.isSimilar(stack);
    }

    /**
     * Checks if a player has a item in his inventory.
     *
     * @param player player to check
     * @param stack  item stack to check
     * @return true if the player has the item in his inventory.
     */
    protected boolean hasPlayerItemInInventory(Player player, ItemStack stack) {
        PlayerInventory inventory = player.getInventory();
        return inventory.containsAtLeast(stack, stack.getAmount());
    }

    /**
     * Takes the item from the off hand.
     *
     * @param player    player to take items from
     * @param itemStack item stack to remove
     */
    protected void takeFromOffHand(Player player, ItemStack itemStack) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - itemStack.getAmount());
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the main hand.
     *
     * @param player    player to take items from
     * @param itemStack item stack to remove
     */
    protected void takeFromMainHand(Player player, ItemStack itemStack) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - itemStack.getAmount());
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the inventory.
     *
     * @param player    player to take items from
     * @param itemStack item stack to remove
     */
    protected void takeFromInventory(Player player, ItemStack itemStack) {
        player.getInventory().removeItem(itemStack);
        player.updateInventory();
    }

    /**
     * Takes the item from the main or off hand.
     *
     * @param player player to take items from
     */
    protected boolean tryTakeFromHands(Player player) {
        if (hasPlayerItemInMainHand(player, getItem())) {
            takeFromMainHand(player, getItem());
            return true;
        } else if (hasPlayerItemInOffHand(player, getItem())) {
            takeFromOffHand(player, getItem());
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("item", item)
                .add("consumed", consumed)
                .build();
    }

    /**
     * This method is called after the check for the door of this key is done and a new evaluation cycle starts.
     * Deletes any internal data in this key.
     */
    public void clear() {
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder()
                .content(localizer.getMessage("conditionDesc.item"))
                .append(TextComponent.builder("[" + item.getType().name().toLowerCase() + "] x" + item.getAmount())
                        .hoverEvent(HoverEvent.showItem(TextComponent.of(GSON.toJson(item)))).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.consumed")).color(C.highlightColor))
                .append(TextComponent.builder(Boolean.toString(isConsumed()))).build();
    }
}

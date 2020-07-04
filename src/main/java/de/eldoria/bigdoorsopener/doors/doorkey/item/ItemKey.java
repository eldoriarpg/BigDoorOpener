package de.eldoria.bigdoorsopener.doors.doorkey.item;

import de.eldoria.bigdoorsopener.doors.doorkey.DoorKey;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@Getter
@KeyParameter("itemKey")
public abstract class ItemKey implements DoorKey {
    private ItemStack item;
    private boolean consumed;

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public ItemKey(ItemStack item, boolean consumed) {
        this.item = item;
        this.consumed = consumed;
    }

    /**
     * This method will be called when a door with this key was opened. Only once.
     *
     * @param player player which opened the door.
     */
    public abstract void consume(Player player);

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
}

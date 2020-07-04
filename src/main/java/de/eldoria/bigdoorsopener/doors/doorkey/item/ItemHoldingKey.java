package de.eldoria.bigdoorsopener.doors.doorkey.item;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which will be open when the player is holding a key in his hand.
 */
@KeyParameter("itemKey")
public class ItemHoldingKey extends ItemKey {
    public ItemHoldingKey(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void consume(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInHand(player, getItem());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

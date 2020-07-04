package de.eldoria.bigdoorsopener.doors.doorkey.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which open the door, when right clicked.
 */
@KeyParameter("itemKey")
public class ItemClickKey extends InteractingKey {
    public ItemClickKey(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player, getItem()) || hasPlayerItemInOffHand(player, getItem())) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void consume(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

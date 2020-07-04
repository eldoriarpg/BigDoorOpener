package de.eldoria.bigdoorsopener.doors.doorkey.item;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which opens a doow, when the player has it in his inventory.
 */
@KeyParameter("itemKey")
public class ItemOwningKey extends ItemKey {
    public ItemOwningKey(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void used(Player player) {
        if(!isConsumed()) return;
        takeFromInventory(player, getItem());
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInInventory(player, getItem());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

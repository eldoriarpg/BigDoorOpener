package de.eldoria.bigdoorsopener.doors.doorkey.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which opens a door, when the player is clicking at a specific block
 */
@KeyParameter("itemKey")
public class ItemKeyholeKey extends InteractingKey {

    private BlockVector position;

    public ItemKeyholeKey(ItemStack item, boolean consumed, Vector position) {
        super(item, consumed);
        this.position = position.toBlockVector();
    }

    @Override
    public void consume(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player, getItem()) || hasPlayerItemInOffHand(player, getItem())) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void clicked(PlayerInteractEvent event) {
        if (event.getClickedBlock().getLocation().toVector().toBlockVector().equals(position)) {
            super.clicked(event);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

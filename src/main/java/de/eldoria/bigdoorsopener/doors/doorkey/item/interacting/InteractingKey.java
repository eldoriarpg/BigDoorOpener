package de.eldoria.bigdoorsopener.doors.doorkey.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.item.ItemKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public abstract class InteractingKey extends ItemKey {

    /**
     * A set of user which has clicked since the last check.
     */
    private Set<UUID> playersClicked;

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public InteractingKey(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return playersClicked.contains(player.getUniqueId());
    }

    /**
     * Takes the item hold by the item key from the player.
     *
     * @param player player which opened the door.
     */
    @Override
    public abstract void consume(Player player);

    /**
     * This method is called when player interact event is fired.
     * This method ideally adds the player to {@link #playersClicked} when the player can open the door with this key.
     *
     * @param event interact event to check.
     */
    public void clicked(PlayerInteractEvent event) {
        playersClicked.add(event.getPlayer().getUniqueId());
    }

    /**
     * This method is called after the check for the door of this key is done and a new evaluation cycle starts.
     */
    public void clear() {
        playersClicked.clear();
    }
}

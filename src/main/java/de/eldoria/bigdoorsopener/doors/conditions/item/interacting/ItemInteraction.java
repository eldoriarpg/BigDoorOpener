package de.eldoria.bigdoorsopener.doors.conditions.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public abstract class ItemInteraction extends Item {

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
    public ItemInteraction(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return playersClicked.contains(player.getUniqueId());
    }

    /**
     * Takes the item hold by the item key from the player.
     *
     * @param player player which opened the door.
     */
    @Override
    public abstract void used(Player player);

    /**
     * This method is called when player interact event is fired.
     * This method ideally adds the player to {@link #playersClicked} when the player can open the door with this key.
     *
     * @param event interact event to check.
     */
    public void clicked(PlayerInteractEvent event) {
        playersClicked.add(event.getPlayer().getUniqueId());
    }

    @Override
    public void evaluated() {
        playersClicked.clear();
    }
}

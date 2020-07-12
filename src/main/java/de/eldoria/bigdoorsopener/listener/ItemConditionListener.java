package de.eldoria.bigdoorsopener.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemInteraction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This listener controls when a player tries to open a door with a item.
 */
public class ItemConditionListener implements Listener {
    private final Config config;

    public ItemConditionListener(Config config) {
        this.config = config;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        config.getDoors().values().forEach(d -> {
            if (d.getConditionChain().getItem() == null) return;
            if (d.getConditionChain().getItem() instanceof ItemInteraction) {
                ((ItemInteraction) d.getConditionChain().getItem()).clicked(event);
            }
        });
    }
}

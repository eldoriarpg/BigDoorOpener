package de.eldoria.bigdoorsopener.conditions.item;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemInteraction;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

/**
 * This listener controls when a player tries to open a door with a item.
 */
public class ItemConditionListener extends BigDoorsAdapter implements Listener {
    private final Config config;

    public ItemConditionListener(BigDoors bigDoors, Config config) {
        super(bigDoors);
        this.config = config;
    }

    public void onConditionBag(ConditionBagModifiedEvent event) {
        event.conditionBag();
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        config.getDoors().forEach(d -> {
            if (!d.isEnabled()) return;
            List<DoorCondition> items = d.conditionBag().getConditions("item");
            if ((items.isEmpty())) return;
            for (DoorCondition item : items) {
                if (item instanceof ItemInteraction) {
                    ((ItemInteraction) item).clicked(event, isAvailableToOpen(d));
                }
            }
        });
    }
}

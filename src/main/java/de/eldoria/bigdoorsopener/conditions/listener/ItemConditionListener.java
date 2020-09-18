package de.eldoria.bigdoorsopener.conditions.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemInteraction;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import de.eldoria.eldoutilities.localization.Localizer;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * This listener controls when a player tries to open a door with a item.
 */
public class ItemConditionListener extends BigDoorsAdapter implements Listener {
    private final Config config;

    public ItemConditionListener(BigDoors bigDoors, Localizer localizer, Config config) {
        super(bigDoors);
        this.config = config;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        config.getDoors().forEach(d -> {
            Optional<DoorCondition> item = d.getConditionBag().getCondition("item");
            if ((!item.isPresent())) return;
            if (item.get() instanceof ItemInteraction) {
                ((ItemInteraction) item.get()).clicked(event, isAvailableToOpen(d));
            }
        });
    }
}

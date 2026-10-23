/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.item.interacting;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Forwards clicks to the item click and item block conditions they belong to.
 * <p>
 * The conditions are indexed by their key and rebuilt from the configuration whenever a door or its conditions change,
 * so replaced condition bags and reloaded doors are picked up.
 */
public class ItemConditionListener implements Listener {
    private final Config config;
    private final Map<InteractionKey, List<ItemInteraction>> interactions = new HashMap<>();

    public ItemConditionListener(Config config) {
        this.config = config;
        rebuild();
    }

    /**
     * Rebuilds the index from the doors in the configuration.
     */
    public void rebuild() {
        interactions.clear();
        for (ConditionalDoor door : config.getDoors()) {
            for (DoorCondition condition : door.conditionBag().getConditions("item")) {
                if (condition instanceof ItemInteraction interaction) {
                    interactions
                            .computeIfAbsent(getKey(door, interaction), k -> new ArrayList<>())
                            .add(interaction);
                }
            }
        }
    }

    private InteractionKey getKey(ConditionalDoor door, ItemInteraction interaction) {
        if (interaction instanceof ItemClick click) {
            return ClickInteractionKey.of(click);
        }
        if (interaction instanceof ItemBlock block) {
            return BlockInteractionKey.of(door, block);
        }
        throw new IllegalStateException("Unknown condition of type " + interaction.getClass());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConditionBagModified(ConditionBagModifiedEvent event) {
        rebuild();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDoorModified(DoorModifiedEvent event) {
        rebuild();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDoorRegistered(DoorRegisteredEvent event) {
        rebuild();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDoorUnregistered(DoorUnregisteredEvent event) {
        rebuild();
    }

    private void notify(InteractionKey key, PlayerInteractEvent event) {
        Optional.ofNullable(interactions.get(key)).ifPresent(c -> c.forEach(con -> con.clicked(event)));
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        BlockInteractionKey.of(event).ifPresent(key -> notify(key, event));
        ClickInteractionKey.of(event).ifPresent(key -> notify(key, event));
    }
}

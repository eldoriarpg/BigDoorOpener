/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.item.interacting;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import de.eldoria.bigdoorsopener.core.events.ConditionAddedEvent;
import de.eldoria.bigdoorsopener.core.events.ConditionRemovedEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This listener controls when a player tries to open a door with a item.
 */
public class ItemConditionListener extends BigDoorsAdapter implements Listener {
    private final Map<InteractionKey, List<ItemInteraction>> interactions = new HashMap<>();

    private void register(ConditionalDoor door, ItemInteraction interaction) {
        getOrComputeConditions(getKey(door, interaction)).add(interaction);
    }

    private void unregister(ConditionalDoor door, ItemInteraction interaction) {
        getOrComputeConditions(getKey(door, interaction)).remove(interaction);
    }

    private List<ItemInteraction> getOrComputeConditions(InteractionKey key) {
        return interactions.computeIfAbsent(key, k -> new ArrayList<>());
    }

    private InteractionKey getKey(ConditionalDoor door, ItemInteraction interaction) {
        if (interaction instanceof ItemClick) {
            return ClickInteractionKey.of((ItemClick) interaction);
        }
        if (interaction instanceof ItemBlock) {
            return BlockInteractionKey.of(door, (ItemBlock) interaction);
        }
        throw new IllegalStateException("Unknown condition of type " + interaction.getClass());
    }

    public ItemConditionListener(BigDoors bigDoors, Config config) {
        super(bigDoors);
        for (ConditionalDoor door : config.getDoors()) {
            for (DoorCondition condition : door.conditionBag().getConditions("item")) {
                if (condition instanceof ItemInteraction) {
                    register(door, (ItemInteraction) condition);
                }
            }
        }
    }

    @EventHandler
    public void onConditionAdd(ConditionAddedEvent event) {
        if (event.condition() instanceof ItemInteraction) {
            register(event.door(), (ItemInteraction) event.condition());
        }
    }

    @EventHandler
    public void onConditionRemove(ConditionRemovedEvent event) {
        if (event.condition() instanceof ItemInteraction) {
            unregister(event.door(), (ItemInteraction) event.condition());
        }
    }

    private void notify(InteractionKey key, PlayerInteractEvent event) {
        getConditions(key).ifPresent(c -> c.forEach(con -> con.clicked(event)));
    }

    private Optional<List<ItemInteraction>> getConditions(InteractionKey key) {
        return Optional.ofNullable(interactions.get(key));
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        BlockInteractionKey.of(event).ifPresent(key -> notify(key, event));
        ClickInteractionKey.of(event).ifPresent(key -> notify(key, event));
    }
}

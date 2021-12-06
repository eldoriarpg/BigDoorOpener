/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.item.interacting;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ClickInteractionKey implements InteractionKey{
    protected ItemStack stack;

    public ClickInteractionKey(ItemStack stack) {
        this.stack = stack.clone();
        this.stack.setAmount(1);
    }

    public static ClickInteractionKey of(ItemClick interaction) {
        return new ClickInteractionKey(interaction.item());
    }

    public static Optional<ClickInteractionKey> of(PlayerInteractEvent event) {
        ItemStack item;
        if (event.getHand() == EquipmentSlot.HAND) {
            item = event.getPlayer().getInventory().getItemInMainHand();
        } else {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }
        return Optional.of(new ClickInteractionKey(item));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClickInteractionKey that = (ClickInteractionKey) o;

        return stack.equals(that.stack);
    }

    @Override
    public int hashCode() {
        return stack.hashCode();
    }
}

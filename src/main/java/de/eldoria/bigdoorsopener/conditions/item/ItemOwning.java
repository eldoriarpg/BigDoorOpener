/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Opens the door for players carrying the key in their inventory.
 */
@JsonTypeName("itemOwningCondition")
public class ItemOwning extends Item {
    @JsonCreator
    public ItemOwning(@JsonProperty("item") ItemStack item, @JsonProperty("consumed") boolean consumed) {
        super(item, consumed);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemOwning.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    conditionBag.accept(new ItemOwning(keyFromHand(sender, args), consumed(args)));
                    messageSender.sendMessage(sender, "setCondition.itemOwning");
                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemOwning", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        takeFromInventory(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInInventory(player);
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.itemOwning", getClass())
                .append(Component.newline())
                .append(super.getDescription(localizer));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " itemOwning " + item().getAmount() + " " + isConsumed();
    }

    @Override
    public ItemOwning clone() {
        return new ItemOwning(item().clone(), isConsumed());
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.item.interacting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.item.Item;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Opens the door for players who clicked while holding the key in a hand.
 */
@JsonTypeName("itemClickCondition")
public class ItemClick extends ItemInteraction {
    @JsonCreator
    public ItemClick(@JsonProperty("item") ItemStack item, @JsonProperty("consumed") boolean consumed) {
        super(item, consumed);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemClick.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    conditionBag.accept(new ItemClick(keyFromHand(sender, args), consumed(args)));
                    messageSender.sendMessage(sender, "setCondition.itemClick");
                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemClick", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player)) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.itemClick", getClass())
                .append(Component.newline())
                .append(super.getDescription(localizer));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " itemClick " + item().getAmount() + " " + isConsumed();
    }

    @Override
    public ItemClick clone() {
        return new ItemClick(item().clone(), isConsumed());
    }
}

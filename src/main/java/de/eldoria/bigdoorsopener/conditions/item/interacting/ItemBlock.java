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
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

/**
 * Opens the door for players who click a block, the keyhole, while holding the key in a hand.
 */
@JsonTypeName("itemBlockCondition")
public class ItemBlock extends ItemInteraction {

    private BlockVector position;

    /**
     * Creates a new item block condition without a set position. This object is incomplete and has to be initialized
     * with the {@link #position(BlockVector)} method before using.
     *
     * @param item     item stack of the key
     * @param consumed true if item should be consumed.
     */
    public ItemBlock(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @JsonCreator
    private ItemBlock(
            @JsonProperty("position") Vector position,
            @JsonProperty("item") ItemStack stack,
            @JsonProperty("consumed") boolean consumed) {
        super(stack, consumed);
        this.position = position == null ? null : position.toBlockVector();
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemBlock.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    Player player = ConditionInput.player(sender);
                    ItemBlock itemBlock = new ItemBlock(keyFromHand(sender, args), consumed(args));

                    messageSender.sendMessage(player, "setCondition.clickKeyBlock");
                    RegisterInteraction.getInstance().register(player, (event, mSender) -> {
                        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                            return false;
                        }
                        if (event.getClickedBlock() == null) return false;
                        itemBlock.position(
                                event.getClickedBlock().getLocation().toVector().toBlockVector());
                        conditionBag.accept(itemBlock);
                        event.setCancelled(true);
                        mSender.sendMessage(player, "setCondition.itemBlockRegistered");
                        return true;
                    });
                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemBlock", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        takeFromInventory(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player)) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void clicked(PlayerInteractEvent event) {
        event.setCancelled(true);
        super.clicked(event);
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.itemBlock", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.keyhole") + " ", C.baseColor))
                .append(Component.text(String.valueOf(position), C.highlightColor))
                .append(Component.newline())
                .append(super.getDescription(localizer));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " itemBlock " + item().getAmount() + " " + isConsumed();
    }

    @Override
    public ItemBlock clone() {
        return new ItemBlock(position, item(), isConsumed());
    }

    public void position(BlockVector position) {
        this.position = position;
    }

    public BlockVector position() {
        return position;
    }
}

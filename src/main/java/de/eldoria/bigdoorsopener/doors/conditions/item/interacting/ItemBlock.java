package de.eldoria.bigdoorsopener.doors.conditions.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import lombok.Setter;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which opens a door, when the player is clicking at a specific block
 */
public class ItemBlock extends ItemInteraction {

    @Setter
    private BlockVector position;

    public ItemBlock(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void used(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player, getItem()) || hasPlayerItemInOffHand(player, getItem())) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void clicked(PlayerInteractEvent event) {
        if (event.getClickedBlock().getLocation().toVector().toBlockVector().equals(position)) {
            super.clicked(event);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemBlock",
                        Replacement.create("NAME", ConditionType.ITEM_BLOCK.keyName))).color(C.baseColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.keyhole")).color(C.baseColor))
                .append(TextComponent.builder(position.toString()).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }
}

package de.eldoria.bigdoorsopener.doors.conditions.item;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which will be open when the player is holding a key in his hand.
 */
public class ItemHolding extends ItemCondition {
    public ItemHolding(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void used(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInHand(player, getItem());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemHolding",
                        Replacement.create("NAME", ConditionType.ITEM_HOLDING.keyName))).color(C.baseColor)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }
}

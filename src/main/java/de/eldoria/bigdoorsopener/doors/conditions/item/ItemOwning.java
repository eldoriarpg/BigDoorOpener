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
 * A key which opens a doow, when the player has it in his inventory.
 */
public class ItemOwning extends ItemCondition {
    public ItemOwning(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void used(Player player) {
        if(!isConsumed()) return;
        takeFromInventory(player, getItem());
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInInventory(player, getItem());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemOwning",
                        Replacement.create("NAME", ConditionType.ITEM_OWNING.keyName))).color(C.baseColor)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }
}

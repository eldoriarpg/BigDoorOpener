package de.eldoria.bigdoorsopener.doors.conditions.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * A key which open the door, when right clicked.
 */
@SerializableAs("itemClickCondition")
public class ItemClick extends ItemInteraction {
    public ItemClick(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player, getItem()) || hasPlayerItemInOffHand(player, getItem())) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void used(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemClick",
                        Replacement.create("NAME", ConditionType.ITEM_CLICK.conditionName))).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }

    public static ItemClick deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemClick(stack, consumed);
    }
}

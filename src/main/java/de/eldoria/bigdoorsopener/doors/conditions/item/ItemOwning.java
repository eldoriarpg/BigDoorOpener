package de.eldoria.bigdoorsopener.doors.conditions.item;

import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * A key which opens a doow, when the player has it in his inventory.
 */
@SerializableAs("itemOwningCondition")
@ConditionScope(ConditionScope.Scope.PLAYER)
public class ItemOwning extends Item {
    public ItemOwning(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    @Override
    public void used(Player player) {
        if (!isConsumed()) return;
        takeFromInventory(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInInventory(player);
    }

    public ItemOwning deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemOwning(stack, consumed);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemOwning",
                        Replacement.create("NAME", ConditionType.ITEM_OWNING.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " itemOwning " + getItem().getAmount() + " " + isConsumed();
    }

    @Override
    public void evaluated() {

    }
}

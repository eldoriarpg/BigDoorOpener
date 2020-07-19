package de.eldoria.bigdoorsopener.doors.conditions.item.interacting;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which opens a door, when the player is clicking at a specific block
 */
@SerializableAs("itemBlockCondition")
public class ItemBlock extends ItemInteraction {

    @Setter
    private BlockVector position;

    /**
     * Creates a new item block condition without a set position.
     * This object is incomplete and has to be initialized with the {@link #setPosition(BlockVector)} method before using.
     *
     * @param item
     * @param consumed
     */
    public ItemBlock(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    /**
     * Constructor used for serialization.
     *
     * @param position position of block
     * @param stack    item stack of block
     * @param consumed true if item should be consumed.
     */
    private ItemBlock(BlockVector position, ItemStack stack, boolean consumed) {
        super(stack, consumed);
        this.position = position;
    }

    public static ItemBlock deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        BlockVector position = resolvingMap.getValue("position");
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemBlock(position, stack, consumed);
    }

    @Override
    public void used(Player player) {
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
    public void clicked(PlayerInteractEvent event, boolean available) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getLocation().toVector().toBlockVector().equals(position)) {
            event.setCancelled(true);

            super.clicked(event, available);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder(super.serialize())
                .add("position", position)
                .build();
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemBlock",
                        Replacement.create("NAME", ConditionType.ITEM_BLOCK.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.keyhole") + " ").color(C.baseColor))
                .append(TextComponent.builder(position.toString()).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " itemBlock " + getItem().getAmount() + " " + isConsumed();

    }
}

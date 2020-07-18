package de.eldoria.bigdoorsopener.doors.conditions.item;

import com.google.gson.Gson;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public abstract class Item implements DoorCondition {
    private final ItemStack item;
    private final boolean consumed;
    private static final Gson GSON;

    static {
        GSON = new Gson();
    }

    /**
     * Creates a new item key
     *
     * @param item     item stack which defines the item needed to open the door. amount matters.
     * @param consumed true if the items are consumed when the door is opened
     */
    public Item(ItemStack item, boolean consumed) {
        this.item = item;
        this.consumed = consumed;
    }

    /**
     * This method will be called when a door with this key was opened. Only once.
     *
     * @param player player which opened the door.
     */
    public abstract void used(Player player);

    /**
     * Checks if a player has a item in the off or main hand.
     *
     * @param player player to check
     * @return true if the player has the item in one of his hands.
     */
    protected boolean hasPlayerItemInHand(Player player) {
        return hasPlayerItemInMainHand(player) || hasPlayerItemInOffHand(player);
    }

    /**
     * Checks if a player has a item in the main hand.
     *
     * @param player player to check
     * @return true if the player has the item in his main hand.
     */
    protected boolean hasPlayerItemInMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() < getItem().getAmount()) {
            return false;
        }
        return item.isSimilar(getItem());
    }

    /**
     * Checks if a player has a item in the off hand.
     *
     * @param player player to check
     * @return true if the player has the item in his main hands.
     */
    protected boolean hasPlayerItemInOffHand(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();
        if (item.getAmount() < getItem().getAmount()) {
            return false;
        }
        return item.isSimilar(getItem());
    }

    /**
     * Checks if a player has a item in his inventory.
     *
     * @param player player to check
     * @return true if the player has the item in his inventory.
     */
    protected boolean hasPlayerItemInInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.containsAtLeast(getItem(), getItem().getAmount());
    }

    /**
     * Takes the item from the off hand.
     *
     * @param player player to take items from
     */
    protected void takeFromOffHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - getItem().getAmount());
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the main hand.
     *
     * @param player player to take items from
     */
    protected void takeFromMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - getItem().getAmount());
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    /**
     * Takes the item from the inventory.
     *
     * @param player player to take items from
     */
    protected void takeFromInventory(Player player) {
        player.getInventory().removeItem(getItem());
        player.updateInventory();
    }

    /**
     * Takes the item from the main or off hand.
     *
     * @param player player to take items from
     */
    protected boolean tryTakeFromHands(Player player) {
        if (hasPlayerItemInMainHand(player)) {
            takeFromMainHand(player);
            return true;
        } else if (hasPlayerItemInOffHand(player)) {
            takeFromOffHand(player);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("item", item)
                .add("consumed", consumed)
                .build();
    }

    /**
     * This method is called after the check for the door of this key is done and a new evaluation cycle starts.
     * Deletes any internal data in this key.
     */
    public void evaluated() {
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        ItemMeta meta = item.getItemMeta();
        TextComponent.Builder builder = TextComponent.builder();
        if (meta != null) {
            builder.content(meta.getDisplayName()).color(TextColors.AQUA);
            if (meta.getLore() != null) {
                for (String s : meta.getLore()) {
                    builder.append(TextComponent.newline())
                            .append(TextComponent.builder(s).color(TextColors.LIGHT_PURPLE));
                }
            }
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                builder.append(TextComponent.newline())
                        .append(TextComponent.builder(entry.getKey().getKey().getKey() + " "
                                + entry.getValue().toString())
                                .color(TextColors.GRAY));
            }
        }

        return TextComponent.builder().color(C.baseColor)
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.item") + " ").color(C.baseColor).build())
                .append(TextComponent.builder("[" + item.getType().name().toLowerCase() + "] x" + item.getAmount())
                        .hoverEvent(HoverEvent.showText(builder.build()))).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.consumed") + " ").color(C.baseColor))
                .append(TextComponent.builder(Boolean.toString(isConsumed()))).color(C.highlightColor).build();
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " item";
    }
}

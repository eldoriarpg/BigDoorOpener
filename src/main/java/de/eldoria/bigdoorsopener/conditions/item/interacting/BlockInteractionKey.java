package de.eldoria.bigdoorsopener.conditions.item.interacting;

import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.util.Optional;

public class BlockInteractionKey extends ClickInteractionKey {
    private final BlockVector position;
    private final String world;

    public BlockInteractionKey(ConditionalDoor door, ItemBlock item) {
        super(item.item());
        position = item.position();
        world = door.world();
    }

    public BlockInteractionKey(ItemStack stack, BlockVector position, String world) {
        super(stack);
        this.position = position;
        this.world = world;
    }

    public static BlockInteractionKey of(ConditionalDoor door, ItemBlock interaction) {
        return new BlockInteractionKey(door, interaction);
    }

    public static Optional<ClickInteractionKey> of(PlayerInteractEvent event) {
        if (!event.hasBlock()) {
            return Optional.empty();
        }
        ItemStack item;
        if (event.getHand() == EquipmentSlot.HAND) {
            item = event.getPlayer().getInventory().getItemInMainHand();
        } else {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }
        String world = event.getPlayer().getWorld().getName();
        BlockVector position = event.getClickedBlock().getLocation().toVector().toBlockVector();
        return Optional.of(new BlockInteractionKey(item, position, world));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockInteractionKey)) return false;
        if (!super.equals(o)) return false;

        BlockInteractionKey that = (BlockInteractionKey) o;

        if (!position.equals(that.position)) return false;
        return world.equals(that.world);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + position.hashCode();
        result = 31 * result + world.hashCode();
        return result;
    }
}

package de.eldoria.bigdoorsopener.doors.doorkey.location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A key which opens the door, when a player is inside a world guard region.
 */
@KeyParameter("locationKey")
public class RegionKey implements LocationKey {
    private ProtectedRegion region;

    public RegionKey(ProtectedRegion region) {

    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return region.contains(BukkitAdapter.asBlockVector(player.getLocation()));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

package de.eldoria.bigdoorsopener.doors.doorkey;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@KeyParameter("permissionKey")
public class PermissionKey implements DoorKey {
    private String permission;

    public PermissionKey(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return player.hasPermission(permission);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }
}

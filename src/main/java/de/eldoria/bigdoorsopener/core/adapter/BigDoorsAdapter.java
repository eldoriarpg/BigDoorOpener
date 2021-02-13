package de.eldoria.bigdoorsopener.core.adapter;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import lombok.Getter;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Adapter to interact with big doors internally.
 */
@Getter
public abstract class BigDoorsAdapter {
    private final Commander commander;
    private final BigDoors bigDoors;
    private final Server server = Bukkit.getServer();
    private final ILocalizer localizer;

    public BigDoorsAdapter(BigDoors bigDoors) {
        this.bigDoors = bigDoors;
        commander = bigDoors.getCommander();
        this.localizer = BigDoorsOpener.localizer();
    }

    /**
     * Set the state of the door.
     *
     * @param open true if the door should be open.
     * @param door door to set the state
     * @return true if the door state was changed succesfully which is indicated by {@link BigDoors#toggleDoor(long)}
     */
    protected boolean setDoorState(boolean open, ConditionalDoor door) {
        if (commander.isDoorBusy(door.getDoorUID())) {
            return false;
        }
        if (isOpen(door) == open) return false;
        return bigDoors.toggleDoor(door.getDoorUID());
    }

    /**
     * Check if a door is currently considered as open.
     * Will use the {@link ConditionalDoor#openInverted(boolean)} method.
     *
     * @param door door to check.
     * @return true when the door is open.
     */
    protected boolean isOpen(ConditionalDoor door) {
        return door.openInverted(bigDoors.isOpen(door.getDoorUID()));
    }

    /**
     * Checks if the world of a door exists and is present in the plugin.
     *
     * @param door door to check
     * @return true if the door exits
     */
    protected boolean doorExists(ConditionalDoor door) {
        World world = server.getWorld(door.getWorld());
        if (world == null) {
            BigDoorsOpener.logger().info(localizer.getMessage("error.worldIsNull",
                    Replacement.create("DOOR_NAME", door.getDoorUID())));
            return false;
        }

        // Make sure that this door still exists on the doors plugin.
        return getDoor(door.getDoorUID()) != null;
    }

    /**
     * Checks if a door is available.
     * A door is considered available if its not open and not busy
     *
     * @param door door to check
     * @return true if the door is available
     */
    protected boolean isAvailableToOpen(ConditionalDoor door) {
        return !isOpen(door) && !isBusy(door) && isDoorLoaded(getDoor(door.getDoorUID()));
    }

    /**
     * Checks if the chunks of a door are loaded
     *
     * @param door door to check
     * @return true if chunks around the door are loaded
     */
    protected boolean isDoorLoaded(Door door) {
        return bigDoors.areChunksLoadedForDoor(door);
    }

    /**
     * Checks if a door is busy. A door is busy if its closing or opening at the moment.
     *
     * @param door door to check
     * @return true if the door is busy
     */
    protected boolean isBusy(ConditionalDoor door) {
        return commander.isDoorBusy(door.getDoorUID());
    }

    /**
     * Get the door with the specified id if the player is the owner of the door.
     *
     * @param uid uid of the door.
     * @return door with id if exitsts
     */
    @Nullable
    protected Door getDoor(Player player, long uid) {
        return commander.getDoor(player.getUniqueId(), uid);
    }

    /**
     * Get the door with the specified id.
     *
     * @param doorId uid of the door.
     * @return door with id if exitsts
     */
    @Nullable
    protected Door getDoor(long doorId) {
        return commander.getDoor(null, doorId);
    }

    /**
     * Get the door with a specific id.
     *
     * @param doorId id of the door as long or string
     * @return Door with id if exists.
     */
    @Nullable
    protected Door getDoor(String doorId) {
        return getDoor(null, doorId);
    }

    /**
     * Get the door with a specific id when the player the permission to access this door.
     *
     * @param player owner of the door.
     * @param doorId id of the door as long or string
     * @return Door with id if exists.
     */
    @Nullable
    protected Door getDoor(Player player, String doorId) {
        return commander.getDoor(doorId, player);
    }

    /**
     * Get a list of doors owned by the player.
     *
     * @param player Player for which the doors should be retrieved.
     * @return List of doors which are owned by the player
     */
    protected List<Door> getDoors(Player player) {
        return getDoors(player, null);
    }

    /**
     * Get a list of doors owned by the player, which match the name.
     *
     * @param player Player for which the doors should be retrieved.
     * @param name   Get doors which match the name.
     * @return List of doors which are owned by the player
     */
    protected List<Door> getDoors(Player player, String name) {
        return commander.getDoors(player.getUniqueId().toString(), name);
    }

    /**
     * Get a set of all existing doors. The door will always be owned by the creator.
     *
     * @return A set of doors.
     */
    protected Set<Door> getDoors() {
        return commander.getDoors();
    }

    protected ILocalizer getLocalizer() {
        return localizer;
    }
}

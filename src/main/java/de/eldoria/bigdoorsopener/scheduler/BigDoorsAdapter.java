package de.eldoria.bigdoorsopener.scheduler;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import lombok.Getter;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

/**
 * Adapter to interact with big doors internally.
 */
@Getter
public abstract class BigDoorsAdapter {
    private final Commander commander;
    private final BigDoors bigDoors;
    private final Server server = Bukkit.getServer();
    private final Localizer localizer;

    public BigDoorsAdapter(BigDoors bigDoors, Localizer localizer) {
        this.bigDoors = bigDoors;
        commander = bigDoors.getCommander();
        this.localizer = localizer;
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
        return commander.getDoor(null, door.getDoorUID()) != null;
    }

    /**
     * Checks if a door is availbale.
     * A door is considered available if its not open and not busy
     *
     * @param door door to check
     * @return true if the door is available
     */
    protected boolean isAvailableToOpen(ConditionalDoor door) {
        return !isOpen(door) && !isBusy(door);
    }

    protected boolean isBusy(ConditionalDoor door) {
        return getCommander().isDoorBusy(door.getDoorUID());
    }
}

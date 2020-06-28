package de.eldoria.bigdoorsopener.scheduler;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import lombok.Getter;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

@Getter
public abstract class BigDoorsAdapter {
    private final Commander commander;
    private final BigDoors bigDoors;
    private final Server server = Bukkit.getServer();

    public BigDoorsAdapter(BigDoors bigDoors) {
        this.bigDoors = bigDoors;
        commander = bigDoors.getCommander();
    }

    protected void setDoorState(boolean open, TimedDoorScheduler.ScheduledDoor door) {
        setDoorState(open, door.getDoor());
    }

    protected void setDoorState(boolean open, TimedDoor door) {
        if (commander.isDoorBusy(door.getDoorUID())) {
            return;
        }
        if (isOpen(door) == open) return;
        bigDoors.toggleDoor(door.getDoorUID());
    }

    /**
     * Checks if the world of a door exists and is present in the plugin.
     *
     * @param door door to check;
     * @return true if the door exits
     */
    protected boolean doorExists(TimedDoor door) {
        World world = server.getWorld(door.getWorld());
        if (world == null) {
            BigDoorsOpener.logger().info("World of door " + door.getDoorUID() + " is null. Removed.");
            return false;
        }

        // Make sure that this door still exists on the doors plugin.
        return commander.getDoor(String.valueOf(door.getDoorUID()), null) != null;
    }

    protected boolean isOpen(TimedDoor door) {
        return door.openInverted(bigDoors.isOpen(door.getDoorUID()));
    }
}

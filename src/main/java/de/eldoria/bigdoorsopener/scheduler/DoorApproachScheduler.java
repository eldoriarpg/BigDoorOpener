package de.eldoria.bigdoorsopener.scheduler;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DoorApproachScheduler extends BigDoorsAdapter implements Runnable {
    private final Server server = Bukkit.getServer();
    private final Config config;
    private Logger logger = BigDoorsOpener.logger();

    public DoorApproachScheduler(Config config, BigDoors doors, Localizer localizer) {
        super(doors, localizer);
        this.config = config;
    }

    @Override
    public void run() {
        List<Player> onlinePlayers = new ArrayList<>(server.getOnlinePlayers());

        Set<TimedDoor> openDoors = new HashSet<>();

        // search for doors where the player is near enough
        for (Player player : onlinePlayers) {
            List<TimedDoor> worldDoors = config.getDoors().values().stream()
                    .filter(w -> w.getWorld().equalsIgnoreCase(player.getWorld().getName()))
                    .collect(Collectors.toList());

            Vector playerPos = player.getLocation().toVector();
            for (TimedDoor door : worldDoors) {
                if (!(door.getOpenRange() > 0)) {
                    continue;
                }

                // Check if door is in range of player.
                if (!quickNearCheck(playerPos, door.getPosition(), door.getOpenRange())) continue;
                if (playerPos.distanceSquared(door.getPosition()) > Math.pow(door.getOpenRange(), 2)) continue;
                if (!door.canOpen(player)) return;
                openDoors.add(door);
            }
        }

        // mark doors without a player in reach as closeable
        Set<TimedDoor> closeDoors = config.getDoors().values().stream()
                .filter(d -> !openDoors.contains(d) && d.getOpenRange() > 0).collect(Collectors.toSet());

        // open closed doors
        for (TimedDoor door : openDoors) {
            if (!doorExists(door)) {
                config.getDoors().remove(door.getDoorUID());
                config.safeConfig();
                continue;
            }
            setDoorState(true, door);
        }

        // close open doors if they should be closed based on time.
        for (TimedDoor door : closeDoors) {
            if (!doorExists(door)) {
                config.getDoors().remove(door.getDoorUID());
                config.safeConfig();
                continue;
            }
            //if (doors.isOpen(door.getDoorUID())) continue;
            World world = server.getWorld(door.getWorld());
            if (!door.shouldBeOpen(world.getFullTime())) {
                setDoorState(false, door);
            }
        }
    }

    private boolean quickNearCheck(Vector point, Vector target, double range) {
        if (Math.abs(point.getX() - target.getX()) > range) return false;
        if (Math.abs(point.getY() - target.getY()) > range) return false;
        if (Math.abs(point.getZ() - target.getZ()) > range) return false;
        return true;
    }
}

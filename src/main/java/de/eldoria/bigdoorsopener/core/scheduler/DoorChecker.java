package de.eldoria.bigdoorsopener.core.scheduler;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.functions.TriFunction;
import de.eldoria.eldoutilities.localization.Localizer;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class DoorChecker extends BigDoorsAdapter implements Runnable, Listener {

    private final Queue<ConditionalDoor> doors = new LinkedList<>();
    private final Server server = Bukkit.getServer();

    private final Config config;

    private final Set<ConditionalDoor> open = new HashSet<>();
    private final Set<ConditionalDoor> close = new HashSet<>();
    private final Set<ConditionalDoor> evaluated = new HashSet<>();
    private final Cache<String, List<Player>> worldPlayers = C.getShortExpiringCache();
    private final Cache<Long, Boolean> chunkStateCache = C.getShortExpiringCache();
    private final TriFunction<Vector, Vector, Vector, Boolean> proximity = Proximity.ProximityForm.CUBOID.check;
    private double doorUpdateInterval;

    public DoorChecker(Config config, BigDoors bigDoors, Localizer localizer) {
        super(bigDoors);
        this.config = config;
        doors.addAll(config.getDoors());
    }

    @EventHandler
    public void onDoorRegister(DoorRegisteredEvent event) {
        if (!doors.contains(event.getDoor())) {
            doors.add(event.getDoor());
        }
    }

    @EventHandler
    public void onDoorUnregister(DoorUnregisteredEvent event) {
        doors.remove(event.getDoor());
    }

    /**
     * Clears all registered doors and load them from the condfiguration.
     * Therefore it should be executed AFTER {@link Config#reloadConfig()}
     */
    public void reload() {
        synchronized (doors) {
            doors.clear();
            doors.addAll(config.getDoors());
        }
    }

    @Override
    public void run() {
        if (doors.isEmpty()) return;

        doorUpdateInterval += doors.size() / (double) config.getRefreshRate();


        open.clear();
        close.clear();
        evaluated.clear();

        Map<Long, Player> openedBy = new HashMap<>();

        while (doorUpdateInterval > 1) {
            doorUpdateInterval--;
            // poll from queue and append door again.
            ConditionalDoor door = doors.poll();
            assert door != null : "Door is null. How could this happen?";

            if (!doorExists(door)) {
                config.removeDoor(door.getDoorUID());
                BigDoorsOpener.logger().info("Door with id " + door.getDoorUID() + " has been deleted. Removing.");
                continue;
            }

            doors.add(door);

            World world = server.getWorld(door.getWorld());
            // If the world of the door does not exists, why should we evaluate it.
            if (world == null) continue;

            // check if chunk of door is loaded. if not skip.
            try {
                if (chunkStateCache.get(door.getDoorUID(), () -> !isDoorLoaded(getDoor(door.getDoorUID())))) {
                    // Skip doors in unloaded chunks
                    continue;
                }
            } catch (ExecutionException e) {
                BigDoorsOpener.logger().log(Level.WARNING,
                        "An error occured while calculating the chunk cache state. Please report this.", e);
                continue;
            }

            // skip busy doors. bcs why should we try to open/close a door we cant open/close
            if (getCommander().isDoorBusy(door.getDoorUID()) || !door.isEnabled()) {
                continue;
            }

            // collect all doors we evaluated.
            evaluated.add(door);

            boolean open = isOpen(door);

            //Check if the door really needs a per player evaluation
            if (door.requiresPlayerEvaluation()) {
                boolean opened = false;
                // Evaluate door per player. If one player can open it, it will open.
                try {
                    boolean checked = false;
                    for (Player player : worldPlayers.get(world.getName(), world::getPlayers)) {
                        if (!proximity.apply(door.getPosition(),
                                player.getLocation().toVector(),
                                config.getPlayerCheckRadius())) {
                            continue;
                        }
                        checked = true;
                        if (door.getState(player, world, open)) {
                            opened = true;
                            // only open the door if its not yet open. because why open it then.
                            if (!open) {
                                this.open.add(door);
                                openedBy.put(door.getDoorUID(), player);
                            }
                            break;
                        }
                    }
                    if (!checked) {
                        if (door.getState(null, world, open)) {
                            opened = true;
                            // only open the door if its not yet open. because why open it then.
                            if (!open) {
                                this.open.add(door);
                                openedBy.put(door.getDoorUID(), null);
                            }
                            break;
                        }
                    }
                } catch (ExecutionException e) {
                    BigDoorsOpener.logger().log(Level.WARNING, "Failed to compute. Please report this.", e);
                }
                if (!opened && open) {
                    close.add(door);
                }
            } else {
                // Evaluate door.
                if (door.getState(null, world, open)) {
                    if (!open) {
                        this.open.add(door);
                    }
                } else {
                    if (open) {
                        close.add(door);
                    }
                }
            }
        }

        // Open doors
        for (ConditionalDoor conditionalDoor : open) {
            if (setDoorState(true, conditionalDoor)) {
                conditionalDoor.opened(openedBy.get(conditionalDoor.getDoorUID()));
            }
        }

        // Close doors
        for (ConditionalDoor conditionalDoor : close) {
            setDoorState(false, conditionalDoor);
        }

        // Notify doors that they were evaluated.
        for (ConditionalDoor conditionalDoor : evaluated) {
            conditionalDoor.evaluated();
        }
    }
}

package de.eldoria.bigdoorsopener.scheduler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DoorChecker extends BigDoorsAdapter implements Runnable {

    private final Queue<ConditionalDoor> doors = new LinkedList<>();
    private final Server server = Bukkit.getServer();

    private final Config config;

    private final Set<ConditionalDoor> open = new HashSet<>();
    private final Set<ConditionalDoor> close = new HashSet<>();
    private final Set<ConditionalDoor> evaluated = new HashSet<>();

    private final Cache<String, List<Player>> worldPlayers = C.getShortExpiringCache();

    public DoorChecker(Config config, BigDoors bigDoors, Localizer localizer) {
        super(bigDoors, localizer);
        this.config = config;
        doors.addAll(config.getDoors().values());
    }

    /**
     * Registers a new door at the door checker.
     * Only registered doors will do anything.
     * The door will only be registered if its not yet registered.
     *
     * @param door door to register.
     */
    public void register(ConditionalDoor door) {
        if (!doors.contains(door)) {
            doors.add(door);
        }
    }

    /**
     * Unregister a door.
     *
     * @param door door to unregister.
     */
    public void unregister(ConditionalDoor door) {
        doors.remove(door);
    }

    /**
     * Clears all registered doors and load them from the condfiguration.
     * Therefore it should be executed AFTER {@link Config#reloadConfig()}
     */
    public void reload() {
        synchronized (doors) {
            doors.clear();
            doors.addAll(config.getDoors().values());
        }
    }

    @Override
    public void run() {
        if (doors.isEmpty()) return;

        int count = (int) Math.max(Math.ceil((double) doors.size() / config.getRefreshRate()), 1);

        open.clear();
        close.clear();
        evaluated.clear();

        Map<Long, Player> openedBy = new HashMap<>();

        for (int i = 0; i < count; i++) {
            // poll from queue and append door again.
            ConditionalDoor door = doors.poll();
            assert door != null : "Door is null. How could this happen?";

            if (!doorExists(door)) {
                config.getDoors().remove(door.getDoorUID());
                BigDoorsOpener.logger().info("Door with id " + door.getDoorUID() + " has been deleted. Removing.");
                config.safeConfig();
                continue;
            }

            doors.add(door);

            // skip busy doors. bcs why should we try to open/close a door we cant open/close
            if (getCommander().isDoorBusy(door.getDoorUID())) {
                continue;
            }

            // collect all doors we evaluated.
            evaluated.add(door);


            World world = server.getWorld(door.getWorld());
            // If the world of the door does not exists, why should we evaluate it.
            if (world == null) continue;

            boolean open = isOpen(door);


            //Check if the door really needs a per player evaluation
            if (door.requiresPlayerEvaluation()) {
                boolean opened = false;
                // Evaluate door per player. If one player can open it, it will open.
                try {
                    for (Player player : worldPlayers.get(world.getName(), world::getPlayers)) {
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

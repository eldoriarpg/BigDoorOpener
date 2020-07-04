package de.eldoria.bigdoorsopener.scheduler;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class DoorChecker extends BigDoorsAdapter implements Runnable {

    private final Queue<ConditionalDoor> doors = new LinkedList<>();
    private final Server server = Bukkit.getServer();

    private final Config config;

    private final Set<ConditionalDoor> open = new HashSet<>();
    private final Set<ConditionalDoor> close = new HashSet<>();
    private final Set<ConditionalDoor> evaluated = new HashSet<>();

    public DoorChecker(Config config, BigDoors bigDoors, Localizer localizer) {
        super(bigDoors, localizer);
        this.config = config;
    }

    public void register(ConditionalDoor door) {
        if (!doors.contains(door)) {
            doors.add(door);
        }
    }

    @Override
    public void run() {
        if (doors.isEmpty()) return;

        int count = (int) Math.max(Math.ceil((double) doors.size() / config.getRefreshRate()), 1);

        open.clear();
        close.clear();
        evaluated.clear();

        for (int i = 0; i < count; i++) {
            // poll from queue and append door again.
            ConditionalDoor door = doors.poll();
            doors.add(door);

            // collect all doors we evaluated.
            evaluated.add(door);

            assert door != null : "Door is null. How could this happen?";

            World world = server.getWorld(door.getWorld());
            // If the world of the door does not exists, why should we evaluate it.
            if (world == null) continue;


            boolean open = isOpen(door);

            //Check if the door really needs a per player evaluation
            if (door.requiresPlayerEvaluation()) {
                boolean opened = false;
                // Evaluate door per player. If one player can open it, it will open.
                for (Player player : world.getPlayers()) {
                    if (door.getState(player, world, open)) {
                        opened = true;
                        // only open the door if its not yet open. because why open it then.
                        if (!open) {
                            this.open.add(door);
                            door.opened(player);
                        }
                        break;
                    }
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
            setDoorState(true, conditionalDoor);
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

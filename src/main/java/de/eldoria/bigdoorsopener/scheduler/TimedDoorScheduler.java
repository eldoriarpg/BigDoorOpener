package de.eldoria.bigdoorsopener.scheduler;


import com.google.common.base.Function;
import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TimedDoorScheduler implements Runnable {
    private final Queue<ScheduledDoor> closeQueue = new PriorityQueue<>(Comparator.comparingLong(ScheduledDoor::getTick));
    private final Queue<ScheduledDoor> openQueue = new PriorityQueue<>(Comparator.comparingLong(ScheduledDoor::getTick));

    private final Server server = Bukkit.getServer();

    private final Commander commander;
    private final BigDoors bigDoors;
    private final Config config;

    private final Logger logger = BigDoorsOpener.logger();

    public TimedDoorScheduler(Commander commander, BigDoors bigDoors, Config config) {
        this.commander = commander;
        this.bigDoors = bigDoors;
        this.config = config;
        reload();
    }

    @Override
    public void run() {
        process(openQueue, closeQueue, true, s -> s.getDoor().getTicksClose());
        process(closeQueue, openQueue, false, s -> s.getDoor().getTicksOpen());
    }

    private ScheduledDoor savepeekQueue(Queue<ScheduledDoor> queue) {
        ScheduledDoor peek = queue.peek();

        if (peek == null) return null;

        // check if world of door still exists.
        World world = server.getWorld(peek.getDoor().getWorld());
        if (world == null) {
            BigDoorsOpener.logger().info("World of door " + peek.getDoor().getDoorUID() + " is null. Removed.");
            config.getDoors().remove(peek.getDoor().getDoorUID());
            config.safeConfig();
            queue.remove();
            return null;
        }

        // Make sure that this door still exists on the doors plugin.
        if (commander.getDoor(String.valueOf(peek.getDoor().getDoorUID()), null) == null) {
            config.getDoors().remove(peek.getDoor().getDoorUID());
            config.safeConfig();
            return null;
        }

        return peek;
    }

    /**
     * Check if a door need to be closed.
     */
    private void process(Queue<ScheduledDoor> current, Queue<ScheduledDoor> next, boolean open,
                         Function<ScheduledDoor, Integer> nextTickEvent) {
        if (current.isEmpty()) return;

        while (!current.isEmpty()) {
            ScheduledDoor scheduledDoor = savepeekQueue(current);
            if (scheduledDoor == null) continue;

            long fullTime = server.getWorld(scheduledDoor.getDoor().getWorld()).getFullTime();

            if (scheduledDoor.getTick() > fullTime) {
                // We assume that the first and all following items are not ready yet.
                break;
            }

            // We will take this object so we use the peek and remove the head of the queue.
            current.remove();

            // Set the door state
            setDoorState(open, scheduledDoor);

            // now schedule the open again.
            scheduledDoor.setTick(nextTime(fullTime, nextTickEvent.apply(scheduledDoor)));

            // and add it to the open queue.
            next.add(scheduledDoor);
        }
    }

    private void setDoorState(boolean open, ScheduledDoor door) {
        if (!bigDoors.isOpen(door.getDoor().getDoorUID()) && open) return;
        bigDoors.toggleDoor(door.getDoor().getDoorUID());
    }

    private long nextTime(long fullTime, long nextTime) {
        return getDiff(fullTime, nextTime) + fullTime;
    }

    private long getDiff(long fullTime, long nextTime) {
        long currentTime = fullTime % 24000;
        return currentTime > nextTime ? 24000 - currentTime + nextTime : nextTime - currentTime;
    }

    public void registerDoor(TimedDoor door) {
        if (isScheduled(door)) {
            return;
        }

        World world = server.getWorld(door.getWorld());
        if (world == null) {
            return;
        }

        long fullTime = world.getFullTime();

        if (door.shouldBeOpen(fullTime)) {
            ScheduledDoor scheduledDoor = new ScheduledDoor(nextTime(fullTime, door.getTicksClose()), door);
            setDoorState(true, scheduledDoor);
            closeQueue.add(scheduledDoor);
        } else {
            ScheduledDoor scheduledDoor = new ScheduledDoor(door.getTicksOpen(), door);
            setDoorState(false, scheduledDoor);
            openQueue.add(scheduledDoor);
        }
    }

    public void reload() {
        closeQueue.clear();
        openQueue.clear();
        config.getDoors().values().forEach(this::registerDoor);
    }

    private boolean isScheduled(TimedDoor door) {
        List<TimedDoor> values = closeQueue.stream().map(ScheduledDoor::getDoor).collect(Collectors.toList());
        values.addAll(openQueue.stream().map(ScheduledDoor::getDoor).collect(Collectors.toList()));
        return values.contains(door);
    }

    @Getter
    public static class ScheduledDoor {
        @Setter
        private long tick;
        private final TimedDoor door;

        public ScheduledDoor(long tick, @NonNull TimedDoor door) {
            this.tick = tick;
            this.door = door;
            door.getOpenRange();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScheduledDoor that = (ScheduledDoor) o;
            return Objects.equal(door.getDoorUID(), that.door.getDoorUID());
        }

        @Override
        public int hashCode() {
            return door.hashCode();
        }
    }
}

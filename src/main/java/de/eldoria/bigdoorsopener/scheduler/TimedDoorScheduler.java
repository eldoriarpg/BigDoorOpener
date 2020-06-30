package de.eldoria.bigdoorsopener.scheduler;


import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.eldoutilities.localization.Localizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TimedDoorScheduler extends BigDoorsAdapter implements Runnable {
    private final Queue<ScheduledDoor> closeQueue = new PriorityQueue<>(Comparator.comparingLong(ScheduledDoor::getTick));
    private final Queue<ScheduledDoor> openQueue = new PriorityQueue<>(Comparator.comparingLong(ScheduledDoor::getTick));

    private final Server server = Bukkit.getServer();

    private final Config config;

    private final Logger logger = BigDoorsOpener.logger();

    public TimedDoorScheduler(BigDoors bigDoors, Config config, Localizer localizer) {
        super(bigDoors, localizer);
        this.config = config;
        reload();
    }

    @Override
    public void run() {
        process(openQueue, closeQueue, true, (s, l) -> s.getDoor().nextClose(l));
        process(closeQueue, openQueue, false, (s, l) -> s.getDoor().nextOpen(l));
    }

    private ScheduledDoor savePeekQueue(Queue<ScheduledDoor> queue) {
        ScheduledDoor peek = queue.peek();

        if (peek == null) return null;

        if (doorExists(peek.door) && peek.getDoor().getTicksClose() != peek.getDoor().getTicksOpen()) {
            return peek;
        }

        config.getDoors().remove(peek.getDoor().getDoorUID());
        config.safeConfig();
        return null;
    }

    /**
     * Check if a door need to be closed.
     */
    private void process(Queue<ScheduledDoor> current, Queue<ScheduledDoor> next, boolean open,
                         BiFunction<ScheduledDoor, Long, Long> nextTickEvent) {
        if (current.isEmpty()) return;

        while (!current.isEmpty()) {
            ScheduledDoor scheduledDoor = savePeekQueue(current);
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
            scheduledDoor.setTick(nextTickEvent.apply(scheduledDoor, fullTime));

            // and add it to the open queue.
            next.add(scheduledDoor);
        }
    }

    public void registerDoor(TimedDoor door) {
        if (isScheduled(door)) {
            return;
        }

        if (!doorExists(door)) {
            return;
        }

        if (door.getTicksClose() == door.getTicksClose()) {
            return;
        }

        World world = server.getWorld(door.getWorld());

        long fullTime = world.getFullTime();

        if (door.shouldBeOpen(fullTime)) {
            ScheduledDoor scheduledDoor = new ScheduledDoor(door.nextClose(fullTime), door);
            setDoorState(true, scheduledDoor);
            closeQueue.add(scheduledDoor);
        } else {
            ScheduledDoor scheduledDoor = new ScheduledDoor(door.nextOpen(fullTime), door);
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

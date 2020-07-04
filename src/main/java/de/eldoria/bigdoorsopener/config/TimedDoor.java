package de.eldoria.bigdoorsopener.config;

import com.google.common.base.Objects;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;

@Getter
@Setter
@Deprecated
public class TimedDoor implements ConfigurationSerializable {
    /**
     * UUID of the door.
     */
    private final long doorUID;
    /**
     * Name of the world the door is in.
     */
    private final String world;
    /**
     * Mass center of the door.
     */
    private final Vector position;

    /**
     * Permission which is needed for the gate
     */
    private String permission = "";
    /**
     * The ticks from when to door should be closed
     */
    private int ticksClose = 0;
    /**
     * The ticks from when the door should be open.
     */
    private int ticksOpen = 14000;
    /**
     * If a player is in this range the door will open.
     * If not the door will be closed if open.
     */
    private double openRange = 10;
    /**
     * Represents the current required state of the door.
     */
    private boolean invertOpen = false;

    public TimedDoor(long doorUID, String world, Vector position) {
        this.doorUID = doorUID;
        this.world = world;
        this.position = position;
    }

    public TimedDoor(long doorUID, String world, Vector position, String permission, int ticksClose, int ticksOpen, double openRange, boolean invertOpen) {
        this(doorUID, world, position);
        this.permission = permission;
        this.ticksClose = ticksClose;
        this.ticksOpen = ticksOpen;
        this.openRange = openRange;
        this.invertOpen = invertOpen;
    }

    @Override
    public Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("doorUID", String.valueOf(doorUID))
                .add("world", world)
                .add("position", position)
                .add("permission", permission)
                .add("ticksClose", ticksClose)
                .add("ticksOpen", ticksOpen)
                .add("range", openRange)
                .add("invertOpen", invertOpen)
                .build();
    }

    public static TimedDoor deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        long doorUID = Long.parseLong(resolvingMap.getValue("doorUID"));
        String world = resolvingMap.getValue("world");
        Vector position = resolvingMap.getValue("position");
        String permission = resolvingMap.getValue("permission");
        int ticksClose = resolvingMap.getValue("ticksClose");
        int ticksOpen = resolvingMap.getValue("ticksOpen");
        double range = resolvingMap.getValue("range");
        boolean invertOpen = resolvingMap.getValue("invertOpen");
        return new TimedDoor(doorUID, world, position,permission, ticksClose, ticksOpen, range, invertOpen);
    }

    public boolean shouldBeOpen(long fulltime) {
        // This is a permanent closed door.
        if (isPermanentlyClosed()) {
            return false;
        }

        long openInTicks = getDiff(fulltime, getTicksOpen());
        long closedInTicks = getDiff(fulltime, getTicksClose());
        if (openInTicks > closedInTicks) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimedDoor timedDoor = (TimedDoor) o;
        return doorUID == timedDoor.doorUID;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(doorUID);
    }

    public boolean openInverted(boolean open) {
        if (invertOpen) return !open;
        return open;
    }

    public long nextClose(long fullTime) {
        return nextTime(fullTime, ticksClose);
    }

    public long nextOpen(long fullTime) {
        return nextTime(fullTime, ticksOpen);
    }

    private long nextTime(long fullTime, long nextTime) {
        return getDiff(fullTime, nextTime) + fullTime;
    }

    private long getDiff(long fullTime, long nextTime) {
        long currentTime = fullTime % 24000;
        return currentTime > nextTime ? 24000 - currentTime + nextTime : nextTime - currentTime;
    }

    public boolean canOpen(Player player) {
        if (permission.isEmpty()) return true;
        return player.hasPermission(permission);
    }

    public boolean isPermanentlyClosed() {
        return ticksClose == ticksOpen;
    }

    public void setTicks(int open, int close) {
        ticksOpen = open;
        ticksClose = close;
    }
}

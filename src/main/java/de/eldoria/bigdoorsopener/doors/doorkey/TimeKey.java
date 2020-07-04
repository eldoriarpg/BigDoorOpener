package de.eldoria.bigdoorsopener.doors.doorkey;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A key which defines the door state by current world time.
 */
@KeyParameter("timeKey")
public class TimeKey implements DoorKey {
    /**
     * The ticks from when to door should be closed
     */
    private int openTick;
    /**
     * The ticks from when the door should be open.
     */
    private int closeTick;

    private boolean forceState;

    private State state = null;

    /**
     * Creates a time key which opens and closes the door based on time.
     * Use the forcestate to define if the door should change the state only one time
     * when its timepoint is reached, or if it should try to stay in the right state.
     *
     * @param openTick   when the door should open
     * @param closeTick  when the door should close
     * @param forceState if true the state will be forced.
     */
    public TimeKey(int openTick, int closeTick, boolean forceState) {
        this.openTick = openTick;
        this.closeTick = closeTick;
        this.forceState = forceState;
    }

    @Override
    public boolean isOpen(@Nullable Player player, World world, @Nullable ConditionalDoor door, boolean currentState) {
        return shouldBeOpen(world.getFullTime(), currentState);
    }

    public boolean shouldBeOpen(long fulltime, boolean currentState) {
        long openInTicks = getDiff(fulltime, openTick);
        long closedInTicks = getDiff(fulltime, closeTick);
        // check if door should be open
        if (openInTicks > closedInTicks) {
            // attemt to open door
            if (state == null || state == State.CLOSED) {
                state = State.OPEN;
                return true;
            } else if (forceState) {
                return true;
            }
        } else {
            // attemt to close door
            if (state == null || state == State.OPEN) {
                state = State.CLOSED;
                return false;
            } else if (forceState) {
                return false;
            }
        }
        return currentState;
    }

    private long getDiff(long fullTime, long nextTime) {
        long currentTime = fullTime % 24000;
        return currentTime > nextTime ? 24000 - currentTime + nextTime : nextTime - currentTime;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    private enum State {OPEN, CLOSED}
}

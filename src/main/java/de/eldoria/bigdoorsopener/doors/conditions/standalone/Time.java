package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A key which defines the door state by current world time.
 */
@SerializableAs("timeCondition")
public class Time implements DoorCondition {
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
    public Time(int openTick, int closeTick, boolean forceState) {
        this.openTick = openTick;
        this.closeTick = closeTick;
        this.forceState = forceState;
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, @Nullable ConditionalDoor door, boolean currentState) {
        return shouldBeOpen(world.getFullTime());
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.time",
                        Replacement.create("NAME", ConditionType.TIME.conditionName))).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.open") + " ").color(C.baseColor))
                .append(TextComponent.builder(Parser.parseTicksToTime(openTick)).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.close") + " ").color(C.baseColor))
                .append(TextComponent.builder(Parser.parseTicksToTime(closeTick)).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.forceState") + " ").color(C.baseColor))
                .append(TextComponent.builder(Boolean.toString(forceState)).color(C.highlightColor))
                .build();
    }

    public Boolean shouldBeOpen(long fulltime) {
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
        return null;
    }

    private long getDiff(long fullTime, long nextTime) {
        long currentTime = fullTime % 24000;
        return currentTime > nextTime ? 24000 - currentTime + nextTime : nextTime - currentTime;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("openTick", openTick)
                .add("closeTick", closeTick)
                .add("forceState", forceState)
                .build();
    }

    public static Time deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        int openTick = resolvingMap.getValue("openTick");
        int closeTick = resolvingMap.getValue("closeTick");
        boolean forceState = resolvingMap.getValue("forceState");
        return new Time(openTick, closeTick, forceState);
    }

    private enum State {OPEN, CLOSED}
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.DoorState;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Opens and closes the door at times of the day.
 * <p>
 * Without {@code forceState} the door is only opened or closed when the time is reached, so it can still be toggled by
 * hand in between. With it the door is kept in the state the time asks for. The state of all time conditions is cached
 * briefly, since the time does not change suddenly.
 */
@JsonTypeName("timeCondition")
public class Time implements DoorCondition {
    private static final Cache<Long, Optional<Boolean>> STATE_CACHE = C.getShortExpiringCache();
    private static final int TICKS_PER_DAY = 24000;
    /**
     * The tick from when the door should be open.
     */
    private final int openTick;
    /**
     * The tick from when the door should be closed.
     */
    private final int closeTick;
    private final boolean forceState;
    @JsonIgnore
    private DoorState state = null;

    /**
     * Creates a time condition which opens and closes the door based on time.
     *
     * @param openTick   when the door should open
     * @param closeTick  when the door should close
     * @param forceState if true the state will be forced.
     */
    @JsonCreator
    public Time(@JsonProperty("openTick") int openTick, @JsonProperty("closeTick") int closeTick,
                @JsonProperty("forceState") boolean forceState) {
        this.openTick = openTick;
        this.closeTick = closeTick;
        this.forceState = forceState;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Time.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ConditionInput.require(args, 2, DoorCondition.localizer(), "<$syntax.openTime$> <$syntax.closeTime$> [$tabcomplete.forceState$]");
                    int open = parseTime(args.asString(0), "error.invalidOpenTime");
                    int close = parseTime(args.asString(1), "error.invalidCloseTime");
                    boolean force = args.asBoolean(2, false);
                    conditionBag.accept(new Time(open, close, force));
                    messageSender.sendMessage(sender, "setCondition.time",
                            Replacement.create("OPEN", Parser.parseTicksToTime(open)),
                            Replacement.create("CLOSE", Parser.parseTicksToTime(close)));
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.open") + ">");
                    }
                    if (args.sizeIs(2)) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.close") + ">");
                    }
                    if (args.sizeIs(3)) {
                        return Completion.completeBoolean(args.asString(2));
                    }
                    return Collections.emptyList();
                })
                .withMeta("time", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    /**
     * Parses a time of day from ticks or from {@code HH:mm}.
     *
     * @param input      time as entered
     * @param invalidKey message key when the input is no time
     * @return time of day in ticks
     * @throws CommandException when the input is no time or out of range
     */
    private static int parseTime(String input, String invalidKey) throws CommandException {
        Optional<Integer> ticks = Parser.parseInt(input);
        int time;
        if (ticks.isPresent()) {
            time = ticks.get();
        } else {
            time = Parser.parseTimeToTicks(input).orElseThrow(() -> CommandException.message(invalidKey));
        }
        if (time < 0 || time > TICKS_PER_DAY) {
            throw CommandException.message("error.invalidRange", Replacement.create("MIN", 0), Replacement.create("MAX", TICKS_PER_DAY));
        }
        return time;
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return STATE_CACHE.get(door.doorUID(), () -> Optional.ofNullable(shouldBeOpen(world.getFullTime()))).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.time", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.open") + " ", C.baseColor))
                .append(Component.text(Parser.parseTicksToTime(openTick), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.close") + " ", C.baseColor))
                .append(Component.text(Parser.parseTicksToTime(closeTick), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.forceState") + " ", C.baseColor))
                .append(Component.text(Boolean.toString(forceState), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " time " + openTick + " " + closeTick + " " + forceState;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " time";
    }

    @Override
    public Time clone() {
        return new Time(openTick, closeTick, forceState);
    }

    /**
     * Decides whether the door should be open at a time.
     *
     * @param fulltime full time of the world
     * @return true to open, false to close, null to leave the door as it is
     */
    public Boolean shouldBeOpen(long fulltime) {
        boolean open = getDiff(fulltime, openTick) > getDiff(fulltime, closeTick);
        DoorState target = open ? DoorState.OPEN : DoorState.CLOSED;
        if (state == null || state != target || forceState) {
            state = target;
            return open;
        }
        return null;
    }

    private long getDiff(long fullTime, long nextTime) {
        long currentTime = fullTime % TICKS_PER_DAY;
        return currentTime > nextTime ? TICKS_PER_DAY - currentTime + nextTime : nextTime - currentTime;
    }
}

package de.eldoria.bigdoorsopener.conditions.standalone;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.DoorState;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A key which defines the door state by current world time.
 */
@SerializableAs("timeCondition")
public class Time implements DoorCondition {
    // We use a static cache here for all time conditions.
    // The time condition is not very likely to change out of a sudden so the refresh cycle does not need to be precisely correct.
    private static final Cache<Long, Optional<Boolean>> STATE_CACHE = C.getShortExpiringCache();
    /**
     * The ticks from when to door should be closed
     */
    private final int openTick;
    /**
     * The ticks from when the door should be open.
     */
    private final int closeTick;
    private final boolean forceState;
    private DoorState state = null;

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

    public Time(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        openTick = resolvingMap.getValue("openTick");
        closeTick = resolvingMap.getValue("closeTick");
        forceState = resolvingMap.getValue("forceState");
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Proximity.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    Localizer localizer = BigDoorsOpener.localizer();
                    if (argumentsInvalid(player, messageSender, localizer, arguments, 2,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("syntax.openTime") + "> <"
                                    + localizer.getMessage("syntax.closeTime") + "> ["
                                    + localizer.getMessage("tabcomplete.forceState") + "]")) {
                        return;
                    }

                    // parse time
                    OptionalInt open = Parser.parseInt(arguments[0]);
                    if (!open.isPresent()) {
                        open = Parser.parseTimeToTicks(arguments[0]);
                        if (!open.isPresent()) {
                            messageSender.sendError(player, localizer.getMessage("error.invalidOpenTime"));
                            return;
                        }
                    }

                    OptionalInt close = Parser.parseInt(arguments[1]);
                    if (!close.isPresent()) {
                        close = Parser.parseTimeToTicks(arguments[1]);
                        if (!close.isPresent()) {
                            messageSender.sendError(player, localizer.getMessage("error.invalidCloseTime"));
                            return;
                        }
                    }

                    if (close.getAsInt() < 0 || close.getAsInt() > 24000
                            || open.getAsInt() < 0 || open.getAsInt() > 24000) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                                Replacement.create("MIN", 0).addFormatting('6'),
                                Replacement.create("MAX", 24000).addFormatting('6')));
                        return;
                    }

                    // parse optional force argument.
                    Optional<Boolean> force = ArgumentUtils.getOptionalParameter(arguments, 2, Optional.of(false), Parser::parseBoolean);

                    if (!force.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                        return;
                    }
                    conditionBag.putCondition(new Time(open.getAsInt(), close.getAsInt(), force.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.time",
                            Replacement.create("OPEN", Parser.parseTicksToTime(open.getAsInt())),
                            Replacement.create("CLOSE", Parser.parseTicksToTime(close.getAsInt()))));

                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.length == 1) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.open") + ">");
                    }
                    if (args.length == 2) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.close") + ">");
                    }
                    if (args.length == 3) {
                        if (args[2].isEmpty()) {
                            return Arrays.asList("true", "false");
                        }
                        return Arrays.asList("[" + localizer.getMessage("tabcomplete.forceState") + "]", "true", "false");
                    }
                    return Collections.emptyList();
                })
                .withMeta("time", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, @Nullable ConditionalDoor door, boolean currentState) {
        try {
            return STATE_CACHE.get(door.getDoorUID(), () -> Optional.ofNullable(shouldBeOpen(world.getFullTime()))).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.time",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined")))).color(TextColors.AQUA)
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

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " time " + openTick + " " + closeTick + " " + forceState;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " time";
    }

    @Override
    public void evaluated() {

    }

    @Override
    public Time clone() {
        return new Time(openTick, closeTick, forceState);
    }

    public Boolean shouldBeOpen(long fulltime) {
        long openInTicks = getDiff(fulltime, openTick);
        long closedInTicks = getDiff(fulltime, closeTick);
        // check if door should be open
        if (openInTicks > closedInTicks) {
            // attemt to open door
            if (state == null || state == DoorState.CLOSED) {
                state = DoorState.OPEN;
                return true;
            } else if (forceState) {
                state = DoorState.OPEN;
                return true;
            }
        } else {
            // attemt to close door
            if (state == null || state == DoorState.OPEN) {
                state = DoorState.CLOSED;
                return false;
            } else if (forceState) {
                state = DoorState.CLOSED;
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


}

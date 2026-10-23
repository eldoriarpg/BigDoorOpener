/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone.weather;

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
import de.eldoria.eldoutilities.utils.EnumUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Opens the door on downfall or on clear weather at the door.
 * <p>
 * Without {@code forceState} the door is only opened or closed when the weather changes, so it can still be toggled by
 * hand in between. With it the door is kept in the state the weather asks for.
 */
@JsonTypeName("weatherCondition")
public class Weather implements DoorCondition {
    private static final Cache<ConditionalDoor, Optional<Boolean>> STATE_CACHE = C.getShortExpiringCache();
    private static final double RAIN_TEMPERATURE = 0.95;
    private static final double TEMPERATURE_PER_BLOCK = 0.0016;
    private final WeatherType weatherType;
    private final boolean forceState;

    @JsonIgnore
    private DoorState state;

    @JsonCreator
    public Weather(
            @JsonProperty("weatherType") WeatherType weatherType, @JsonProperty("forceState") boolean forceState) {
        this.weatherType = weatherType == null ? WeatherType.DOWNFALL : weatherType;
        this.forceState = forceState;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Weather.class, Scope.WORLD)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ILocalizer localizer = DoorCondition.localizer();
                    ConditionInput.require(args, 1, localizer, "<$syntax.weatherType$> [$tabcomplete.forceState$]");
                    WeatherType weatherType = EnumUtil.parse(args.asString(0), WeatherType.class)
                            .orElseThrow(() -> CommandException.message("error.invalidWeatherType"));
                    boolean forceWeather = args.asBoolean(1, false);
                    conditionBag.accept(new Weather(weatherType, forceWeather));
                    messageSender.sendMessage(
                            sender,
                            "setCondition.weather",
                            Replacement.create("OPEN", localizer.getMessage(weatherKey(weatherType))));
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Completion.complete(args.asString(0), WeatherType.class, true, false);
                    }
                    if (args.sizeIs(2)) {
                        return Completion.completeBoolean(args.asString(1));
                    }
                    return Collections.emptyList();
                })
                .withMeta("weather", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    private static String weatherKey(WeatherType weatherType) {
        return weatherType == WeatherType.CLEAR ? "conditionDesc.clear" : "conditionDesc.downfall";
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return STATE_CACHE
                    .get(door, () -> Optional.ofNullable(isRaining(door)))
                    .orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    private Boolean isRaining(ConditionalDoor door) {
        Vector pos = door.position();
        World world = Bukkit.getWorld(door.world());
        if (world == null) return null;

        boolean raining = WeatherListener.isRaining(world) && getTemperature(world, pos) <= RAIN_TEMPERATURE;
        boolean open = raining == (weatherType == WeatherType.DOWNFALL);
        DoorState target = open ? DoorState.OPEN : DoorState.CLOSED;

        if (state == null || state != target || forceState) {
            state = target;
            return open;
        }
        return null;
    }

    private double getTemperature(World world, Vector pos) {
        int seaLevel = world.getSeaLevel();
        double temperature = world.getTemperature(pos.getBlockX(), seaLevel, pos.getBlockZ());
        if (pos.getY() > seaLevel) {
            temperature -= (pos.getY() - seaLevel) * TEMPERATURE_PER_BLOCK;
        }
        return temperature;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.weather", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.openWhen") + " ", C.baseColor))
                .append(Component.text(localizer.getMessage(weatherKey(weatherType)), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.forceState") + " ", C.baseColor))
                .append(Component.text(Boolean.toString(forceState), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " weather "
                + weatherType.toString().toLowerCase() + " " + forceState;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " weather";
    }

    @Override
    public Weather clone() {
        return new Weather(weatherType, forceState);
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.standalone.weather;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.DoorState;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import de.eldoria.eldoutilities.utils.EnumUtil;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A condition which opens the door based on the current weather in the world.
 */
@SerializableAs("weatherCondition")
public class Weather implements DoorCondition {
    // We use a static cache here for all weather conditions.
    // The weather condition is not very likely to change out of a sudden so the refresh cycle does not need to be precisely correct.
    private static final Cache<ConditionalDoor, Optional<Boolean>> STATE_CACHE = C.getShortExpiringCache();
    private final WeatherType weatherType;
    private boolean forceState;
    private DoorState state;

    public Weather(WeatherType weatherType, boolean forceState) {
        this.weatherType = weatherType;
    }

    public Weather(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        weatherType = EnumUtil.parse(resolvingMap.getValue("weatherType"), WeatherType.class, WeatherType.DOWNFALL);
        forceState = resolvingMap.getValueOrDefault("forceState", false);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Weather.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("syntax.weatherType") + ">")) {
                        return;
                    }

                    WeatherType weatherType = null;
                    for (WeatherType value : WeatherType.values()) {
                        if (value.name().equalsIgnoreCase(arguments[0])) {
                            weatherType = value;
                        }
                    }
                    if (weatherType == null) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidWeatherType"));
                        return;
                    }

                    Optional<Boolean> forceWeather = ArgumentUtils.getOptionalParameter(arguments, 1, Optional.of(false), Parser::parseBoolean);

                    if (!forceWeather.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                        return;
                    }

                    conditionBag.accept(new Weather(weatherType, forceWeather.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.weather",
                            Replacement.create("OPEN", weatherType == WeatherType.CLEAR
                                    ? localizer.getMessage("conditionDesc.clear")
                                    : localizer.getMessage("conditionDesc.downfall"))));

                })
                .onTabComplete((sender, localizer, args) -> {
                    final String[] weatherType = Arrays.stream(WeatherType.values())
                            .map(v -> v.name().toLowerCase())
                            .toArray(String[]::new);

                    if (args.length == 1) {
                        return ArrayUtil.startingWithInArray(args[0], weatherType).collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                })
                .withMeta("weather", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return STATE_CACHE.get(door, () -> Optional.ofNullable(isRaining(door))).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    private Boolean isRaining(ConditionalDoor door) {
        Vector pos = door.position();

        World world = Bukkit.getWorld(door.world());
        boolean raining = WeatherListener.isRaining(world);


        // check if it can rain at door based on temperature.
        if (raining) {
            raining = getTemperature(world, pos) <= 0.95;
        }


        if (raining) {
            // attemt to open
            if (weatherType == WeatherType.DOWNFALL) {
                if (state == null || state == DoorState.CLOSED || forceState) {
                    state = DoorState.OPEN;
                    return true;
                }
            }

            // attemt to close
            if (weatherType == WeatherType.CLEAR) {
                if (state == null || state == DoorState.OPEN || forceState) {
                    state = DoorState.CLOSED;
                    return false;
                }
            }
        } else {
            // attemt to open
            if (weatherType == WeatherType.CLEAR) {
                if (state == null || state == DoorState.CLOSED || forceState) {
                    state = DoorState.OPEN;
                    return true;
                }
            }

            // attemt to close
            if (weatherType == WeatherType.DOWNFALL) {
                if (state == null || state == DoorState.OPEN || forceState) {
                    state = DoorState.CLOSED;
                    return false;
                }
            }
        }
        return null;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.weather",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.openWhen") + " ", C.baseColor))
                .append(Component.text(weatherType == WeatherType.CLEAR
                        ? localizer.getMessage("conditionDesc.clear")
                        : localizer.getMessage("conditionDesc.downfall"), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " weather " + weatherType.toString().toLowerCase();
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " weather";
    }

    @Override
    public void evaluated() {

    }

    @Override
    public Weather clone() {
        return new Weather(weatherType, forceState);
    }

    private double getTemperature(World world, Vector pos) {
        double temperature = world.getTemperature((int) pos.getX(), (int) pos.getZ());
        if (pos.getY() > world.getSeaLevel()) {
            temperature -= (pos.getY() - world.getSeaLevel()) * 0.0016;
        }
        return temperature;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("weatherType", weatherType)
                .add("forceState", forceState)
                .build();
    }

}

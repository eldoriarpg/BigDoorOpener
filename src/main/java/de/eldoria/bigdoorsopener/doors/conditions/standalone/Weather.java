package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.doors.conditions.DoorState;
import de.eldoria.bigdoorsopener.listener.WeatherListener;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.EnumUtil;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * A condition which opens the door based on the current weather in the world.
 */
@SerializableAs("weatherCondition")
public class Weather implements DoorCondition {
    // We use a static cache here for all weather conditions.
    // The weather condition is not very likely to change out of a sudden so the refresh cycle does not need to be precisely correct.
    private static final Cache<ConditionalDoor, Optional<Boolean>> STATE_CACHE = C.getExpiringCache();
    private final WeatherType weatherType;
    private boolean forceState;
    private DoorState state;

    public Weather(WeatherType weatherType, boolean forceState) {
        this.weatherType = weatherType;
    }

    public Weather(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        weatherType = EnumUtil.parse(resolvingMap.getValue("weatherType"), WeatherType.class);
        forceState = resolvingMap.getValueOrDefault("forceState", false);
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
        Vector pos = door.getPosition();

        World world = Bukkit.getWorld(door.getWorld());
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
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.weather",
                        Replacement.create("NAME", ConditionType.WEATHER.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.openWhen") + " ").color(C.baseColor))
                .append(TextComponent.builder(weatherType == WeatherType.CLEAR
                        ? localizer.getMessage("conditionDesc.clear")
                        : localizer.getMessage("conditionDesc.downfall")).color(C.highlightColor))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " weather " + weatherType.toString().toLowerCase();
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " weather";
    }

    @Override
    public void evaluated() {

    }

    @Override
    public Weather clone() {
        return new Weather(weatherType, forceState);
    }

    private double getTemperature(World world, Vector pos) {
        double temperature = world.getTemperature((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
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

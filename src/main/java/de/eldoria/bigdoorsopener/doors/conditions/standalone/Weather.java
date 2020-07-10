package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.listener.WeatherListener;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.text.TextComponent;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

public class Weather implements DoorCondition {
    private final WeatherType weatherType;

    public Weather(WeatherType weatherType) {
        this.weatherType = weatherType;
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        Vector pos = door.getPosition();

        boolean raining = WeatherListener.isRaining(world);

        // check if it can rain at door based on temperature.
        if (raining) {
            raining = getTemperature(world, pos) <= 95;
        }

        return raining ? weatherType == WeatherType.DOWNFALL : weatherType == WeatherType.CLEAR;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.weather",
                        Replacement.create("NAME", ConditionType.WEATHER.conditionName))).color(C.baseColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.open")).color(C.baseColor))
                .append(TextComponent.builder(weatherType == WeatherType.CLEAR
                        ? localizer.getMessage("conditionDesc.clear")
                        : localizer.getMessage("conditionDesc.downfall")).color(C.highlightColor))
                .build();
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
                .build();
    }

    public static Weather deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        WeatherType type = resolvingMap.getValue("weatherType");
        return new Weather(type);
    }
}

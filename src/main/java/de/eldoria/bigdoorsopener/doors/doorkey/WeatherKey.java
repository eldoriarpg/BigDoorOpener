package de.eldoria.bigdoorsopener.doors.doorkey;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.listener.WeatherListener;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

@KeyParameter("weatherKey")
public class WeatherKey implements DoorKey {
    WeatherType type;

    public WeatherKey(WeatherType type) {
        this.type = type;
    }

    @Override
    public boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        Vector pos = door.getPosition();

        boolean raining = WeatherListener.isRaining(world);

        if (raining) {
            raining = getTemperature(world, pos) <= 95;
        }

        if (!WeatherListener.isRaining(world) || type == WeatherType.CLEAR) return true;
        boolean canRain = getTemperature(world, pos) <= 0.95;
        if (!canRain) {
            return type == WeatherType.CLEAR;
        }

        return raining ? type == WeatherType.DOWNFALL : type == WeatherType.CLEAR;
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
        return null;
    }
}

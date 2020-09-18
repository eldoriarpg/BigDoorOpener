package de.eldoria.bigdoorsopener.listener;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This listener allows to check the current weather in a world.
 * For some reasons spigot does not know the current weather of a world... wtf.
 */
public class WeatherListener implements Listener {
    private static final Map<UUID, Boolean> WEATHER_MAP = new HashMap<>();

    /**
     * Get if its raining in a world.
     * Is only really correct if the weather changed after plugin initialization.
     *
     * @param world world to check
     * @return true if its currently raining in the world
     */
    public static boolean isRaining(World world) {
        return WEATHER_MAP.computeIfAbsent(world.getUID(), (k) -> world.hasStorm());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        WEATHER_MAP.put(event.getWorld().getUID(), event.toWeatherState());
    }
}

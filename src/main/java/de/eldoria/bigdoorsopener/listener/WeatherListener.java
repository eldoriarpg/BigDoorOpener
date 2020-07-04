package de.eldoria.bigdoorsopener.listener;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WeatherListener implements Listener {
    private static Map<UUID, Boolean> weatherMap = new HashMap<>();

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        weatherMap.put(event.getWorld().getUID(), event.toWeatherState());
    }

    public static boolean isRaining(World world) {
        return weatherMap.computeIfAbsent(world.getUID(), (k) -> world.hasStorm());
    }
}

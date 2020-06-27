package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Config {
    private final Plugin plugin;
    private final Map<Long, TimedDoor> doors = new HashMap<>();
    private int approachRefreshRate;
    private int timedRefreshRate;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {
        reloadConfig();
    }

    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        setIfAbsent(config, "doors", new ArrayList<TimedDoor>());
        List<TimedDoor> configDoors = (List<TimedDoor>) config.getList("doors");
        if (configDoors != null) {
            doors.clear();
            for (TimedDoor door : configDoors) {
                BigDoorsOpener.logger().info("Registered door " + door.getDoorUID());
                doors.put(door.getDoorUID(), door);
            }
        } else {
            BigDoorsOpener.logger().info("No doors defined.");
        }

        setIfAbsent(config, "approachRefreshRate", 20);
        setIfAbsent(config, "timedRefreshRate", 20);

        approachRefreshRate = config.getInt("approachRefreshRate", 20);
        timedRefreshRate = config.getInt("timedRefreshRate", 20);

        BigDoorsOpener.logger().info("Config reloaded!");
    }

    public void safeConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("doors", new ArrayList<>(doors.values()));
        plugin.saveConfig();
    }

    private static void setIfAbsent(FileConfiguration config, String path, Object value) {
        if (!config.isSet(path)) {
            config.set(path, value);
        }
    }

    private static void setIfAbsent(ConfigurationSection section, String path, Object value) {
        if (!section.isSet(path)) {
            section.set(path, value);
        }
    }

    private static ConfigurationSection createSectionIfAbsent(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return config.createSection(path);
        }
        return section;
    }

}

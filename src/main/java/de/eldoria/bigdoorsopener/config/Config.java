package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyChain;
import de.eldoria.bigdoorsopener.doors.doorkey.PermissionKey;
import de.eldoria.bigdoorsopener.doors.doorkey.TimeKey;
import de.eldoria.bigdoorsopener.doors.doorkey.location.ProximityKey;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Config {
    private final Plugin plugin;
    private final Map<Long, ConditionalDoor> doors = new HashMap<>();
    private int approachRefreshRate;
    private int timedRefreshRate;
    private boolean enableMetrics;
    private String language;
    private int refreshRate;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {
        updateConfig();
        forceConfigConsitency();
        reloadConfig();
    }

    private void updateConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        if (!config.contains("version")) {
            plugin.getLogger().info("Config has no version key.");
            plugin.getLogger().info("Detected config version 0. Performing migration to version 1.");

            updateVersion0();
        }
        int version = config.getInt("version");
        switch (version) {
            case 1:
                plugin.getLogger().info("Config is up to date.");
                return;
        }
    }

    private void updateVersion0() {

        FileConfiguration config = plugin.getConfig();
        // set new config version
        config.set("version", 1);

        // Convert TimedDoor to ConditionDoor
        List<TimedDoor> timedDoors = (List<TimedDoor>) config.getList("doors");

        if (timedDoors != null) {
            List<ConditionalDoor> conditionalDoors = new ArrayList<>();

            for (TimedDoor tD : timedDoors) {
                ConditionalDoor cD = new ConditionalDoor(tD.getDoorUID(), tD.getWorld(), tD.getPosition());

                KeyChain keyChain = cD.getKeyChain();

                if (tD.getPermission() != null || tD.getPermission().isEmpty()) {
                    keyChain.setPermissionKey(new PermissionKey(tD.getPermission()));
                }

                if (!tD.isPermanentlyClosed()) {
                    keyChain.setTimeKey(new TimeKey(tD.getTicksOpen(), tD.getTicksClose(), false));
                }

                if (tD.getOpenRange() > 0) {
                    keyChain.setLocationKey(
                            new ProximityKey(
                                    new Vector(tD.getOpenRange(), tD.getOpenRange(), tD.getOpenRange()),
                                    ProximityKey.ProximityForm.ELIPSOID));
                }
            }
            config.set("doors", conditionalDoors);
        }

        setIfAbsent(config, "refreshRate", 20);

        config.set("approachRefreshRate", null);
        config.set("timedRefreshRate", null);

        plugin.saveConfig();
        plugin.getLogger().info("Config migration to version 1 completed.");
    }

    /**
     * Forces the current actual config values.
     * Must be always executed after {@link #updateConfig()}
     */
    private void forceConfigConsitency() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        setIfAbsent(config, "doors", new ArrayList<ConditionalDoor>());

        setIfAbsent(config, "refreshRate", 20);
        setIfAbsent(config, "enableMetrics", true);
        setIfAbsent(config, "language", "en_US");
    }

    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        setIfAbsent(config, "doors", new ArrayList<ConditionalDoor>());
        List<ConditionalDoor> configDoors = (List<ConditionalDoor>) config.getList("doors");
        if (configDoors != null) {
            doors.clear();
            for (ConditionalDoor door : configDoors) {
                doors.put(door.getDoorUID(), door);
            }
        } else {
            BigDoorsOpener.logger().info("No doors defined.");
        }

        refreshRate = config.getInt("refreshRate", 20);
        enableMetrics = config.getBoolean("enableMetrics", true);
        language = config.getString("language", "en_US");

        BigDoorsOpener.logger().info("Config loaded!");
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

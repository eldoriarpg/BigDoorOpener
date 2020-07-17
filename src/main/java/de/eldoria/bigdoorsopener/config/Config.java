package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionChain;
import de.eldoria.bigdoorsopener.doors.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Getter
public class Config {
    private final Plugin plugin;
    private final Map<Long, ConditionalDoor> doors = new HashMap<>();
    private int approachRefreshRate;
    private int timedRefreshRate;
    private boolean enableMetrics;
    private String language;
    private int refreshRate;
    private boolean checkUpdates;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        updateConfig();
        reloadConfig();
    }

    private void updateConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        if (!config.isSet("version")) {
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

    /**
     * Updates the config from version 0 to version 1.
     * Version 0 is identified by a missing version key.
     */
    @SuppressWarnings("unchecked")
    private void updateVersion0() {
        FileConfiguration config = plugin.getConfig();
        // set new config version
        config.set("version", 1);

        // Convert TimedDoor to ConditionDoor
        List<TimedDoor> timedDoors = (List<TimedDoor>) config.getList("doors");

        Logger log = BigDoorsOpener.logger();
        if (timedDoors != null) {
            log.info("Trying to convert " + timedDoors.size() + " timed door(s).");
            List<ConditionalDoor> conditionalDoors = new ArrayList<>();


            for (TimedDoor tD : timedDoors) {
                log.info("Converting door " + tD.getDoorUID());
                ConditionalDoor cD = new ConditionalDoor(tD.getDoorUID(), tD.getWorld(), tD.getPosition());

                ConditionChain conditionChain = cD.getConditionChain();

                if (tD.getPermission() != null && !tD.getPermission().isEmpty()) {
                    conditionChain.setPermission(new Permission(tD.getPermission()));
                    log.info("Adding permission condition.");
                }

                if (!tD.isPermanentlyClosed()) {
                    conditionChain.setTime(new Time(tD.getTicksOpen(), tD.getTicksClose(), false));
                    log.info("Adding time condition.");
                }

                if (tD.getOpenRange() > 0) {
                    conditionChain.setLocation(
                            new Proximity(
                                    new Vector(tD.getOpenRange(), tD.getOpenRange(), tD.getOpenRange()),
                                    Proximity.ProximityForm.ELLIPSOID));
                    log.info("Adding proximity condition.");
                }
                log.info("Door " + tD.getDoorUID() + " successfully converted.");
                conditionalDoors.add(cD);
            }
            config.set("doors", conditionalDoors);
        } else {
            log.info("No doors defined. skipping doors conversation.");
        }

        // set added keys
        config.set("refreshRate", 20);
        config.set("checkUpdates", true);

        // remove no longer used keys
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
        setIfAbsent(config, "checkUpdates", true);

        // never set the version here.
    }

    /**
     * Discards any internal changes to the config and loads it.
     * Ensures config consistency.
     */
    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        forceConfigConsitency();
        FileConfiguration config = plugin.getConfig();

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
        checkUpdates = config.getBoolean("checkUpdates", true);

        BigDoorsOpener.logger().info("Config loaded!");
    }

    /**
     * Save the currently defined doors with current state to config.
     */
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

package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.conditions.permission.PermissionNode;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

public class Config {
    private final Plugin plugin;
    private final Map<Long, ConditionalDoor> doors = new HashMap<>();
    private int approachRefreshRate;
    private int timedRefreshRate;
    private boolean enableMetrics;
    private String language;
    private int refreshRate;
    private boolean checkUpdates;
    private int jsCacheSize;
    private Vector playerCheckRadius;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
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
     * Updates the config from version 0 to version 1. Version 0 is identified by a missing version key.
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
                log.info("Converting door " + tD.doorUID());
                ConditionalDoor cD = new ConditionalDoor(tD.doorUID(), tD.world(), tD.position());

                ConditionBag conditionBag = cD.conditionBag();

                if (tD.permission() != null && !tD.permission().isEmpty()) {
                    conditionBag.setCondition(new PermissionNode(tD.permission()));
                    log.info("Adding permission condition.");
                }

                if (!tD.isPermanentlyClosed()) {
                    conditionBag.setCondition(new Time(tD.ticksOpen(), tD.ticksClose(), false));
                    log.info("Adding time condition.");
                }

                if (tD.openRange() > 0) {
                    conditionBag.setCondition(
                            new Proximity(
                                    new Vector(tD.openRange(), tD.openRange(), tD.openRange()),
                                    Proximity.ProximityForm.ELLIPSOID));
                    log.info("Adding proximity condition.");
                }
                log.info("Door " + tD.doorUID() + " successfully converted.");
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
     * Forces the current actual config values. Must be always executed after {@link #updateConfig()}
     */
    private void forceConfigConsistency() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        setIfAbsent(config, "doors", new ArrayList<ConditionalDoor>());

        setIfAbsent(config, "refreshRate", 20);
        setIfAbsent(config, "enableMetrics", true);
        setIfAbsent(config, "language", "en_US");
        setIfAbsent(config, "checkUpdates", true);
        setIfAbsent(config, "jsCacheSize", 400);
        setIfAbsent(config, "playerCheckRadius", 200);


        // never set the version here.
    }

    /**
     * Discards any internal changes to the config and loads it. Ensures config consistency.
     */
    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        forceConfigConsistency();
        FileConfiguration config = plugin.getConfig();

        List<ConditionalDoor> configDoors = (List<ConditionalDoor>) config.getList("doors");
        if (configDoors != null) {
            doors.clear();
            for (ConditionalDoor door : configDoors) {
                doors.put(door.doorUID(), door);
            }
        } else {
            BigDoorsOpener.logger().info("No doors defined.");
        }

        refreshRate = config.getInt("refreshRate", 20);
        enableMetrics = config.getBoolean("enableMetrics", true);
        language = config.getString("language", "en_US");
        checkUpdates = config.getBoolean("checkUpdates", true);
        jsCacheSize = config.getInt("jsCacheSize", 400);
        int radius = config.getInt("playerCheckRadius", 200);
        playerCheckRadius = new Vector(radius, radius, radius);

        // ensure that js cache size is not too small
        if (jsCacheSize < 200) {
            BigDoorsOpener.logger().warning("Js cache is small. This may cause performance issues. We recommend at least a size of 200");
            jsCacheSize = Math.max(jsCacheSize, 10);
        }

        safeConfig();

        BigDoorsOpener.logger().info("Config loaded!");
    }

    /**
     * Save the currently defined doors with current state to config.
     */
    public void safeConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("doors", new ArrayList<>(doors.values()));
        plugin.saveConfig();
        BigDoorsOpener.logger().fine("Config saved.");
    }

    public ConditionalDoor getDoor(Long key) {
        return doors.get(key);
    }

    public Set<Long> keySet() {
        return doors.keySet();
    }

    public Collection<ConditionalDoor> getDoors() {
        return doors.values();
    }

    public Map<Long, ConditionalDoor> getDoorMap() {
        return doors;
    }

    public ConditionalDoor putDoorIfAbsent(Long key, ConditionalDoor value) {
        ConditionalDoor conditionalDoor = doors.putIfAbsent(key, value);
        Bukkit.getPluginManager().callEvent(new DoorUnregisteredEvent(conditionalDoor));
        return conditionalDoor;
    }

    public boolean removeDoor(Long key) {
        ConditionalDoor remove = doors.remove(key);
        safeConfig();
        Bukkit.getPluginManager().callEvent(new DoorUnregisteredEvent(remove));
        return remove != null;
    }

    public ConditionalDoor computeDoorIfAbsent(Long key, @NotNull Function<? super Long, ? extends ConditionalDoor> mappingFunction) {
        if (doors.containsKey(key)) {
            return doors.get(key);
        }
        ConditionalDoor conditionalDoor = doors.computeIfAbsent(key, mappingFunction);
        Bukkit.getPluginManager().callEvent(new DoorRegisteredEvent(conditionalDoor));
        return conditionalDoor;
    }

    public boolean containsDoor(long key) {
        return doors.containsKey(key);
    }

    public int approachRefreshRate() {
        return approachRefreshRate;
    }

    public int timedRefreshRate() {
        return timedRefreshRate;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public String language() {
        return language;
    }

    public int refreshRate() {
        return refreshRate;
    }

    public boolean isCheckUpdates() {
        return checkUpdates;
    }

    public int jsCacheSize() {
        return jsCacheSize;
    }

    public Vector playerCheckRadius() {
        return playerCheckRadius;
    }
}

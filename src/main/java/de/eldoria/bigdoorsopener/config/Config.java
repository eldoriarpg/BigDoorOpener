/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.config.elements.Settings;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Settings and doors of the plugin.
 * <p>
 * The settings are read from {@code config.yml}, the doors from {@code doors.yml}. A configuration written by 2.x is
 * migrated on the first load, see {@link LegacyDoorMigration}.
 */
public class Config {
    private static final int MIN_JS_CACHE_SIZE = 10;
    private static final int RECOMMENDED_JS_CACHE_SIZE = 200;
    private final Plugin plugin;
    private final JacksonConfiguration configuration;

    public Config(Plugin plugin, JacksonConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        migrate();
        reloadConfig();
    }

    private void migrate() {
        try {
            if (LegacyDoorMigration.migrate(plugin.getDataFolder().toPath(), plugin.getLogger())) {
                configuration.main().version(Settings.CURRENT_VERSION);
                configuration.save();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not migrate the configuration of Big Doors Opener 2.x", e);
        }
    }

    /**
     * Discards any internal changes and loads the settings and doors from their files.
     */
    public void reloadConfig() {
        configuration.reload();
        configuration.save();
        if (jsCacheSize() < RECOMMENDED_JS_CACHE_SIZE) {
            BigDoorsOpener.logger()
                    .warning("Js cache is small. This may cause performance issues. We recommend at least a size of "
                            + RECOMMENDED_JS_CACHE_SIZE);
        }
        BigDoorsOpener.logger().info("Config loaded!");
    }

    /**
     * Saves the doors with their current state.
     */
    public void saveDoors() {
        configuration.save(JacksonConfiguration.DOORS);
        BigDoorsOpener.logger().fine("Doors saved.");
    }

    /**
     * Saves the settings and doors, for example after the update settings changed.
     */
    public void save() {
        configuration.save();
    }

    public Settings settings() {
        return configuration.main();
    }

    public ConditionalDoor getDoor(Long key) {
        return doors().get(key);
    }

    public Set<Long> keySet() {
        return doors().keySet();
    }

    public Collection<ConditionalDoor> getDoors() {
        return doors().values();
    }

    public Map<Long, ConditionalDoor> getDoorMap() {
        return doors();
    }

    /**
     * Registers a door unless a door with this id is registered.
     *
     * @param key   id of the door
     * @param value door to register
     * @return the door registered before, or null if the door was registered now
     */
    public ConditionalDoor putDoorIfAbsent(Long key, ConditionalDoor value) {
        ConditionalDoor conditionalDoor = doors().putIfAbsent(key, value);
        if (conditionalDoor == null) {
            Bukkit.getPluginManager().callEvent(new DoorRegisteredEvent(value));
        }
        return conditionalDoor;
    }

    public boolean removeDoor(Long key) {
        ConditionalDoor remove = doors().remove(key);
        saveDoors();
        Bukkit.getPluginManager().callEvent(new DoorUnregisteredEvent(remove));
        return remove != null;
    }

    public ConditionalDoor computeDoorIfAbsent(
            Long key, @NotNull Function<? super Long, ? extends ConditionalDoor> mappingFunction) {
        if (doors().containsKey(key)) {
            return doors().get(key);
        }
        ConditionalDoor conditionalDoor = doors().computeIfAbsent(key, mappingFunction);
        Bukkit.getPluginManager().callEvent(new DoorRegisteredEvent(conditionalDoor));
        return conditionalDoor;
    }

    public boolean containsDoor(long key) {
        return doors().containsKey(key);
    }

    public boolean isEnableMetrics() {
        return settings().enableMetrics();
    }

    public String language() {
        return settings().language();
    }

    public int refreshRate() {
        return settings().refreshRate();
    }

    public int jsCacheSize() {
        return Math.max(settings().jsCacheSize(), MIN_JS_CACHE_SIZE);
    }

    public Vector playerCheckRadius() {
        int radius = settings().playerCheckRadius();
        return new Vector(radius, radius, radius);
    }

    private Map<Long, ConditionalDoor> doors() {
        return configuration.doors().byId();
    }
}

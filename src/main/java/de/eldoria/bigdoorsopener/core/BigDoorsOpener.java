/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.commands.BDOCommand;
import de.eldoria.bigdoorsopener.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemConditionListener;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.conditions.location.Region;
import de.eldoria.bigdoorsopener.conditions.location.SimpleRegion;
import de.eldoria.bigdoorsopener.conditions.permission.DoorPermission;
import de.eldoria.bigdoorsopener.conditions.permission.PermissionNode;
import de.eldoria.bigdoorsopener.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs.MythicMob;
import de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs.MythicMobsListener;
import de.eldoria.bigdoorsopener.conditions.standalone.weather.Weather;
import de.eldoria.bigdoorsopener.conditions.standalone.weather.WeatherListener;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldProximity;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldRegion;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldSimpleRegion;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.JacksonConfiguration;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.exceptions.PluginInitFailed;
import de.eldoria.bigdoorsopener.core.listener.DoorOpenedListener;
import de.eldoria.bigdoorsopener.core.listener.ModificationListener;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.core.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.core.updates.Updates;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.eldoutilities.config.template.PluginBaseConfiguration;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.plugin.EldoPlugin;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin entry point.
 * <p>
 * The conditions, localizer and message sender are set up on enable. Everything which needs Big Doors and the
 * optional plugins (the configuration, the door checker, listeners and commands) is set up after the server started.
 */
public class BigDoorsOpener extends EldoPlugin {
    private static final Pattern BIG_DOORS_VERSION = Pattern.compile("([0-9]\\.(?:[0-9]\\.?)+)");
    private static final Pattern BIG_DOORS_BUILD = Pattern.compile("\\((b[0-9]+)\\)");

    private static CachingJSEngine JS;
    private static boolean placeholderEnabled = false;
    private static boolean mythicMobsEnabled;
    private static RegionContainer regionContainer = null;
    private static BigDoorsOpener instance;
    private final JacksonConfiguration configuration = new JacksonConfiguration(this);
    private final Updates updates = new Updates(this, this::getDoorsConfig);
    private Config config;
    private ILocalizer localizer;
    private BigDoors doors;
    private DoorChecker doorChecker;
    private ItemConditionListener itemConditionListener;

    public static RegionContainer regionContainer() {
        return regionContainer;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    public static CachingJSEngine JS() {
        return JS;
    }

    public static boolean isPlaceholderEnabled() {
        return placeholderEnabled;
    }

    public static boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }

    public static BigDoors getBigDoors() {
        return instance.doors;
    }

    public static ILocalizer localizer() {
        return instance.localizer;
    }

    public static MessageSender getPluginMessageSender() {
        return MessageSender.getPluginMessageSender(BigDoorsOpener.class);
    }

    public static BigDoorsOpener instance() {
        return instance;
    }

    @Override
    public Level getLogLevel() {
        return configuration.secondary(PluginBaseConfiguration.KEY).logLevel();
    }

    @Override
    public void onPluginEnable() {
        instance = this;
        registerConditions();
        localizer = Localizer.builder(this, "en_US")
                .setIncludedLocales("en_US", "de_DE")
                .build();
        MessageSender.builder(this)
                .prefix("<gold>[BDO]")
                .localizer(localizer)
                .register();
    }

    @Override
    public void onPostStart() {
        loadExternalSources();
        lateInitThirdPartyAPIs();

        config = new Config(this, configuration);
        localizer.setLocale(config.language());
        JS = new CachingJSEngine(config.jsCacheSize());

        updates.apply();
        enableMetrics();

        doorChecker = new DoorChecker(config, doors);
        scheduleRepeatingTask(doorChecker, 100, 1);

        registerListeners();
        registerCommand(new BDOCommand(this, doors, config));
    }

    @Override
    public void onPluginDisable() {
        updates.stop();
        if (config != null) {
            config.save();
        }
    }

    private void registerListeners() {
        itemConditionListener = new ItemConditionListener(config);
        registerListener(new ModificationListener(config), doorChecker, new DoorOpenedListener(config),
                itemConditionListener, new RegisterInteraction(getPluginMessageSender(), config), new WeatherListener());
        if (mythicMobsEnabled) {
            registerListener(new MythicMobsListener(doors, config));
        }
    }

    private void loadExternalSources() throws PluginInitFailed {
        if (!getPluginManager().isPluginEnabled("BigDoors")) {
            getPluginManager().disablePlugin(this);
            throw new PluginInitFailed("Big Doors is not enabled");
        }

        doors = (BigDoors) getPluginManager().getPlugin("BigDoors");

        if (doors.getCommander() == null) {
            throw new PluginInitFailed("Big Doors is not ready or not loaded properly");
        }
        logger().info("Hooked into Big Doors successfully.");
    }

    private void lateInitThirdPartyAPIs() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (regionContainer != null) {
                logger().info("Hooked into world guard successfully.");
            } else {
                logger().warning("Failed to hook into world guard.");
            }
        } else {
            logger().info("World guard not found. Region conditions can't be used.");
        }

        placeholderEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        logger().info(placeholderEnabled
                ? "Placeholder API found. Enabling placeholder usage."
                : "Placeholder API not found. Placeholder usage is disabled.");

        mythicMobsEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        logger().info(mythicMobsEnabled
                ? "MythicMobs found. Enabling mythic mobs listener."
                : "MythicMobs not found. MythicMobs conditions are disabled.");
    }

    private void registerConditions() {
        ConditionRegistrar.registerCondition(ItemBlock.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemClick.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemHolding.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemOwning.getConditionContainer());
        ConditionRegistrar.registerCondition(Proximity.getConditionContainer());
        ConditionRegistrar.registerCondition(Region.getConditionContainer());
        ConditionRegistrar.registerCondition(SimpleRegion.getConditionContainer());
        ConditionRegistrar.registerCondition(WorldProximity.getConditionContainer());
        ConditionRegistrar.registerCondition(WorldRegion.getConditionContainer());
        ConditionRegistrar.registerCondition(WorldSimpleRegion.getConditionContainer());
        ConditionRegistrar.registerCondition(PermissionNode.getConditionContainer());
        ConditionRegistrar.registerCondition(DoorPermission.getConditionContainer());
        ConditionRegistrar.registerCondition(Time.getConditionContainer());
        ConditionRegistrar.registerCondition(Weather.getConditionContainer());
        ConditionRegistrar.registerCondition(Placeholder.getConditionContainer());
        ConditionRegistrar.registerCondition(MythicMob.getConditionContainer());
    }

    private void enableMetrics() {
        if (!config.isEnableMetrics()) return;

        Metrics metrics = new Metrics(this, 8015);

        logger().info(localizer.getMessage("general.metrics"));

        metrics.addCustomChart(new SimplePie("big_doors_version", () -> doors.getDescription().getVersion()));

        metrics.addCustomChart(new DrilldownPie("big_doors_version_new", () -> {
            String version = doors.getDescription().getVersion();
            return Map.of(bigDoorsVersion(version), Map.of(bigDoorsBuild(version), 1));
        }));

        metrics.addCustomChart(new AdvancedPie("condition_types", () -> {
            Map<String, Integer> counts = new HashMap<>();
            Collection<ConditionalDoor> values = config.getDoors();
            for (String group : ConditionRegistrar.getGroups()) {
                ConditionRegistrar.getConditionGroup(group).ifPresent(g ->
                        counts.put(group, (int) values.stream().filter(d -> d.conditionBag().isConditionSet(g)).count()));
            }
            return counts;
        }));
    }

    private static String bigDoorsVersion(String version) {
        Matcher matcher = BIG_DOORS_VERSION.matcher(version);
        return matcher.find() ? matcher.group(1) : "undefined";
    }

    private static String bigDoorsBuild(String version) {
        Matcher matcher = BIG_DOORS_BUILD.matcher(version);
        return matcher.find() ? matcher.group(1) : "release";
    }

    /**
     * Returns the configuration for the doors.
     * <p>
     * The config is loaded after the server ticked for the first time.
     *
     * @return The configuration
     */
    @NotNull
    public Config getDoorsConfig() {
        return Objects.requireNonNull(config, "Do not call before server is fully initialized.");
    }

    /**
     * Reloads the configuration, the update settings and the door checker.
     */
    public void reload() {
        config.reloadConfig();
        localizer.setLocale(config.language());
        updates.apply();
        doorChecker.reload();
        itemConditionListener.rebuild();
    }
}

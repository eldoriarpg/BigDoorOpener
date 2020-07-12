package de.eldoria.bigdoorsopener;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.commands.BigDoorsOpenerCommand;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionChain;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.doors.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.doors.conditions.location.Region;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.listener.ItemConditionListener;
import de.eldoria.bigdoorsopener.listener.WeatherListener;
import de.eldoria.bigdoorsopener.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.updater.UpdateChecker;
import lombok.Getter;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigDoorsOpener extends JavaPlugin {

    private static Logger logger;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private Config config;
    private boolean initialized;
    private Localizer localizer;
    private static CachingJSEngine JS;


    // External instances.
    private BigDoors doors;
    @Getter
    private static RegionContainer regionContainer = null;
    private Commander commander;

    // scheduler
    private DoorChecker doorChecker;

    // listener
    private WeatherListener weatherListener;
    private RegisterInteraction registerInteraction;

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        PluginManager pm = Bukkit.getPluginManager();
        if (!initialized) {
            logger = this.getLogger();
            JS = new CachingJSEngine(200);

            buildSerializer();

            if (!pm.isPluginEnabled("BigDoors")) {
                logger().warning("Big Doors is disabled.");
                pm.disablePlugin(this);
                return;
            }

            // Load external resources before world guard.
            loadExternalSources();

            // create config
            config = new Config(this);


            // Check for updates
            if (config.isCheckUpdates()) {
                UpdateChecker.performAndNotifyUpdateCheck(this, 80805, true);
            }


            localizer = new Localizer(this, config.getLanguage(), "messages",
                    "messages", Locale.US, "de_DE", "en_US");

            //enable metrics
            enableMetrics();

            registerListener();


            MessageSender.create(this, "§6[BDO] ", '2', 'c');

            // start door checker
            doorChecker = new DoorChecker(config, doors, localizer);
            scheduler.scheduleSyncRepeatingTask(this, doorChecker, 100, 1);

            getCommand("bigdoorsopener")
                    .setExecutor(new BigDoorsOpenerCommand(this, commander, config, localizer, doorChecker, registerInteraction));
        }

        if (initialized) {
            localizer.setLocale(config.getLanguage());
            scheduler.cancelTasks(this);
            doorChecker.reload();
        }
        initialized = true;
    }

    private void registerListener() {
        weatherListener = new WeatherListener();
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(weatherListener, this);
        registerInteraction = new RegisterInteraction();
        pm.registerEvents(registerInteraction, this);
        pm.registerEvents(new ItemConditionListener(config), this);
    }

    private void loadExternalSources() {
        PluginManager pm = Bukkit.getPluginManager();

        Plugin bigDoorsPlugin = pm.getPlugin("BigDoors");
        doors = (BigDoors) bigDoorsPlugin;
        commander = doors.getCommander();

        // check if world guard is loaded
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            logger().info("World Guard found. Trying to get a hook.");
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (regionContainer != null) {
                logger().info("Hooked into world guard successfully.");
            } else {
                logger().warning("Failed to hook into world guard.");
            }
        } else {
            logger().info("World guard not found. Region conditions cant be used.");
        }
    }

    /**
     * Register the serializer classes.
     * When a provided alias should be used the class needs the {@link org.bukkit.configuration.serialization.SerializableAs}
     * annotation.
     * Its hightly recommended to set a alias otherwise moving a class would break serialization.
     */
    private void buildSerializer() {
        ConfigurationSerialization.registerClass(TimedDoor.class, "timedDoor");
        ConfigurationSerialization.registerClass(ConditionChain.class, "conditionChain");
        ConfigurationSerialization.registerClass(ConditionalDoor.class, "conditionalDoor");
        ConfigurationSerialization.registerClass(ItemBlock.class, "itemBlockCondition");
        ConfigurationSerialization.registerClass(ItemClick.class, "itemClickCondition");
        ConfigurationSerialization.registerClass(ItemHolding.class, "itemHoldingCondition");
        ConfigurationSerialization.registerClass(ItemOwning.class, "itemOwningCondition");
        ConfigurationSerialization.registerClass(Proximity.class, "proximityCondition");
        ConfigurationSerialization.registerClass(Region.class, "regionCondition");
        ConfigurationSerialization.registerClass(Permission.class, "permissionCondition");
        ConfigurationSerialization.registerClass(Time.class, "timeCondition");
        ConfigurationSerialization.registerClass(Weather.class, "weatherCondition");
    }

    /**
     * Get the plugin logger instance.
     *
     * @return plugin logger instance
     */
    @NotNull
    public static Logger logger() {
        return logger;
    }

    /**
     * Enable metrics
     */
    private void enableMetrics() {
        if (!config.isEnableMetrics()) return;

        Pattern version = Pattern.compile("([0-9]\\.(?:[0-9]\\.?)+)");
        Pattern build = Pattern.compile("\\((b[0-9]+)\\)");

        Metrics metrics = new Metrics(this, 8015);

        logger().info(localizer.getMessage("general.metrics"));

        metrics.addCustomChart(new Metrics.DrilldownPie("big_doors_version", () -> {
            String ver = doors.getDescription().getVersion();
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Matcher versionMatcher = version.matcher(ver);
            Matcher buildMatcher = build.matcher(ver);
            if (versionMatcher.find() || buildMatcher.find()) {
                Map<String, Integer> versionMap = new HashMap<>();
                versionMap.put(buildMatcher.group(1), 1);
                map.put(versionMatcher.group(1), versionMap);
            }
            return map;
        }));

        metrics.addCustomChart(new Metrics.AdvancedPie("condition_types", () -> {
            Map<String, Integer> counts = new HashMap<>();
            Collection<ConditionalDoor> values = config.getDoors().values();
            counts.put("item", (int) values.parallelStream().filter(d -> d.getConditionChain().getItem() != null).count());
            counts.put("location", (int) values.parallelStream().filter(d -> d.getConditionChain().getLocation() != null).count());
            counts.put("permission", (int) values.parallelStream().filter(d -> d.getConditionChain().getPermission() != null).count());
            counts.put("time", (int) values.parallelStream().filter(d -> d.getConditionChain().getTime() != null).count());
            counts.put("weather", (int) values.parallelStream().filter(d -> d.getConditionChain().getWeather() != null).count());
            return counts;
        }));
    }

    public static CachingJSEngine JS() {
        return JS;
    }
}

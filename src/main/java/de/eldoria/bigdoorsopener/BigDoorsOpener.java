package de.eldoria.bigdoorsopener;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.commands.BigDoorsOpenerCommand;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.listener.WeatherListener;
import de.eldoria.bigdoorsopener.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.scheduler.DoorChecker;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigDoorsOpener extends JavaPlugin {

    private static @NotNull Logger logger;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private Config config;
    private boolean initialized;
    private Localizer localizer;

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

            buildSerializer();

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
            if (config.isEnableMetrics()) {
                enableMetrics();
            }

            registerListener();


            MessageSender.create(this, "§6[BDO] ", '2', 'c');

        }

        doorChecker = new DoorChecker(config, doors, localizer);

        if (!initialized) {
            getCommand("bigdoorsopener")
                    .setExecutor(new BigDoorsOpenerCommand(this, commander, config, localizer, doorChecker, registerInteraction));
        }

        if (initialized) {
            localizer.setLocale(config.getLanguage());
            scheduler.cancelTasks(this);
        }

        scheduler.scheduleSyncRepeatingTask(this, doorChecker, 100, 1);

        initialized = true;
    }

    private void registerListener() {
        weatherListener = new WeatherListener();
        Bukkit.getPluginManager().registerEvents(weatherListener, this);
        registerInteraction = new RegisterInteraction();
        Bukkit.getPluginManager().registerEvents(registerInteraction, this);
    }

    private void loadExternalSources() {
        PluginManager pm = Bukkit.getPluginManager();

        Plugin bigDoorsPlugin = pm.getPlugin("BigDoors");
        doors = (BigDoors) bigDoorsPlugin;
        commander = doors.getCommander();

        // check if world guard is loaded
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        }
    }

    private void buildSerializer() {
        ConfigurationSerialization.registerClass(TimedDoor.class, "timedDoor");
        ConfigurationSerialization.registerClass(ConditionalDoor.class, "conditionalDoor");

    }


    @NotNull
    public static Logger logger() {
        return logger;
    }

    private void enableMetrics() {
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
    }
}

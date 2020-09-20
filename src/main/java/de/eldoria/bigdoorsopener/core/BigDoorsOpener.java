package de.eldoria.bigdoorsopener.core;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.commands.BDOCommand;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.listener.ModificationListener;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionChain;
import de.eldoria.bigdoorsopener.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.conditions.location.Region;
import de.eldoria.bigdoorsopener.conditions.location.SimpleRegion;
import de.eldoria.bigdoorsopener.conditions.permission.DoorPermission;
import de.eldoria.bigdoorsopener.conditions.permission.PermissionNode;
import de.eldoria.bigdoorsopener.conditions.standalone.MythicMob;
import de.eldoria.bigdoorsopener.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.core.listener.DoorOpenedListener;
import de.eldoria.bigdoorsopener.conditions.listener.ItemConditionListener;
import de.eldoria.bigdoorsopener.conditions.listener.MythicMobsListener;
import de.eldoria.bigdoorsopener.conditions.listener.WeatherListener;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.core.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.crossversion.ServerVersion;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.updater.UpdateChecker;
import lombok.Getter;
import lombok.SneakyThrows;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigDoorsOpener extends JavaPlugin {

    private static Logger logger;
    private static CachingJSEngine JS;
    private static boolean placeholderEnabled = false;
    private static boolean mythicMobsEnabled;
    @Getter
    private static RegionContainer regionContainer = null;
    private static BigDoorsOpener instance;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private Config config;
    private boolean initialized;
    private Localizer localizer;
    // External instances.
    private BigDoors doors;
    private Commander commander;
    // scheduler
    private DoorChecker doorChecker;
    // listener
    private WeatherListener weatherListener;
    private RegisterInteraction registerInteraction;

    /**
     * Get the plugin logger instance.
     *
     * @return plugin logger instance
     */
    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    @NotNull
    public static Logger logger() {
        return logger;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    public static CachingJSEngine JS() {
        return JS;
    }

    public static boolean isPlaceholderEnabled() {
        return placeholderEnabled;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    @NotNull
    public static boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }

    public static BigDoors getBigDoors() {
        return instance.doors;
    }

    public static Localizer localizer() {
        return instance.localizer;
    }

    public static MessageSender getPluginMessageSender() {
        return MessageSender.get(instance);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @SneakyThrows
    @Override
    public void onEnable() {

        ServerVersion.forceVersion(ServerVersion.MC_1_8, ServerVersion.MC_1_16);

        PluginManager pm = Bukkit.getPluginManager();
        if (!initialized) {
            instance = this;
            logger = this.getLogger();

            buildSerializer();

            // create config
            config = new Config(this);

            JS = new CachingJSEngine(config.getJsCacheSize());

            // Check for updates
            if (config.isCheckUpdates()) {
                UpdateChecker.performAndNotifyUpdateCheck(this, 80805, true);
            }

            localizer = new Localizer(this, config.getLanguage(), "messages",
                    "messages", Locale.US, "de_DE", "en_US");

            //enable metrics
            enableMetrics();

            // Load external resources.
            loadExternalSources();



            MessageSender.create(this, "§6[BDO] ", '2', 'c');

            // start door checker
            doorChecker = new DoorChecker(config, doors, localizer);
            scheduler.scheduleSyncRepeatingTask(this, doorChecker, 100, 1);

            registerListener();

            registerCommand("bigdoorsopener",
                    new BDOCommand(this, doors, config, doorChecker));
        }

        if (initialized) {
            localizer.setLocale(config.getLanguage());
            doorChecker.reload();
        }
        initialized = true;
    }

    private void registerCommand(String command, TabExecutor executor) {
        PluginCommand cmd = getCommand("bigdoorsopener");
        if (cmd != null) {
            cmd.setExecutor(executor);
            return;
        }
        logger().warning("Command " + command + " not found!");
    }

    private void registerListener() {
        weatherListener = new WeatherListener();
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(weatherListener, this);
        registerInteraction = new RegisterInteraction(MessageSender.get(this), config);
        pm.registerEvents(registerInteraction, this);
        pm.registerEvents(new ItemConditionListener(doors, localizer, config), this);
        pm.registerEvents(new DoorOpenedListener(config), this);
        pm.registerEvents(doorChecker, this);
        pm.registerEvents(new ModificationListener(config), this);
    }

    @SuppressWarnings( {"AssignmentToStaticFieldFromInstanceMethod", "VariableNotUsedInsideIf"})
    private void loadExternalSources() throws InstantiationException {
        PluginManager pm = Bukkit.getPluginManager();

        if (!pm.isPluginEnabled("BigDoors")) {
            logger().warning("Big Doors is disabled.");
            pm.disablePlugin(this);
            throw new InstantiationException("Big Doors is not enabled");
        }

        Plugin bigDoorsPlugin = pm.getPlugin("BigDoors");
        doors = (BigDoors) bigDoorsPlugin;
        commander = doors.getCommander();

        if (commander != null) {
            logger().info("Hooked into Big Doors successfully.");
        } else {
            logger().warning("Big Doors is not ready or not loaded properly");
            throw new InstantiationException("Big Doors is not enabled");
        }

        // check if world guard is loaded
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            logger().info("World Guard found. Trying to get a hook.");

            String worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard").getDescription().getVersion();
            if (worldGuard.startsWith("7")) {
                regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                if (regionContainer != null) {
                    logger().info("Hooked into world guard successfully.");
                } else {
                    logger().warning("Failed to hook into world guard.");
                }
            } else {
                logger().info("Found legacy World Guard Version. Region conditions can't be used.");
            }
        } else {
            logger().info("World guard not found. Region conditions can't be used.");
        }

        // check if placeholder api is present.
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderEnabled = true;
            logger().info("Placeholder API found. Enabling placeholder usage.");
        } else {
            logger().info("Placeholder API not found. Placeholder usage is disabled.");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            mythicMobsEnabled = true;
            logger().info("MythicMobs found. Enabling mythic mobs listener.");
            pm.registerEvents(new MythicMobsListener(doors, localizer, config), this);
        } else {
            logger().info("MythicMobs not found. MythicMobs conditions are disabled.");
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
        ConfigurationSerialization.registerClass(ConditionBag.class);
        ConfigurationSerialization.registerClass(ConditionalDoor.class);
        ConditionRegistrar.registerCondition(ItemBlock.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemClick.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemHolding.getConditionContainer());
        ConditionRegistrar.registerCondition(ItemOwning.getConditionContainer());
        ConditionRegistrar.registerCondition(Proximity.getConditionContainer());
        ConditionRegistrar.registerCondition(Region.getConditionContainer());
        ConditionRegistrar.registerCondition(SimpleRegion.getConditionContainer());
        ConditionRegistrar.registerCondition(PermissionNode.getConditionContainer());
        ConditionRegistrar.registerCondition(DoorPermission.getConditionContainer());
        ConditionRegistrar.registerCondition(Time.getConditionContainer());
        ConditionRegistrar.registerCondition(Weather.getConditionContainer());
        ConditionRegistrar.registerCondition(Placeholder.getConditionContainer());
        ConditionRegistrar.registerCondition(MythicMob.getConditionContainer());
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

        // old version. will be removed when enough ppl are on version 2.0
        metrics.addCustomChart(new Metrics.SimplePie("big_doors_version",
                () -> doors.getDescription().getVersion()));

        // new hopefully more detailed version information.
        metrics.addCustomChart(new Metrics.DrilldownPie("big_doors_version_new", () -> {
            String ver = doors.getDescription().getVersion();
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Pair<String, String> doorsVersion = getDoorsVersion(ver);
            Map<String, Integer> versionMap = new HashMap<>();
            versionMap.put(doorsVersion.second, 1);
            map.put(doorsVersion.first, versionMap);
            return map;
        }));

        // Get some insights in the condition types.
        // This will probably help to decide which should be developed further.
        metrics.addCustomChart(new Metrics.AdvancedPie("condition_types", () -> {
            Map<String, Integer> counts = new HashMap<>();
            Collection<ConditionalDoor> values = config.getDoors();
            for (String group : ConditionRegistrar.getGroups()) {
                ConditionRegistrar.getConditionGroup(group).ifPresent(g ->
                        counts.put(group, (int) values.parallelStream().filter(d -> d.getConditionBag().isConditionSet(g)).count()));
            }
            return counts;
        }));
    }

    private Pair<String, String> getDoorsVersion(String ver) {
        Pattern version = Pattern.compile("([0-9]\\.(?:[0-9]\\.?)+)");
        Pattern build = Pattern.compile("\\((b[0-9]+)\\)");

        Matcher versionMatcher = version.matcher(ver);
        Matcher buildMatcher = build.matcher(ver);

        String versionString;
        String buildString;

        if (versionMatcher.find()) {
            versionString = versionMatcher.group(1);
        } else {
            versionString = "undefined";
        }

        if (buildMatcher.find()) {
            buildString = buildMatcher.group(1);
        } else {
            buildString = "release";
        }
        return new Pair<>(versionString, buildString);
    }
}

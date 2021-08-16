package de.eldoria.bigdoorsopener.core;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.commands.BDOCommand;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemConditionListener;
import de.eldoria.bigdoorsopener.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemClick;
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
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.exceptions.PluginInitFailed;
import de.eldoria.bigdoorsopener.core.listener.DoorOpenedListener;
import de.eldoria.bigdoorsopener.core.listener.ModificationListener;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.core.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionChain;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.eldoutilities.bstats.EldoMetrics;
import de.eldoria.eldoutilities.bstats.charts.AdvancedPie;
import de.eldoria.eldoutilities.bstats.charts.DrilldownPie;
import de.eldoria.eldoutilities.bstats.charts.SimplePie;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.crossversion.ServerVersion;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.plugin.EldoPlugin;
import de.eldoria.eldoutilities.updater.Updater;
import de.eldoria.eldoutilities.updater.butlerupdater.ButlerUpdateData;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigDoorsOpener extends EldoPlugin {

    private static CachingJSEngine JS;
    private static boolean placeholderEnabled = false;
    private static boolean mythicMobsEnabled;
    private static RegionContainer regionContainer = null;
    private static BigDoorsOpener instance;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private Config config;
    private boolean initialized;
    private ILocalizer localizer;
    // External instances.
    private BigDoors doors;
    private Commander commander;
    // scheduler
    private DoorChecker doorChecker;
    // listener
    private WeatherListener weatherListener;
    private RegisterInteraction registerInteraction;
    private boolean postStart = false;

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

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    @NotNull
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
    public void onPluginEnable() {
        ServerVersion.forceVersion(ServerVersion.MC_1_8, ServerVersion.MC_1_17);

        if (!initialized) {
            BigDoorsOpener.instance = this;
            buildSerializer();
        } else {
            localizer.setLocale(config.language());
            doorChecker.reload();
        }
        initialized = true;
    }

    @Override
    public void onPostStart() {
        if (postStart) return;
        postStart = true;
        // Load external resources. Must be loaded first.
        loadExternalSources();

        lateInitThirdPartyAPIs();

        // create config
        config = new Config(instance);

        JS = new CachingJSEngine(config.jsCacheSize());

        // Check for updates
        if (config.isCheckUpdates()) {
            Updater.butler(new ButlerUpdateData(instance, "bdo.command.reload", true, false, 8, "https://plugins.eldoria.de")).start();
        }

        localizer = ILocalizer.create(instance, "de_DE", "en_US");
        localizer.setLocale(config.language());

        //enable metrics
        enableMetrics();


        MessageSender.create(instance, "§6[BDO]");

        // start door checker
        doorChecker = DoorChecker.start(this, config, doors);
        scheduler.scheduleSyncRepeatingTask(instance, doorChecker, 100, 1);

        registerListener();

        registerCommand("bigdoorsopener",
                new BDOCommand(instance, doors, config, doorChecker));
    }

    private void registerListener() {
        weatherListener = new WeatherListener();
        registerInteraction = new RegisterInteraction(MessageSender.getPluginMessageSender(this), config);
        registerListener(new ModificationListener(config), doorChecker, new DoorOpenedListener(config),
                new ItemConditionListener(doors, config), registerInteraction, weatherListener);
        if (mythicMobsEnabled) {
            registerListener(new MythicMobsListener(doors, config));
        }
    }

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "VariableNotUsedInsideIf"})
    private void loadExternalSources() throws PluginInitFailed {

        if (!getPluginManager().isPluginEnabled("BigDoors")) {
            logger().warning("Big Doors is disabled.");
            getPluginManager().disablePlugin(this);
            throw new PluginInitFailed("Big Doors is not enabled");
        }

        Plugin bigDoorsPlugin = getPluginManager().getPlugin("BigDoors");
        doors = (BigDoors) bigDoorsPlugin;
        commander = doors.getCommander();

        if (commander != null) {
            logger().info("Hooked into Big Doors successfully.");
        } else {
            logger().warning("Big Doors is not ready or not loaded properly");
            throw new PluginInitFailed("Big Doors is not enabled");
        }
    }

    private void lateInitThirdPartyAPIs() {
        // Since bukkit is not able to guarantee that plugins will be loaded before this,
        // the third party apis will be initialized delayed

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
        } else {
            logger().info("MythicMobs not found. MythicMobs conditions are disabled.");
        }
    }

    @Override
    public List<Class<? extends ConfigurationSerializable>> getConfigSerialization() {
        return Arrays.asList(TimedDoor.class, ConditionChain.class, ConditionBag.class, ConditionalDoor.class);
    }

    /**
     * Register the serializer classes. When a provided alias should be used the class needs the {@link SerializableAs}
     * annotation. Its hightly recommended to set a alias otherwise moving a class would break serialization.
     */
    private void buildSerializer() {
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


    /**
     * Enable metrics
     */
    private void enableMetrics() {
        if (!config.isEnableMetrics()) return;

        EldoMetrics metrics = new EldoMetrics(this, 8015);

        logger().info(localizer.getMessage("general.metrics"));

        // old version. will be removed when enough ppl are on version 2.0
        metrics.addCustomChart(new SimplePie("big_doors_version",
                () -> doors.getDescription().getVersion()));

        // new hopefully more detailed version information.
        metrics.addCustomChart(new DrilldownPie("big_doors_version_new", () -> {
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
        metrics.addCustomChart(new AdvancedPie("condition_types", () -> {
            Map<String, Integer> counts = new HashMap<>();
            Collection<ConditionalDoor> values = config.getDoors();
            for (String group : ConditionRegistrar.getGroups()) {
                ConditionRegistrar.getConditionGroup(group).ifPresent(g ->
                        counts.put(group, (int) values.parallelStream().filter(d -> d.conditionBag().isConditionSet(g)).count()));
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

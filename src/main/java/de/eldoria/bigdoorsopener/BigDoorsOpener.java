package de.eldoria.bigdoorsopener;

import de.eldoria.bigdoorsopener.commands.BigDoorsOpenerCommand;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.scheduler.DoorApproachScheduler;
import de.eldoria.bigdoorsopener.scheduler.TimedDoorScheduler;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class BigDoorsOpener extends JavaPlugin {

    private static @NotNull Logger logger;
    private BukkitScheduler scheduler = Bukkit.getScheduler();
    private Config config;
    private boolean initialized;

    private BigDoors doors;
    private Commander commander;

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        if (!initialized) {
            logger = this.getLogger();
            ConfigurationSerialization.registerClass(TimedDoor.class, "timedDoor");
            PluginManager pm = Bukkit.getPluginManager();
            Plugin bigDoorsPlugin = pm.getPlugin("BigDoors");
            doors = (BigDoors) bigDoorsPlugin;
            commander = doors.getCommander();
            config = new Config(this);
        }
        TimedDoorScheduler timedDoorScheduler = new TimedDoorScheduler(doors, config);

        if (!initialized) {
            getCommand("bigdoorsopener").setExecutor(new BigDoorsOpenerCommand(this, commander, config, timedDoorScheduler));
        }

        if (initialized) {
            scheduler.cancelTasks(this);
        }

        scheduler.scheduleSyncRepeatingTask(this, new DoorApproachScheduler(config, doors), 100, config.getApproachRefreshRate());
        scheduler.scheduleSyncRepeatingTask(this, timedDoorScheduler, 100, config.getTimedRefreshRate());

        initialized = true;
    }

    @NotNull
    public static Logger logger() {
        return logger;
    }
}

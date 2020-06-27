package de.eldoria.bigdoorsopener.commands;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.scheduler.TimedDoorScheduler;
import de.eldoria.bigdoorsopener.util.ArrayUtil;
import de.eldoria.bigdoorsopener.util.MessageSender;
import de.eldoria.bigdoorsopener.util.Parser;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BigDoorsOpenerCommand implements TabExecutor {
    private final BigDoorsOpener plugin;
    private final Commander commander;
    private final Config config;
    private final TimedDoorScheduler scheduler;

    public BigDoorsOpenerCommand(BigDoorsOpener plugin, Commander commander, Config config, TimedDoorScheduler scheduler) {
        this.plugin = plugin;
        this.commander = commander;
        this.config = config;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (args.length == 0) return false;
        // bdo setTime <door> <tick open> <tick close>
        // bdo setAutoOpenRange <door> <range in blocks>
        if ("setTime".equalsIgnoreCase(args[0])) {
            if (args.length != 4) return false;
            Door door = commander.getDoor(args[1], null);
            if (door == null) {
                MessageSender.sendError(player, "Door not found.");
                return true;
            }
            Integer open = Parser.parseInt(args[2]);
            Integer close = Parser.parseInt(args[3]);

            if (open == null || close == null) {
                open = Parser.parseTimeToTicks(args[2]);
                close = Parser.parseTimeToTicks(args[3]);
                if (open == null || close == null) {
                    MessageSender.sendError(player, "Could not parse time.");
                    return true;
                }
            }

            World world = door.getWorld();
            if (world == null) {
                MessageSender.sendError(player, "The world of this door is not loaded.");
                return true;
            }
            Location maximum = door.getMaximum();
            Location minimum = door.getMinimum();
            Vector vector = new Vector(
                    (maximum.getX() + minimum.getX()) / 2,
                    (maximum.getY() + minimum.getY()) / 2,
                    (maximum.getZ() + minimum.getZ()) / 2);

            TimedDoor timedDoor = config.getDoors().computeIfAbsent(door.getDoorUID(),
                    d -> new TimedDoor(d, world.getName(), vector));
            timedDoor.setTicksOpen(open);
            timedDoor.setTicksClose(close);
            config.safeConfig();

            scheduler.registerDoor(timedDoor);

            MessageSender.sendMessage(player, "Time for door §6" + door.getName() + "§r set.\n"
                    + "Open at: §6" + Parser.parseTicksToTime(open) + "§r\n"
                    + "Close at: §6" + Parser.parseTicksToTime(close));
            return true;
        }

        if ("setRange".equalsIgnoreCase(args[0])) {
            if (args.length != 3) return false;
            Door door = commander.getDoor(args[1], null);
            if (door == null) {
                MessageSender.sendError(player, "Door not found.");
                return true;
            }
            Double range = Parser.parseDouble(args[2]);
            if (range == null) {
                MessageSender.sendError(player, "Could not parse range.");
                return true;
            }
            if (!config.getDoors().containsKey(door.getDoorUID())) {
                MessageSender.sendError(player, "Please set the time first.");
                return true;
            }

            if (range > 100 || range < 0) {
                MessageSender.sendError(player, "Invalid range");
                return true;
            }

            TimedDoor timedDoor = config.getDoors().get(door.getDoorUID());
            timedDoor.setOpenRange(range);
            config.safeConfig();

            MessageSender.sendMessage(player, "Set range of door §6" + door.getName() + "§r to §6" + range + "§r.");
            return true;
        }

        if ("doorInfo".equalsIgnoreCase(args[0])) {
            if (args.length != 2) return false;
            Door door = commander.getDoor(args[1], null);
            if (door == null) {
                MessageSender.sendError(player, "Door not found.");
                return true;
            }
            TimedDoor timedDoor = config.getDoors().get(door.getDoorUID());
            if (timedDoor == null) {
                MessageSender.sendMessage(player, "This door is not registered.");
                return true;
            }
            MessageSender.sendMessage(player,
                    door.getName() + " Info:"
                            + "Range: " + timedDoor.getOpenRange() + "\n"
                            + "Open at: " + Parser.parseTicksToTime(timedDoor.getTicksOpen()) + "\n"
                            + "Close at: " + Parser.parseTicksToTime(timedDoor.getTicksClose()) + "\n"
            );
            return true;
        }

        if ("unregisterDoor".equalsIgnoreCase(args[0])) {
            if (args.length != 2) return false;
            Door door = commander.getDoor(args[1], null);


            if (door == null) {
                MessageSender.sendError(player, "Door not found.");
                return true;
            }

            if (!config.getDoors().containsKey(door.getDoorUID())) {
                MessageSender.sendError(player, "This door is not registered.");
            }

            config.getDoors().remove(door.getDoorUID());
            config.safeConfig();

            MessageSender.sendMessage(player, "Unregistered door §6" + door.getName() + "§r.");
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            config.reloadConfig();
            scheduler.reload();
            plugin.onEnable();
            MessageSender.sendMessage(player, "Reload complete.");
            return true;
        }
        return false;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("setTime".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<door name>");
            }
            if (args.length == 3) {
                return Collections.singletonList("<open in ticks (0-24000) or time (HH:mm)>");
            }
            if (args.length == 4) {
                return Collections.singletonList("<close in ticks (0-24000) or time (HH:mm)>");
            }
            return Collections.emptyList();
        }

        if ("setRange".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<door name>");
            }
            if (args.length == 3) {
                return Collections.singletonList("<range 0-100, 0 to disable>");
            }
            return Collections.emptyList();
        }

        if ("doorInfo".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<door name>");
            }
            return Collections.emptyList();
        }

        if ("unregisterDoor".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<door name>");
            }
            return Collections.emptyList();
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }


        return ArrayUtil.startingWithInArray(args[0],
                new String[] {"setTime", "setRange", "unregisterDoor", "doorInfo", "reload"})
                .collect(Collectors.toList());
    }
}

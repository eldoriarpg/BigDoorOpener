package de.eldoria.bigdoorsopener.commands;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.scheduler.TimedDoorScheduler;
import de.eldoria.bigdoorsopener.util.ArrayUtil;
import de.eldoria.bigdoorsopener.util.MessageSender;
import de.eldoria.bigdoorsopener.util.Pair;
import de.eldoria.bigdoorsopener.util.Parser;
import de.eldoria.bigdoorsopener.util.Permissions;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
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

        if (args.length == 0) {
            MessageSender.sendMessage(player,
                    "Help for §6BigDoorOpener§r\n"
                            + "§6setClosed <doorId>\n"
                            + "  §rSet a door as permanent closed. It will only open on player approach.\n"
                            + "§6setTimed <doorId> <open> <close>\n"
                            + "  §rSet a door as timed. It will be open between open and close time. However it will open on player approach if closed. Use a permission to avoid this.\n"
                            + "§6setRange <doorId> <range>\n"
                            + "  §rSet the approach range of a door. The range is spherical around the mass center of the door. Default is 10.\n"
                            + "§6invertOpen <doorId> <true|false>\n"
                            + "  §rInvert the state of the door. Useful if you created it in a open state.\n"
                            + "§6requiresPermission <doorId> <true|false|permission>\n"
                            + "  §rA gate will require a permission to open on player approach.\n"
                            + "§6unregister <doorId>\n"
                            + "  §rUnregister a door.\n"
                            + "§6info <doorId>\n"
                            + "  §rGet information about this door.\n"
                            + "§6list\n"
                            + "  §rGet a list of all registered doors.\n"
                            + "§6reload\n"
                            + "  §rThis will reload the plugin/config and force the doors in the correct state.\n"
                            + "§6about\n"
                            + "  §rInformation about this plugin");
            return true;

        }

        if ("about".equalsIgnoreCase(args[0])) {
            PluginDescriptionFile descr = plugin.getDescription();
            String info = "§bBig Doors opener§r by §b" + String.join(", ", descr.getAuthors()) + "§r\n"
                    + "§bVersion§r : " + descr.getVersion() + "\n"
                    + "§bSpigot:§r " + descr.getWebsite() + "\n"
                    + "§bSupport:§r https://discord.gg/SrFg7S";
            return true;
        }

        // TODO: Weather based door. Closes when it rains or when sky is clear.
        // TODO: Key door. Define a item which is needed to open this door, when in range. Item can be consumed.


        if ("setClosed".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }

            if (args.length != 2) return false;
            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if (!saveDoor(door, player, 0, 0)) {
                MessageSender.sendMessage(player, "Door §6" + door.getName() + "§r is now permanent closed."
                        + "It will only open on approach of a player.");
            }
            return true;
        }

        if ("setTimed".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }

            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if (args.length != 4) return false;

            OptionalInt open = Parser.parseInt(args[2]);
            OptionalInt close = Parser.parseInt(args[3]);

            if (!open.isPresent() || !close.isPresent()) {
                open = Parser.parseTimeToTicks(args[2]);
                close = Parser.parseTimeToTicks(args[3]);
                if (!open.isPresent() || !close.isPresent()) {
                    MessageSender.sendError(player, "Could not parse time.");
                    return true;
                }

                if (open.getAsInt() > 24000 || open.getAsInt() < 0
                        || close.getAsInt() > 24000 || close.getAsInt() < 0) {
                    MessageSender.sendError(player, "Invalid time.");
                    return true;
                }
            }

            if (saveDoor(door, player, open.getAsInt(), close.getAsInt())) {
                MessageSender.sendMessage(player, "Time for door §6" + door.getName() + "§r set.\n"
                        + "Open at: §6" + Parser.parseTicksToTime(open.getAsInt()) + "§r\n"
                        + "Close at: §6" + Parser.parseTicksToTime(close.getAsInt()));
            }
            return true;
        }

        // TODO: Enter range also as x,y,z range
        // TODO: Enter region version: (CUBOID, ELIPSOID)
        // TODO: Enter World Guard region?
        if ("setRange".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            if (args.length != 3) return false;
            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }
            OptionalDouble range = Parser.parseDouble(args[2]);
            if (!range.isPresent()) {
                MessageSender.sendError(player, "Could not parse range.");
                return true;
            }
            if (!config.getDoors().containsKey(door.getDoorUID())) {
                MessageSender.sendError(player, "Please set the door timed or closed first.");
                return true;
            }

            if (range.getAsDouble() > 100 || range.getAsDouble() < 0) {
                MessageSender.sendError(player, "Invalid range");
                return true;
            }

            TimedDoor timedDoor = config.getDoors().get(door.getDoorUID());
            timedDoor.setOpenRange(range.getAsDouble());
            config.safeConfig();

            MessageSender.sendMessage(player, "Set range of door §6" + door.getName() + "§r to §6" + range + "§r.");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            if (args.length != 2) return false;
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);
            if (door == null) {
                return true;
            }

            TimedDoor d = door.first;
            StringBuilder builder = new StringBuilder("§6" + door.second.getName() + "§r Info:\n")
                    .append("§rRange: §6").append(d.getOpenRange()).append("\n");
            if (d.isPermanentlyClosed()) {
                builder.append("§6Permanently closed.\n");
            } else {
                builder.append("§rOpen at: §6").append(Parser.parseTicksToTime(d.getTicksOpen())).append("\n")
                        .append("§rClose at: §6").append(Parser.parseTicksToTime(d.getTicksClose())).append("\n");
            }
            if (d.getPermission().isEmpty()) {
                builder.append("§rPermission: §6none").append("\n");
            } else {
                builder.append("§rPermission: §6").append(d.getPermission()).append("\n");
            }
            builder.append("§rInverted open: §6").append(d.isInvertOpen());

            MessageSender.sendMessage(player, builder.toString());
            return true;
        }

        if ("unregister".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            if (args.length != 2) return false;
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);

            if (door == null) {
                return true;
            }

            config.getDoors().remove(door.second.getDoorUID());
            config.safeConfig();

            MessageSender.sendMessage(player, "Unregistered door §6" + door.second.getName() + "§r.");
            return true;
        }

        if ("invertOpen".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            if (args.length != 3) return false;
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if ("true".equalsIgnoreCase(args[2])) {
                door.first.setInvertOpen(true);
                MessageSender.sendMessage(player, "Open state is now inverted.");
            } else if ("false".equalsIgnoreCase(args[2])) {
                door.first.setInvertOpen(false);
                MessageSender.sendMessage(player, "Open state is not inverted.");
            } else {
                MessageSender.sendError(player, "Invalid input. §2True§r or §4False§r.");
                return true;
            }
            config.safeConfig();
            return true;
        }

        if ("requiresPermission".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            if (args.length != 3) return false;
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if ("true".equalsIgnoreCase(args[2])) {
                door.first.setPermission(Permissions.USE + "." + door.second.getDoorUID());
                MessageSender.sendMessage(player, "The permission §6" + door.first.getPermission() + "§r is now required to use this door.");
                return true;
            }
            if ("false".equalsIgnoreCase(args[2])) {
                door.first.setPermission("");
                MessageSender.sendMessage(player, "No permission is required for this door.");
                return true;
            }
            door.first.setPermission(args[2]);
            MessageSender.sendMessage(player, "The permission §6" + door.first.getPermission() + "§r is now required to use this door.");
            config.safeConfig();
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            Map<Long, TimedDoor> doors = config.getDoors();
            StringBuilder builder = new StringBuilder("Registered doors:\n");

            for (TimedDoor value : doors.values()) {
                Door door = commander.getDoor(String.valueOf(value.getDoorUID()), null);
                builder.append(value.getDoorUID()).append(" | ")
                        .append(door.getName())
                        .append("(").append(door.getWorld().getName()).append(")\n");
            }
            MessageSender.sendMessage(player, builder.toString());
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.RELOAD)) {
                MessageSender.sendError(player, Permissions.MESSAGE);
                return true;
            }
            config.reloadConfig();
            scheduler.reload();
            plugin.onEnable();
            MessageSender.sendMessage(player, "Reload complete.");
            return true;
        }

        return false;
    }

    private boolean saveDoor(Door door, Player player, int open, int close) {
        World world = door.getWorld();
        if (world == null) {
            MessageSender.sendError(player, "The world of this door is not loaded.");
            return false;
        }
        Location maximum = door.getMaximum();
        Location minimum = door.getMinimum();
        Vector vector = new Vector(
                (maximum.getX() + minimum.getX()) / 2,
                (maximum.getY() + minimum.getY()) / 2,
                (maximum.getZ() + minimum.getZ()) / 2);

        TimedDoor timedDoor = config.getDoors().computeIfAbsent(door.getDoorUID(),
                d -> new TimedDoor(d, world.getName(), vector));
        timedDoor.setTicks(open, close);
        config.safeConfig();

        scheduler.registerDoor(timedDoor);
        return true;
    }

    private Pair<TimedDoor, Door> getTimedDoor(String doorUID, Player player) {
        Door door = getDoor(doorUID, player);
        if (door == null) {
            return null;
        }
        TimedDoor timedDoor = config.getDoors().get(door.getDoorUID());
        if (timedDoor == null) {
            MessageSender.sendMessage(player, "This door is not registered.");
            return null;
        }
        return new Pair<>(timedDoor, door);
    }

    private Door getDoor(String doorUID, Player player) {
        Door door = commander.getDoor(doorUID, null);
        if (door == null) {
            MessageSender.sendError(player, "Door not found.");
            return null;
        }
        return door;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("setTimed".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            if (args.length == 3) {
                return Collections.singletonList("<open in ticks (0-24000) or time (HH:mm)>");
            }
            if (args.length == 4) {
                return Collections.singletonList("<close in ticks (0-24000) or time (HH:mm)>");
            }
            return Collections.emptyList();
        }

        if ("setClosed".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            return Collections.emptyList();
        }

        if ("setRange".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            if (args.length == 3) {
                return Collections.singletonList("<range 0-100, 0 to disable>");
            }
            return Collections.emptyList();
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            return Collections.emptyList();
        }

        if ("unregister".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            return Collections.emptyList();
        }

        if ("invertOpen".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false");
            }
            return Collections.emptyList();
        }

        if ("requiresPermission".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false", "own.permission.node");
            }
            return Collections.emptyList();
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }

        return ArrayUtil.startingWithInArray(args[0],
                new String[] {"setTimed", "setClosed", "setRange", "unregister", "info", "invertOpen", "list", "requiresPermission", "reload"})
                .collect(Collectors.toList());
    }
}

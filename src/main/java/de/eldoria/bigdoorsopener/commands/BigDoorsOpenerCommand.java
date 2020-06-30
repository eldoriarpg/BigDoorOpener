package de.eldoria.bigdoorsopener.commands;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.scheduler.TimedDoorScheduler;
import de.eldoria.bigdoorsopener.util.Pair;
import de.eldoria.bigdoorsopener.util.Parser;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArrayUtil;
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
    private final Localizer localizer;
    private final TimedDoorScheduler scheduler;
    private final MessageSender messageSender;

    public BigDoorsOpenerCommand(BigDoorsOpener plugin, Commander commander, Config config, Localizer localizer, TimedDoorScheduler scheduler) {
        this.plugin = plugin;
        this.commander = commander;
        this.config = config;
        this.localizer = localizer;
        this.scheduler = scheduler;
        messageSender = MessageSender.get(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (args.length == 0 || (args.length == 1 && "help".equalsIgnoreCase(args[0]))) {
            messageSender.sendMessage(player,
                    localizer.getMessage("help.title",
                            Replacement.create("PLUGIN_NAME", plugin.getDescription().getName()).addFormatting('6')) + "\n"
                            + "§6setClosed <doorId>\n"
                            + "  §r" + localizer.getMessage("help.setClosed") + "\n"
                            + "§6setTimed <doorId> <open> <close>\n"
                            + "  §r" + localizer.getMessage("help.setTimed") + "\n"
                            + "§6setRange <doorId> <range>\n"
                            + "  §r" + localizer.getMessage("help.setRange") + "\n"
                            + "§6invertOpen <doorId> <true|false>\n"
                            + "  §r" + localizer.getMessage("help.invertOpen") + "\n"
                            + "§6requiresPermission <doorId> <true|false|permission>\n"
                            + "  §r" + localizer.getMessage("help.requiresPermission") + "\n"
                            + "§6unregister <doorId>\n"
                            + "  §r" + localizer.getMessage("help.unregister") + "\n"
                            + "§6info <doorId>\n"
                            + "  §r" + localizer.getMessage("help.info") + "\n"
                            + "§6list\n"
                            + "  §r" + localizer.getMessage("help.list") + "\n"
                            + "§6reload\n"
                            + "  §r" + localizer.getMessage("help.reload") + "\n"
                            + "§6about\n"
                            + "  §r" + localizer.getMessage("help.about"));
            return true;

        }

        if ("about".equalsIgnoreCase(args[0])) {
            PluginDescriptionFile descr = plugin.getDescription();
            String info = localizer.getMessage("about",
                    Replacement.create("PLUGIN_NAME", "Big Doors Opener").addFormatting('b'),
                    Replacement.create("AUTHORS", String.join(", ", descr.getAuthors())).addFormatting('b'),
                    Replacement.create("VERSION", descr.getVersion()).addFormatting('b'),
                    Replacement.create("WEBSITE", descr.getWebsite()).addFormatting('b'),
                    Replacement.create("DISCORD", "https://discord.gg/JJdx3xe").addFormatting('b'));
            messageSender.sendMessage(player, info);
            return true;
        }

        // TODO: Weather based door. Closes when it rains or when sky is clear.
        // TODO: Key door. Define a item which is needed to open this door, when in range. Item can be consumed.
        // TODO: Password door. Enter a password to open a door when a player approaches it. https://hub.spigotmc.org/javadocs/spigot/org/bukkit/conversations/Conversation.html

        // TODO: Permission to access all doors. Default only own doors.

        if ("setClosed".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }

            if (args.length != 2) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if (!saveDoor(door, player, 0, 0)) {
                messageSender.sendMessage(player, localizer.getMessage("setClosed.message",
                        Replacement.create("DOOR_NAME", door.getName()).addFormatting('6')));
            }
            return true;
        }

        if ("setTimed".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }

            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if (args.length != 4) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }

            OptionalInt open = Parser.parseInt(args[2]);
            OptionalInt close = Parser.parseInt(args[3]);

            if (!open.isPresent() || !close.isPresent()) {
                open = Parser.parseTimeToTicks(args[2]);
                close = Parser.parseTimeToTicks(args[3]);
                if (!open.isPresent() || !close.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("setTimed.parseError"));
                    return true;
                }

                if (open.getAsInt() > 24000 || open.getAsInt() < 0
                        || close.getAsInt() > 24000 || close.getAsInt() < 0) {
                    messageSender.sendError(player, localizer.getMessage("setTimed.invalidTime"));
                    return true;
                }
            }

            if (saveDoor(door, player, open.getAsInt(), close.getAsInt())) {
                messageSender.sendMessage(player, localizer.getMessage("setTimed.message",
                        Replacement.create("DOOR_NAME", door.getName()).addFormatting('6'),
                        Replacement.create("OPEN_TIME", Parser.parseTicksToTime(open.getAsInt())).addFormatting('6'),
                        Replacement.create("CLOSE_TIME", Parser.parseTicksToTime(close.getAsInt())).addFormatting('6')));
            }
            return true;
        }

        // TODO: Enter range also as x,y,z range
        // TODO: Enter region version: (CUBOID, ELIPSOID)
        // TODO: Enter World Guard region?
        if ("setRange".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }
            if (args.length != 3) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Door door = getDoor(args[1], player);
            if (door == null) {
                return true;
            }

            OptionalDouble range = Parser.parseDouble(args[2]);
            if (!range.isPresent()) {
                messageSender.sendError(player, localizer.getMessage("setRange.parseError"));
                return true;
            }
            if (!config.getDoors().containsKey(door.getDoorUID())) {
                messageSender.sendError(player, localizer.getMessage("setRange.unregisteredError"));
                return true;
            }

            if (range.getAsDouble() > 100 || range.getAsDouble() < 0) {
                messageSender.sendError(player, localizer.getMessage("setRange.invalidRange"));
                return true;
            }

            TimedDoor timedDoor = config.getDoors().get(door.getDoorUID());
            timedDoor.setOpenRange(range.getAsDouble());
            config.safeConfig();

            messageSender.sendMessage(player, localizer.getMessage("setRange.message",
                    Replacement.create("DOOR_NAME", door.getName()).addFormatting('6'),
                    Replacement.create("RANGE", range.getAsDouble()).addFormatting('6')));
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }

            if (args.length != 2) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);
            if (door == null) {
                return true;
            }

            TimedDoor d = door.first;
            StringBuilder builder = new StringBuilder(localizer.getMessage("info.info",
                    Replacement.create("DOOR_NAME", door.second.getName()).addFormatting('6')) + "\n")
                    .append(localizer.getMessage("info.range",
                            Replacement.create("RANGE", d.getOpenRange()).addFormatting('6')))
                    .append("\n");
            if (d.isPermanentlyClosed()) {
                builder.append("§6").append(localizer.getMessage("info.closed")).append("§r")
                        .append("\n");
            } else {
                builder.append(localizer.getMessage("info.time",
                        Replacement.create("OPEN_TIME", Parser.parseTicksToTime(d.getTicksOpen())).addFormatting('6'),
                        Replacement.create("CLOSE_TIME", Parser.parseTicksToTime(d.getTicksClose())).addFormatting('6')))
                        .append("\n");
            }
            if (d.getPermission().isEmpty()) {
                builder.append(localizer.getMessage("info.permission",
                        Replacement.create("PERMISSION", "none").addFormatting('6')))
                        .append("\n");
            } else {
                builder.append(localizer.getMessage("info.permission",
                        Replacement.create("PERMISSION", d.getPermission()).addFormatting('6')))
                        .append("\n");
            }
            builder.append(localizer.getMessage("info.open",
                    Replacement.create("STATE", d.isInvertOpen()).addFormatting('6')));

            messageSender.sendMessage(player, builder.toString());
            return true;
        }

        if ("unregister".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }
            if (args.length != 2) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);

            if (door == null) {
                return true;
            }

            config.getDoors().remove(door.second.getDoorUID());
            config.safeConfig();

            messageSender.sendMessage(player, localizer.getMessage("unregister.message",
                    Replacement.create("DOOR_NAME", door.second.getName()).addFormatting('6')));
            return true;
        }

        if ("invertOpen".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }
            if (args.length != 3) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);
            if (door == null) {
                return true;
            }

            if ("true".equalsIgnoreCase(args[2])) {
                door.first.setInvertOpen(true);
                messageSender.sendMessage(player, localizer.getMessage("invertOpen.inverted"));
            } else if ("false".equalsIgnoreCase(args[2])) {
                door.first.setInvertOpen(false);
                messageSender.sendMessage(player, localizer.getMessage("invertOpen.notInverted"));
            } else {
                messageSender.sendError(player, localizer.getMessage("invertOpen.invalidInput",
                        Replacement.create("TRUE", "true").addFormatting('2'),
                        Replacement.create("FALSE", "false").addFormatting('4')));
                return true;
            }
            config.safeConfig();
            return true;
        }

        if ("requiresPermission".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }

            if (args.length != 3) {
                messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
                return true;
            }
            Pair<TimedDoor, Door> door = getTimedDoor(args[1], player);

            if (door == null) {
                return true;
            }

            if ("true".equalsIgnoreCase(args[2])) {
                door.first.setPermission(Permissions.USE + "." + door.second.getDoorUID());
                messageSender.sendMessage(player, localizer.getMessage("requiredPermission.permission",
                        Replacement.create("PERMISSION", door.first.getPermission())));
                return true;
            }

            if ("false".equalsIgnoreCase(args[2])) {
                door.first.setPermission("");
                messageSender.sendMessage(player, localizer.getMessage("requiredPermission.noPermission"));
                return true;
            }

            door.first.setPermission(args[2]);
            messageSender.sendMessage(player, localizer.getMessage("requiredPermission.permission",
                    Replacement.create("PERMISSION", door.first.getPermission())));
            config.safeConfig();
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.USE)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }
            Map<Long, TimedDoor> doors = config.getDoors();
            StringBuilder builder = new StringBuilder(localizer.getMessage("list.title"));

            for (TimedDoor value : doors.values()) {
                Door door = commander.getDoor(String.valueOf(value.getDoorUID()), null);
                builder.append(value.getDoorUID()).append(" | ")
                        .append(door.getName())
                        .append("(").append(door.getWorld().getName()).append(")\n");
            }

            messageSender.sendMessage(player, builder.toString());
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission(Permissions.RELOAD)) {
                messageSender.sendError(player, localizer.getMessage("error.permission"));
                return true;
            }
            config.reloadConfig();
            scheduler.reload();
            plugin.onEnable();
            messageSender.sendMessage(player, localizer.getMessage("reload.completed"));
            return true;
        }
        return false;
    }

    private boolean saveDoor(Door door, Player player, int open, int close) {
        World world = door.getWorld();
        if (world == null) {
            messageSender.sendError(player, localizer.getMessage("error.worldNotLoaded"));
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
            messageSender.sendMessage(player, localizer.getMessage("error.doorNotRegistered"));
            return null;
        }
        return new Pair<>(timedDoor, door);
    }

    private Door getDoor(String doorUID, Player player) {
        Door door = commander.getDoor(doorUID, null);
        if (door == null) {
            messageSender.sendError(player, localizer.getMessage("error.doorNotFound"));
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
                return Collections.singletonList(localizer.getMessage("tabcomplete.setTimed.open"));
            }
            if (args.length == 4) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.setTimed.close"));
            }
            return Collections.emptyList();
        }

        if ("setClosed".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("setRange".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.range"));
            }
            return Collections.emptyList();
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("unregister".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("invertOpen".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false");
            }
            return Collections.emptyList();
        }

        if ("requiresPermission".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false", localizer.getMessage("tabcomplete.permission"));
            }
            return Collections.emptyList();
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }

        if (args.length == 1) {

            return ArrayUtil.startingWithInArray(args[0],
                    new String[] {"setTimed", "setClosed", "setRange", "unregister", "info", "invertOpen", "list", "requiresPermission", "reload"})
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

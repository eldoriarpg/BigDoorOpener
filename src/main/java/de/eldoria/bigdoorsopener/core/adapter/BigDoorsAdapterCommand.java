/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.adapter;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.command.AdvancedCommand;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.commands.executor.ITabExecutor;
import de.eldoria.eldoutilities.container.Pair;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Base of the commands which work on a door. Resolves doors by name or id with the access rules of Big Doors and
 * {@link Permissions#ACCESS_ALL}. A door which can not be used ends the command with an error.
 */
public abstract class BigDoorsAdapterCommand extends AdvancedCommand implements ITabExecutor {
    private final Commander commander;
    private final Config config;

    public BigDoorsAdapterCommand(Plugin plugin, CommandMeta meta, BigDoors bigDoors, Config config) {
        super(plugin, meta);
        commander = bigDoors.getCommander();
        this.config = config;
    }

    /**
     * Get the door with a specific id.
     *
     * @param doorId id of the door as long or string
     * @return Door with id if exists.
     */
    @Nullable
    protected Door getDoor(String doorId) {
        return getDoor(null, doorId);
    }

    /**
     * Get the door with a specific id when the player the permission to access this door.
     *
     * @param player owner of the door.
     * @param doorId id of the door as long or string
     * @return Door with id if exists.
     */
    @Nullable
    protected Door getDoor(Player player, String doorId) {
        return commander.getDoor(doorId, player);
    }

    /**
     * Get a list of doors owned by the player.
     *
     * @param player Player for which the doors should be retrieved.
     * @return List of doors which are owned by the player
     */
    public List<Door> getDoors(Player player) {
        return getDoors(player, null);
    }

    /**
     * Get a list of doors owned by the player, which match the name.
     *
     * @param player Player for which the doors should be retrieved.
     * @param name   Get doors which match the name.
     * @return List of doors which are owned by the player
     */
    protected List<Door> getDoors(Player player, String name) {
        return commander.getDoors(player.getUniqueId().toString(), name);
    }

    /**
     * Get a set of all existing doors. The door will always be owned by the creator.
     *
     * @return A set of doors.
     */
    public Set<Door> getDoors() {
        return commander.getDoors();
    }

    /**
     * Finds a door the sender may use. Searches the doors of the player first and all doors by id afterwards. A door
     * of another player is only returned with {@link Permissions#ACCESS_ALL}. The console may use every door.
     *
     * @param doorUID uid or name of the door.
     * @param sender  sender which performed the request.
     * @return the door
     * @throws CommandException when the door does not exist, the name is ambiguous or the sender has no access
     */
    protected Door getPlayerDoor(String doorUID, CommandSender sender) throws CommandException {
        if (!(sender instanceof Player player)) {
            Door door = getDoor(doorUID);
            if (door == null) throw CommandException.message("error.doorNotFound");
            door.setPermission(0);
            return door;
        }

        List<Door> doors = getDoors(player, doorUID);

        if (doors.isEmpty()) {
            Door door = getDoor(player, doorUID);
            if (door != null) return door;

            door = getDoor(doorUID);
            if (door == null) throw CommandException.message("error.doorNotFound");

            if (!player.hasPermission(Permissions.ACCESS_ALL)) throw CommandException.message("error.notYourDoor");
            door.setPermission(0);
            return door;
        }

        if (doors.size() != 1) throw CommandException.message("error.ambiguousDoorName");

        return doors.getFirst();
    }

    /**
     * Returns the door from config or registers it if it is not registered yet.
     *
     * @param door door to get.
     * @return registered door
     * @throws CommandException when the world of the door is not loaded
     */
    protected ConditionalDoor getOrRegister(Door door) throws CommandException {
        World world = door.getWorld();
        if (world == null) throw CommandException.message("error.worldNotLoaded");

        if (config.containsDoor(door.getDoorUID())) {
            return config.getDoor(door.getDoorUID());
        }

        Location max = door.getMaximum();
        Location min = door.getMinimum();
        Vector vector = new Vector(
                (max.getX() + min.getX()) / 2,
                (max.getY() + min.getY()) / 2,
                (max.getZ() + min.getZ()) / 2);

        return config.computeDoorIfAbsent(door.getDoorUID(),
                doorId -> new ConditionalDoor(doorId, world.getName(), vector));
    }

    /**
     * Finds a registered door the sender may use. See {@link #getPlayerDoor(String, CommandSender)}.
     *
     * @param doorUID uid or name of the door
     * @param sender  sender which requests this door.
     * @return the registered door and the Big Doors door
     * @throws CommandException when the door can not be used or is not registered
     */
    public Pair<ConditionalDoor, Door> getConditionalPlayerDoor(String doorUID, CommandSender sender) throws CommandException {
        Door door = getPlayerDoor(doorUID, sender);

        if (door.getPermission() > 1) throw CommandException.message("error.notYourDoor");

        ConditionalDoor conditionalDoor = config.getDoor(door.getDoorUID());
        if (conditionalDoor == null) throw CommandException.message("error.doorNotRegistered");
        return new Pair<>(conditionalDoor, door);
    }

    /**
     * Completes a door name or id. Players complete the names of their own doors and the ids of others.
     *
     * @param sender sender of the command
     * @param name   partial input
     * @return matching door names and ids
     */
    @SuppressWarnings("unchecked")
    public List<String> getDoorCompletion(CommandSender sender, String name) {
        if (!(sender instanceof Player player)) {
            return Collections.singletonList("<" + localizer().getMessage("syntax.doorId") + ">");
        }
        List<String> doorNames;
        try {
            doorNames = (List<String>) C.PLUGIN_CACHE.get(player.getName() + "doors", () -> {
                if (player.hasPermission(Permissions.ACCESS_ALL)) {
                    return getDoors().stream()
                            .map(d -> d.getPlayerUUID().equals(player.getUniqueId())
                                    ? d.getName() : String.valueOf(d.getDoorUID()))
                            .toList();
                }
                return getDoors(player).stream()
                        .map(d -> d.getPermission() == 0 ? d.getName() : String.valueOf(d.getDoorUID()))
                        .toList();
            });
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer().getMessage("syntax.doorId") + ">");
        }
        return Completion.complete(name, new ArrayList<>(doorNames));
    }
}

package de.eldoria.bigdoorsopener.core.adapter;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.simplecommands.EldoCommand;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public abstract class BigDoorsAdapterCommand extends EldoCommand {
    private final Commander commander;
    private final BigDoors bigDoors;
    private final Server server = Bukkit.getServer();
    private final Localizer localizer;
    private final MessageSender messageSender;
    private final Config config;

    public BigDoorsAdapterCommand(BigDoors bigDoors, Config config) {
        this.bigDoors = bigDoors;
        commander = bigDoors.getCommander();
        this.config = config;
        this.localizer = BigDoorsOpener.localizer();
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    /**
     * Set the state of the door.
     *
     * @param open true if the door should be open.
     * @param door door to set the state
     * @return true if the door state was changed succesfully which is indicated by {@link BigDoors#toggleDoor(long)}
     */
    protected boolean setDoorState(boolean open, ConditionalDoor door) {
        if (commander.isDoorBusy(door.getDoorUID())) {
            return false;
        }
        if (isOpen(door) == open) return false;
        return bigDoors.toggleDoor(door.getDoorUID());
    }

    /**
     * Check if a door is currently considered as open.
     * Will use the {@link ConditionalDoor#openInverted(boolean)} method.
     *
     * @param door door to check.
     * @return true when the door is open.
     */
    protected boolean isOpen(ConditionalDoor door) {
        return door.openInverted(bigDoors.isOpen(door.getDoorUID()));
    }

    /**
     * Checks if the world of a door exists and is present in the plugin.
     *
     * @param door door to check
     * @return true if the door exits
     */
    protected boolean doorExists(ConditionalDoor door) {
        World world = server.getWorld(door.getWorld());
        if (world == null) {
            BigDoorsOpener.logger().info(localizer.getMessage("error.worldIsNull",
                    Replacement.create("DOOR_NAME", door.getDoorUID())));
            return false;
        }

        // Make sure that this door still exists on the doors plugin.
        return getDoor(door.getDoorUID()) != null;
    }

    /**
     * Checks if a door is available.
     * A door is considered available if its not open and not busy
     *
     * @param door door to check
     * @return true if the door is available
     */
    protected boolean isAvailableToOpen(ConditionalDoor door) {
        return !isOpen(door) && !isBusy(door) && isDoorLoaded(getDoor(door.getDoorUID()));
    }

    /**
     * Checks if the chunks of a door are loaded
     *
     * @param door door to check
     * @return true if chunks around the door are loaded
     */
    protected boolean isDoorLoaded(Door door) {
        return bigDoors.areChunksLoadedForDoor(door);
    }

    /**
     * Checks if a door is busy. A door is busy if its closing or opening at the moment.
     *
     * @param door door to check
     * @return true if the door is busy
     */
    protected boolean isBusy(ConditionalDoor door) {
        return commander.isDoorBusy(door.getDoorUID());
    }

    /**
     * Get the door with the specified id if the player is the owner of the door.
     *
     * @param uid uid of the door.
     * @return door with id if exitsts
     */
    @Nullable
    protected Door getDoor(Player player, long uid) {
        return commander.getDoor(player.getUniqueId(), uid);
    }

    /**
     * Get the door with the specified id.
     *
     * @param doorId uid of the door.
     * @return door with id if exitsts
     */
    @Nullable
    protected Door getDoor(long doorId) {
        return commander.getDoor(null, doorId);
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

    protected Localizer getLocalizer() {
        return localizer;
    }

    /**
     * Tries to find a door.
     * Will search for a door of the player.
     * If no door is found a search in all doors by id is performed.
     * If the player has the {@link Permissions#ACCESS_ALL} permission,
     * a door will be returned even when its not owned by the player.
     *
     * @param doorUID uid or name of the door.
     * @param player  player which performed the request.
     * @return door if the door exists and the player is allowed to access it. Otherwise null.
     */
    protected Door getPlayerDoor(String doorUID, Player player) {
        if (player == null) {
            // requester is console. should always have access to all doors.
            Door door = getDoor(doorUID);
            if (door == null) {
                messageSender.sendError(null, localizer.getMessage("error.doorNotFound"));
                return null;
            }
            return door;
        }

        // sender id not console. retrieve door of player.
        List<Door> doors = getDoors(player, doorUID);

        if (doors.isEmpty()) {
            // door is null. check if door exists anyway
            Door door = getDoor(doorUID);
            if (door == null) {
                messageSender.sendError(player, localizer.getMessage("error.doorNotFound"));
                return null;
            }
            // when the door exists and the player has access to all doors return it.
            if (player.hasPermission(Permissions.ACCESS_ALL)) {
                return door;
            } else {
                messageSender.sendError(player, localizer.getMessage("error.notYourDoor"));
            }
            return null;
        }

        if (doors.size() != 1) {
            messageSender.sendMessage(player, localizer.getMessage("error.ambiguousDoorName"));
            return null;
        }

        return doors.get(0);
    }

    /**
     * Returns the door from config or creates it if not in config.
     *
     * @param door   door to get.
     * @param player player which wants to get the door.
     * @return door or null if the door does not exist in BD.
     */
    protected ConditionalDoor getOrRegister(Door door, Player player) {
        World world = door.getWorld();

        if (world == null) {
            messageSender.sendError(player, localizer.getMessage("error.worldNotLoaded"));
            return null;
        }

        if (config.containsDoor(door.getDoorUID())) {
            return config.getDoor(door.getDoorUID());
        }

        Location max = door.getMaximum();
        Location min = door.getMinimum();
        Vector vector = new Vector(
                (max.getX() + min.getX()) / 2,
                (max.getY() + min.getY()) / 2,
                (max.getZ() + min.getZ()) / 2);

        ConditionalDoor conditionalDoor = config.computeDoorIfAbsent(door.getDoorUID(),
                doorId -> new ConditionalDoor(doorId, world.getName(), vector));

        Bukkit.getPluginManager().callEvent(new DoorRegisteredEvent(conditionalDoor));
        return conditionalDoor;
    }

    /**
     * Tries to find the door for a player.
     * Will call {@link #getPlayerDoor(String, Player)} to retrieve a door.
     * Will check if the door is already registered.
     *
     * @param doorUID uid or name of the door
     * @param player  player which requests this door.
     * @return door with conditional door or null if the door is not registered or the user has no access.
     */
    public Pair<ConditionalDoor, Door> getConditionalPlayerDoor(String doorUID, Player player) {
        Door door = getPlayerDoor(doorUID, player);
        if (door == null) {
            return null;
        }

        if (door.getPermission() > 1) {
            messageSender.sendError(player, localizer.getMessage("error.notYourDoor"));
            return null;
        }

        ConditionalDoor conditionalDoor = config.getDoor(door.getDoorUID());
        if (conditionalDoor == null) {
            messageSender.sendMessage(player, localizer.getMessage("error.doorNotRegistered"));
            return null;
        }
        return new Pair<>(conditionalDoor, door);
    }

    @SuppressWarnings("unchecked")
    public List<String> getDoorCompletion(CommandSender sender, String name) {
        Player player = getPlayerFromSender(sender);
        if (player == null) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }
        List<Door> doors;
        try {
            doors = (List<Door>) C.PLUGIN_CACHE.get("doors",
                    () -> {
                        List<Door> d = new ArrayList<>();
                        d.addAll(getDoors());
                        return d;
                    });
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }
        List<String> doorNames;
        try {
            doorNames = (List<String>) C.PLUGIN_CACHE.get(player.getName() + "doors",
                    () -> {
                        if (player.hasPermission(Permissions.ACCESS_ALL)) {
                            return doors.stream()
                                    .map(d -> d.getPlayerUUID().equals(player.getUniqueId())
                                            ? d.getName() : String.valueOf(d.getDoorUID()))
                                    .collect(Collectors.toList());
                        }

                        // Map door names for doors where the player is the creator and can use the door name
                        return getDoors(player).stream()
                                .map(d -> d.getPermission() == 0 ? d.getName() : String.valueOf(d.getDoorUID()))
                                .collect(Collectors.toList());
                    });
        } catch (
                ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }

        return ArrayUtil.startingWithInArray(name,
                doorNames.toArray(new String[0]))
                .collect(Collectors.toList());
    }
}


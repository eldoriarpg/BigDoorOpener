package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DoorList extends BigDoorsAdapterCommand {
    private final Config config;

    public DoorList(BigDoors bigDoors, Config config) {
        super(bigDoors, config);
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        Map<Long, ConditionalDoor> doors = config.getDoorMap();
        StringBuilder builder = new StringBuilder(localizer().getMessage("list.title")).append("\n");

        Player playerFromSender = getPlayerFromSender(sender);

        if (sender.hasPermission(Permissions.ACCESS_ALL)) {
            for (ConditionalDoor value : doors.values()) {
                Door door = getDoor(String.valueOf(value.getDoorUID()));
                builder.append(value.getDoorUID()).append(" | ")
                        .append("§6").append(door.getName()).append("§r")
                        .append(" (").append(door.getWorld().getName()).append(")\n");
            }
        } else {
            List<Door> registeredDoors = getDoors(playerFromSender, null)
                    .stream()
                    .filter(d -> doors.containsKey(d.getDoorUID()))
                    .collect(Collectors.toList());
            for (Door value : registeredDoors) {
                builder.append(value.getDoorUID()).append(" | ")
                        .append("§6").append(value.getName()).append("§r")
                        .append(" (").append(value.getWorld().getName()).append(")\n");
            }
        }

        messageSender().sendMessage(sender, builder.toString());
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

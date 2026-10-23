/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lists the registered doors. Players without {@link Permissions#ACCESS_ALL} only see their own doors.
 */
public class DoorList extends BigDoorsAdapterCommand {
    private final Config config;

    public DoorList(Plugin plugin, BigDoors bigDoors, Config config) {
        super(
                plugin,
                CommandMeta.builder("list").withPermission(Permissions.USE).build(),
                bigDoors,
                config);
        this.config = config;
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        Map<Long, ConditionalDoor> doors = config.getDoorMap();
        Collection<Door> visible;
        if (sender.hasPermission(Permissions.ACCESS_ALL) || !(sender instanceof Player player)) {
            visible = doors.keySet().stream()
                    .map(id -> getDoor(String.valueOf(id)))
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            visible = getDoors(player, null).stream()
                    .filter(d -> doors.containsKey(d.getDoorUID()))
                    .toList();
        }

        TextComponent.Builder list = Component.text()
                .append(Component.text(localizer().getMessage("list.title"), NamedTextColor.DARK_GREEN));
        for (Door door : visible) {
            World world = door.getWorld();
            list.append(Component.newline())
                    .append(Component.text(door.getDoorUID() + " | ", NamedTextColor.DARK_GREEN))
                    .append(Component.text(door.getName(), NamedTextColor.GOLD))
                    .append(Component.text(
                            " (" + (world == null ? "?" : world.getName()) + ")", NamedTextColor.DARK_GREEN));
        }
        sender.sendMessage(list.build());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        return Collections.emptyList();
    }
}

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
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.container.Pair;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Inverts the open state of a door, for doors which were created open.
 */
public class InvertOpen extends BigDoorsAdapterCommand {

    public InvertOpen(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, CommandMeta.builder("invertOpen")
                .withPermission(Permissions.USE)
                .addArgument("syntax.doorId", true)
                .build(), bigDoors, config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) throws CommandException {
        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args.asString(0), sender);
        door.first.invertOpen();
        messageSender().sendMessage(sender, "invertOpen.inverted");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        return Collections.emptyList();
    }
}

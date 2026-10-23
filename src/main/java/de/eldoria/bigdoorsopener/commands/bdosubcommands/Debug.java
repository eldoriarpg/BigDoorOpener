/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.command.AdvancedCommand;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.executor.ITabExecutor;
import de.eldoria.eldoutilities.debug.DebugSettings;
import de.eldoria.eldoutilities.debug.DebugUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Uploads a debug report for support.
 */
public class Debug extends AdvancedCommand implements ITabExecutor {

    public Debug(Plugin plugin) {
        super(
                plugin,
                CommandMeta.builder("debug").withPermission(Permissions.DEBUG).build());
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        DebugUtil.dispatchDebug(sender, plugin(), DebugSettings.DEFAULT);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        return Collections.emptyList();
    }
}

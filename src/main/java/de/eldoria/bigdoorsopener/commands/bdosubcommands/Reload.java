package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.simplecommands.EldoCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;

public class Reload extends EldoCommand {
    private final Config config;
    private final DoorChecker doorChecker;
    private final Plugin plugin;
    private final MessageSender messageSender;
    private final Localizer localizer;

    public Reload(Config config, DoorChecker doorChecker, Plugin plugin) {
        this.config = config;
        this.doorChecker = doorChecker;
        this.plugin = plugin;
        messageSender = BigDoorsOpener.getPluginMessageSender();
        localizer = BigDoorsOpener.localizer();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.RELOAD)) {
            return true;
        }

        config.reloadConfig();
        doorChecker.reload();
        plugin.onEnable();
        messageSender.sendMessage(sender, localizer.getMessage("reload.completed"));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

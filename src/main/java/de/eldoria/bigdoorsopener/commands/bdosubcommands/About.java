package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class About extends de.eldoria.eldoutilities.simplecommands.EldoCommand {
    private final Localizer localizer;
    private final Plugin plugin;
    private final MessageSender messageSender;

    public About(Plugin plugin) {
        this.localizer = BigDoorsOpener.localizer();
        this.plugin = plugin;
        messageSender = BigDoorsOpener.getPluginMessageSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PluginDescriptionFile descr = plugin.getDescription();
        String info = localizer.getMessage("about",
                Replacement.create("PLUGIN_NAME", "Big Doors Opener").addFormatting('b'),
                Replacement.create("AUTHORS", String.join(", ", descr.getAuthors())).addFormatting('b'),
                Replacement.create("VERSION", descr.getVersion()).addFormatting('b'),
                Replacement.create("WEBSITE", descr.getWebsite()).addFormatting('b'),
                Replacement.create("DISCORD", "https://discord.gg/JJdx3xe").addFormatting('b'));
        messageSender.sendMessage(sender, info);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

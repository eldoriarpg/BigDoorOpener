/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.eldoutilities.commands.command.AdvancedCommand;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.executor.ITabExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Lists the commands with their arguments.
 */
public class Help extends AdvancedCommand implements ITabExecutor {

    public Help(Plugin plugin) {
        super(plugin, CommandMeta.builder("help").build());
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        TextComponent.Builder help = Component.text()
                .append(MiniMessage.miniMessage().deserialize(localizer().getMessage("help.title"),
                                Placeholder.unparsed("plugin_name", plugin().getName()))
                        .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        entry(help, "setCondition", "<$syntax.doorId$> <$syntax.condition$> <$syntax.conditionValues$>");
        entry(help, "addCondition", "<$syntax.doorId$> <$syntax.condition$> <$syntax.conditionValues$>");
        entry(help, "removeCondition", "<$syntax.doorId$> <$syntax.condition$>");
        entry(help, "copyCondition", "<$syntax.sourceDoor$> <$syntax.targetDoor$> [$syntax.condition$]");
        entry(help, "cloneDoor", "<$syntax.sourceDoor$> <$syntax.targetDoor$>");
        entry(help, "giveKey", "<$syntax.doorId$> [$syntax.amount$] [$syntax.player$]");
        entry(help, "info", "<$syntax.doorId$>");
        entry(help, "unregister", "<$syntax.doorId$>");
        entry(help, "invertOpen", "<$syntax.doorId$>");
        entry(help, "setEvaluator", "<$syntax.doorId$> <$syntax.evaluationType$> [$syntax.customEvaluator$]");
        entry(help, "stayOpen", "<$syntax.doorId$> <$syntax.seconds$>");
        entry(help, "setState", "<$syntax.doorId$> <enabled|disabled>");
        entry(help, "list", "");
        entry(help, "reload", "");
        entry(help, "debug", "");
        entry(help, "about", "");
        entry(help, "help", "");
        sender.sendMessage(help.build());
    }

    private void entry(TextComponent.Builder help, String command, String syntax) {
        help.append(Component.newline())
                .append(Component.text(command, NamedTextColor.GOLD));
        if (!syntax.isEmpty()) {
            help.append(Component.text(" " + ConditionInput.localize(localizer(), syntax), NamedTextColor.AQUA));
        }
        help.append(Component.newline())
                .append(Component.text("  " + localizer().getMessage("help." + command), NamedTextColor.DARK_GREEN));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        return Collections.emptyList();
    }
}

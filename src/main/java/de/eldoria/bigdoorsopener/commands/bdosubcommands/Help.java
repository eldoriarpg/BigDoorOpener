package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.simplecommands.EldoCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class Help extends EldoCommand {
    private final Plugin plugin;
    private final BukkitAudiences bukkitAudiences;

    public Help(Plugin plugin) {
        super(BigDoorsOpener.localizer(), BigDoorsOpener.getPluginMessageSender());
        this.plugin = plugin;
        bukkitAudiences = BukkitAudiences.create(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        TextComponent component = TextComponent.builder()
                .append(TextComponent.builder(localizer().getMessage("help.title",
                        Replacement.create("PLUGIN_NAME", plugin.getDescription().getName())))
                        .style(Style.builder().decoration(TextDecoration.BOLD, true).color(TextColors.GOLD).build()).build())
                .append(TextComponent.newline())
                .append(TextComponent.builder("setCondition ")
                        .style(Style.builder()
                                .color(TextColors.GOLD)
                                .decoration(TextDecoration.BOLD, false)
                                .build()))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + "> <"
                        + localizer().getMessage("syntax.condition") + "> <"
                        + localizer().getMessage("syntax.conditionValues") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.setCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("removeCondition")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + "> <"
                        + localizer().getMessage("syntax.condition") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.removeCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("copyCondition")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.sourceDoor") + "> <"
                        + localizer().getMessage("syntax.targetDoor") + "> ["
                        + localizer().getMessage("syntax.condition") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.copyCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("cloneDoor")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.sourceDoor") + "> <"
                        + localizer().getMessage("syntax.targetDoor") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.cloneDoor"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("giveKey")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + "> ["
                        + localizer().getMessage("syntax.amount") + "] ["
                        + localizer().getMessage("syntax.player") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.giveKey"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("info")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.info"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("unregister")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.unregister"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("invertOpen").color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.invertOpen"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("setEvaluator")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + "> <"
                        + localizer().getMessage("syntax.evaluationType") + "> ["
                        + localizer().getMessage("syntax.customEvaluator") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.setEvaluator"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("stayOpen")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer().getMessage("syntax.doorId") + "> <"
                        + localizer().getMessage("syntax.seconds") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.stayOpen"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("list")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.list"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("reload")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.reload"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("about")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.about"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("help")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer().getMessage("help.help"))
                        .color(TextColors.DARK_GREEN))
                .build();

        bukkitAudiences.sender(sender).sendMessage(component);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

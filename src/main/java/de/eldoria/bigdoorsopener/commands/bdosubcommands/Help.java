package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.simplecommands.EldoCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class Help extends EldoCommand {
	private final Localizer localizer;
	private final Plugin plugin;
	private final BukkitAudiences bukkitAudiences;

	public Help(Plugin plugin) {
		this.plugin = plugin;
		this.localizer = BigDoorsOpener.localizer();
		bukkitAudiences = BukkitAudiences.create(plugin);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		Component component = Component.text()
				.append(Component.text(localizer.getMessage("help.title",
						Replacement.create("PLUGIN_NAME", plugin.getDescription().getName())), NamedTextColor.GOLD, TextDecoration.BOLD))
				.append(Component.newline())
				.append(Component.text("setCondition ", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + "> <"
						+ localizer.getMessage("syntax.condition") + "> <"
						+ localizer.getMessage("syntax.conditionValues") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.setCondition"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("removeCondition", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + "> <"
						+ localizer.getMessage("syntax.condition") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.removeCondition"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("copyCondition", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.sourceDoor") + "> <"
						+ localizer.getMessage("syntax.targetDoor") + "> ["
						+ localizer.getMessage("syntax.condition") + "]", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.copyCondition"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("cloneDoor", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.sourceDoor") + "> <"
						+ localizer.getMessage("syntax.targetDoor") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.cloneDoor"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("giveKey", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + "> ["
						+ localizer.getMessage("syntax.amount") + "] ["
						+ localizer.getMessage("syntax.player") + "]", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.giveKey"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("info", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.info"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("unregister", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.unregister"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("invertOpen", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.invertOpen"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("setEvaluator", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + "> <"
						+ localizer.getMessage("syntax.evaluationType") + "> ["
						+ localizer.getMessage("syntax.customEvaluator") + "]", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.setEvaluator"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("stayOpen", NamedTextColor.GOLD))
				.append(Component.text(" <" + localizer.getMessage("syntax.doorId") + "> <"
						+ localizer.getMessage("syntax.seconds") + ">", NamedTextColor.AQUA))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.stayOpen"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("list", NamedTextColor.GOLD))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.list"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("reload", NamedTextColor.GOLD))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.reload"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("about", NamedTextColor.GOLD))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.about"), NamedTextColor.DARK_GREEN))
				.append(Component.newline())
				.append(Component.text("help", NamedTextColor.GOLD))
				.append(Component.newline())
				.append(Component.text("  " + localizer.getMessage("help.help"), NamedTextColor.DARK_GREEN))
				.build();

		bukkitAudiences.sender(sender).sendMessage(component);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return Collections.emptyList();
	}
}

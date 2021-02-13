package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Replacement;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class Unregister extends BigDoorsAdapterCommand {
	private final Config config;

	public Unregister(BigDoors bigDoors, Config config) {
		super(bigDoors, config);
		this.config = config;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (denyAccess(sender, Permissions.USE)) {
			return true;
		}

		if (argumentsInvalid(sender, args, 1, "<$syntax.doorId$>")) {
			return true;
		}

		Player playerFromSender = getPlayerFromSender(sender);

		Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);

		if (door == null) {
			return true;
		}

		config.removeDoor(door.second.getDoorUID());

		messageSender().sendLocalizedMessage(sender, "unregister.message",
				Replacement.create("DOOR_NAME", door.second.getName()).addFormatting('6'));
		return true;

	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (args.length == 1) {
			return getDoorCompletion(sender, args[0]);
		}
		return Collections.emptyList();
	}
}

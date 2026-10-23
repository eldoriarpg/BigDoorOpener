/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.container.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the settings and conditions of a door with buttons to edit and remove them.
 */
public class Info extends BigDoorsAdapterCommand {

    public Info(Plugin plugin, BigDoors bigDoors, Config config) {
        super(
                plugin,
                CommandMeta.builder("info")
                        .withPermission(Permissions.USE)
                        .addArgument("syntax.doorId", true)
                        .build(),
                bigDoors,
                config);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args)
            throws CommandException {
        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args.asString(0), sender);

        ConditionalDoor cDoor = door.first;
        TextComponent.Builder component = Component.text()
                .append(Component.text(door.second.getName() + " ", C.highlightColor, TextDecoration.BOLD))
                .append(Component.text("(Id:" + door.second.getDoorUID() + ") ", C.highlightColor, TextDecoration.BOLD))
                .append(Component.text(localizer().getMessage("info.info"), C.baseColor, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(localizer().getMessage("info.state") + " ", C.baseColor))
                .append(Component.text(
                        localizer().getMessage(cDoor.isEnabled() ? "info.state.enabled" : "info.state.disabled"),
                        C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer().getMessage("info.world") + " ", C.baseColor))
                .append(Component.text(cDoor.world(), C.highlightColor))
                .append(Component.newline());

        component.append(Component.text(localizer().getMessage("info.evaluator") + " ", C.baseColor));
        if (cDoor.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            component
                    .append(Component.text(cDoor.evaluator() + " ", C.highlightColor))
                    .append(editButton("/bdo setEvaluator " + cDoor.doorUID() + " custom " + cDoor.evaluator()));
        } else {
            component.append(Component.text(cDoor.evaluationType().name(), C.highlightColor));
        }
        component.append(Component.newline());

        component
                .append(Component.text(localizer().getMessage("info.stayOpen") + " ", C.baseColor))
                .append(Component.text(cDoor.stayOpen() + " ", C.highlightColor))
                .append(editButton("/bdo stayOpen " + cDoor.doorUID() + " " + cDoor.stayOpen()))
                .append(Component.newline());

        component.append(
                Component.text(localizer().getMessage("info.conditions"), C.highlightColor, TextDecoration.BOLD));

        Map<String, Integer> groupCount = new HashMap<>();

        for (DoorCondition condition : cDoor.conditionBag().getConditions()) {
            String group = ConditionRegistrar.getContainerByClass(condition.getClass())
                    .map(ConditionContainer::getGroup)
                    .orElse("undefined");
            int id = groupCount.compute(group, (k, v) -> v == null ? 0 : v + 1);
            component
                    .append(Component.newline())
                    .append(Component.text(id + " | ", NamedTextColor.AQUA))
                    .append(condition.getDescription(localizer()))
                    .append(Component.newline())
                    .append(Component.text(
                                    "[" + localizer().getMessage("info.remove") + "]",
                                    NamedTextColor.DARK_RED,
                                    TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.runCommand(condition.getRemoveCommand(cDoor) + " " + id)))
                    .append(Component.text(" "))
                    .append(editButton(condition.getCreationCommand(cDoor)));
        }

        sender.sendMessage(component.build());
    }

    private Component editButton(String command) {
        return Component.text(
                        "[" + localizer().getMessage("info.edit") + "]",
                        NamedTextColor.GREEN,
                        TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.suggestCommand(command));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull Arguments args) {
        if (args.sizeIs(1)) {
            return getDoorCompletion(sender, args.asString(0));
        }
        return Collections.emptyList();
    }
}

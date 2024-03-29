/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Info extends BigDoorsAdapterCommand {
    private final BukkitAudiences bukkitAudiences;

    public Info(BigDoors bigDoors, Plugin plugin, Config config) {
        super(bigDoors, config);
        bukkitAudiences = BukkitAudiences.create(plugin);
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

        ConditionalDoor cDoor = door.first;
        TextComponent.Builder component = Component.text()
                .append(Component.text(door.second.getName() + " ", C.highlightColor, TextDecoration.BOLD))
                .append(Component.text("(Id:" + door.second.getDoorUID() + ") ", C.highlightColor, TextDecoration.BOLD))
                .append(Component.text(localizer().getMessage("info.info"), C.baseColor, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(localizer().getMessage("info.state") + " ", C.baseColor))
                .append(Component.text(localizer().getMessage(cDoor.isEnabled() ? "info.state.enabled" : "info.state.disabled"), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer().getMessage("info.world") + " ", C.baseColor))
                .append(Component.text(cDoor.world(), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(""));

        // append evaluator
        component.append(Component.text(localizer().getMessage("info.evaluator") + " ", C.baseColor));
        if (cDoor.evaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            component.append(Component.text(cDoor.evaluator() + " ", C.highlightColor))
                    .append(Component.text("[" + localizer().getMessage("info.edit") + "]", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setEvaluator " + cDoor.doorUID() + " custom " + cDoor.evaluator())));
        } else {
            component.append(Component.text(cDoor.evaluationType().name(), C.highlightColor));
        }
        component.append(Component.newline());

        // append open time
        component.append(Component.text(localizer().getMessage("info.stayOpen") + " ", C.baseColor))
                .append(Component.text(cDoor.stayOpen() + " ", C.highlightColor))
                .append(Component.text("[" + localizer().getMessage("info.edit") + "]", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.suggestCommand("/bdo stayOpen " + cDoor.doorUID() + " " + cDoor.stayOpen())))
                .append(Component.newline());

        // start of key list
        component.append(Component.text(localizer().getMessage("info.conditions"), C.highlightColor, TextDecoration.BOLD));

        ConditionBag conditionBag = cDoor.conditionBag();

        Map<String, Integer> groupCount = new HashMap<>();

        for (DoorCondition condition : conditionBag.getConditions()) {
            Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(condition.getClass());
            Integer id = groupCount.compute(containerByClass.get().getGroup(), (k, v) -> v == null ? 0 : v + 1);
            component.append(Component.newline())
                    .append(Component.text(id + " | ", NamedTextColor.AQUA))
                    .append(condition.getDescription(localizer()))
                    .append(Component.newline())
                    .append(Component.text("[" + localizer().getMessage("info.remove") + "]", NamedTextColor.DARK_RED, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.runCommand(condition.getRemoveCommand(cDoor) + " " + id)))
                    .append(Component.text(" "))
                    .append(Component.text("[" + localizer().getMessage("info.edit") + "]", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.suggestCommand(condition.getCreationCommand(cDoor))));
        }

        bukkitAudiences.sender(sender).sendMessage(component.build());
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

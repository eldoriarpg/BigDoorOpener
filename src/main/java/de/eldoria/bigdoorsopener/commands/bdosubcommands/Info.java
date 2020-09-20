package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapterCommand;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
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
import java.util.List;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.denyAccess;
import static de.eldoria.bigdoorsopener.commands.CommandHelper.getPlayerFromSender;

public class Info extends BigDoorsAdapterCommand {
    private final Localizer localizer;
    private final BukkitAudiences bukkitAudiences;

    public Info(BigDoors bigDoors, Plugin plugin, Config config) {
        super(bigDoors, config);
        this.localizer = BigDoorsOpener.localizer();
        bukkitAudiences = BukkitAudiences.create(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (denyAccess(sender, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(sender, args, 1, "<" + localizer.getMessage("syntax.doorId") + ">")) {
            return true;
        }

        Player playerFromSender = getPlayerFromSender(sender);

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], playerFromSender);
        if (door == null) {
            return true;
        }

        ConditionalDoor cDoor = door.first;
        TextComponent.Builder builder = TextComponent.builder()
                .append(TextComponent.builder(door.second.getName() + " ").color(C.highlightColor).decoration(TextDecoration.BOLD, true))
                .append(TextComponent.builder("(Id:" + door.second.getDoorUID() + ") ").decoration(TextDecoration.BOLD, true)).color(C.highlightColor)
                .append(TextComponent.builder(localizer.getMessage("info.info")).color(C.baseColor).decoration(TextDecoration.BOLD, true))
                .append(TextComponent.newline()).decoration(TextDecoration.BOLD, false)
                .append(TextComponent.builder(localizer.getMessage("info.state") + " ").color(C.baseColor))
                .append(TextComponent.builder(localizer.getMessage(cDoor.isEnabled() ? "info.state.enabled" : "info.state.disabled")).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("info.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getWorld()).color(C.highlightColor))
                .append(TextComponent.newline()
                        .append(TextComponent.builder("")));

        // append evaluator
        builder.append(TextComponent.builder(localizer.getMessage("info.evaluator") + " ").color(C.baseColor));
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            builder.append(TextComponent.builder(cDoor.getEvaluator() + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                            .style(Style.builder().decoration(TextDecoration.UNDERLINED, true)
                                    .color(TextColors.GREEN).build())
                            .clickEvent(ClickEvent.suggestCommand("/bdo setEvaluator " + cDoor.getDoorUID() + " custom " + cDoor.getEvaluator())));
        } else {
            builder.append(TextComponent.builder(cDoor.getEvaluationType().name()).color(C.highlightColor));
        }
        builder.append(TextComponent.newline());

        // append open time
        builder.append(TextComponent.builder(localizer.getMessage("info.stayOpen") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getStayOpen() + " ").color(C.highlightColor))
                .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                        .style(Style.builder().decoration(TextDecoration.UNDERLINED, true)
                                .color(TextColors.GREEN).build())
                        .clickEvent(ClickEvent.suggestCommand("/bdo stayOpen " + cDoor.getDoorUID() + " " + cDoor.getStayOpen())))
                .append(TextComponent.newline());

        // start of key list
        builder.append(TextComponent.builder(localizer.getMessage("info.conditions"))
                .style(Style.builder().color(C.highlightColor).decoration(TextDecoration.BOLD, true).build()));

        ConditionBag conditionBag = cDoor.getConditionBag();

        for (DoorCondition condition : conditionBag.getConditions()) {
            builder.append(TextComponent.newline())
                    .append(condition.getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .style(Style.builder().color(TextColors.DARK_RED)
                                    .decoration(TextDecoration.UNDERLINED, true).build())
                            .clickEvent(ClickEvent.runCommand(condition.getRemoveCommand(cDoor))))
                    .append(TextComponent.builder(" "))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                            .style(Style.builder().color(TextColors.GREEN)
                                    .decoration(TextDecoration.UNDERLINED, true).build())
                            .clickEvent(ClickEvent.suggestCommand(condition.getCreationCommand(cDoor))));
        }

        bukkitAudiences.audience(sender).sendMessage(builder.build());
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

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * Opens the door when a JavaScript expression with PlaceholderAPI placeholders is true for a player.
 */
@JsonTypeName("placeholderCondition")
public class Placeholder implements DoorCondition {
    private final String evaluator;

    @JsonCreator
    public Placeholder(@JsonProperty("evaluator") String evaluator) {
        this.evaluator = evaluator;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Placeholder.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    if (!BigDoorsOpener.isPlaceholderEnabled())
                        throw CommandException.message("error.placeholderNotFound");
                    Player player = ConditionInput.player(sender);
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$syntax.customEvaluator$>");
                    String evaluator = String.join(" ", args.raw());
                    Pair<JsSyntaxHelper.ValidatorResult, String> result =
                            JsSyntaxHelper.checkExecution(evaluator, BigDoorsOpener.JS(), player, false);
                    switch (result.first) {
                        case UNBALANCED_PARENTHESIS -> throw CommandException.message("error.unbalancedParenthesis");
                        case INVALID_VARIABLE ->
                            throw CommandException.message(
                                    "error.invalidVariable", Replacement.unparsed("ERROR", result.second));
                        case INVALID_OPERATOR ->
                            throw CommandException.message(
                                    "error.invalidOperator", Replacement.unparsed("ERROR", result.second));
                        case INVALID_SYNTAX ->
                            throw CommandException.message(
                                    "error.invalidSyntax", Replacement.unparsed("ERROR", result.second));
                        case EXECUTION_FAILED ->
                            throw CommandException.message(
                                    "error.executionFailed", Replacement.unparsed("ERROR", result.second));
                        case NON_BOOLEAN_RESULT ->
                            throw CommandException.message(
                                    "error.nonBooleanResult", Replacement.unparsed("ERROR", result.second));
                        case FINE -> conditionBag.accept(new Placeholder(JsSyntaxHelper.translateEvaluator(evaluator)));
                    }
                    messageSender.sendMessage(sender, "setCondition.placeholder");
                })
                .onTabComplete((sender, localizer, args) ->
                        Collections.singletonList("<" + localizer.getMessage("syntax.customEvaluator") + ">"))
                .withMeta("placeholder", ConditionContainer.Builder.Cost.PLAYER_HIGH.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (BigDoorsOpener.isPlaceholderEnabled()) {
            return BigDoorsOpener.JS().eval(PlaceholderAPI.setPlaceholders(player, evaluator), null);
        }
        BigDoorsOpener.logger()
                .warning("A placeholder condition on door " + door.doorUID()
                        + " was called but PlaceholderAPI is not active.");
        return null;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.placeholder", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.evaluator") + " ", C.baseColor))
                .append(Component.text(evaluator, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " placeholder " + evaluator;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " placeholder";
    }

    @Override
    public Placeholder clone() {
        return new Placeholder(evaluator);
    }
}

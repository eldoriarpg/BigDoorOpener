package de.eldoria.bigdoorsopener.conditions.standalone;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A condition which uses the placeholder api.
 */
@SerializableAs("placeholderCondition")
public class Placeholder implements DoorCondition {

    private final String evaluator;

    public Placeholder(String evaluator) {
        this.evaluator = evaluator;
    }

    public Placeholder(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        evaluator = resolvingMap.getValue("evaluator");
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(Placeholder.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    if (!BigDoorsOpener.isPlaceholderEnabled()) {
                        messageSender.sendError(player, localizer.getMessage("error.placeholderNotFound"));
                        return;
                    }

                    if (player == null) {
                        messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                        return;
                    }

                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("syntax.customEvaluator") + ">")) {
                        return;
                    }

                    String evaluator = String.join(" ", arguments);

                    Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.checkExecution(evaluator, BigDoorsOpener.JS(), player, false);

                    switch (result.first) {
                        case UNBALANCED_PARENTHESIS:
                            messageSender.sendError(player, localizer.getMessage("error.unbalancedParenthesis"));
                            return;
                        case INVALID_VARIABLE:
                            messageSender.sendError(player, localizer.getMessage("error.invalidVariable",
                                    Replacement.create("ERROR", result.second).addFormatting('6')));
                            return;
                        case INVALID_OPERATOR:
                            messageSender.sendError(player, localizer.getMessage("error.invalidOperator",
                                    Replacement.create("ERROR", result.second).addFormatting('6')));
                            return;
                        case INVALID_SYNTAX:
                            messageSender.sendError(player, localizer.getMessage("error.invalidSyntax",
                                    Replacement.create("ERROR", result.second).addFormatting('6')));
                            return;
                        case EXECUTION_FAILED:
                            messageSender.sendError(player, localizer.getMessage("error.executionFailed",
                                    Replacement.create("ERROR", result.second).addFormatting('6')));
                            return;
                        case NON_BOOLEAN_RESULT:
                            messageSender.sendError(player, localizer.getMessage("error.nonBooleanResult",
                                    Replacement.create("ERROR", result.second).addFormatting('6')));
                            return;
                        case FINE:
                            conditionBag.accept(new Placeholder(JsSyntaxHelper.translateEvaluator(evaluator)));
                            break;
                    }

                    messageSender.sendMessage(player, localizer.getMessage("setCondition.placeholder"));

                })
                .onTabComplete((sender, localizer, args) -> Collections.singletonList("<" + localizer.getMessage("syntax.customEvaluator") + ">"))
                .withMeta("placeholder", ConditionContainer.Builder.Cost.PLAYER_HIGH.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (BigDoorsOpener.isPlaceholderEnabled()) {
            PlaceholderAPI.setPlaceholders(player, evaluator);
            return BigDoorsOpener.JS().eval(PlaceholderAPI.setPlaceholders(player, evaluator), null);
        }
        BigDoorsOpener.logger().warning("A placeholder condition on door " + door.getDoorUID() + " was called but PlaceholderAPI is not active.");
        return null;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.placeholder",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.evaluator") + " ", C.baseColor))
                .append(Component.text(evaluator, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " placeholder " + evaluator;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + " placeholder";
    }

    @Override
    public void evaluated() {

    }

    @Override
    public Placeholder clone() {
        return new Placeholder(evaluator);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("evaluator", evaluator)
                .build();
    }

}

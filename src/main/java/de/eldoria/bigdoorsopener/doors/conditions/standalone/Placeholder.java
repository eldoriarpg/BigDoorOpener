package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A condition which uses the placeholder api.
 */
@SerializableAs("placeholderCondition")
@ConditionScope(ConditionScope.Scope.PLAYER)
public class Placeholder implements DoorCondition {

    private final String evaluator;

    public Placeholder(String evaluator) {
        this.evaluator = evaluator;
    }

    public Placeholder(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        evaluator = resolvingMap.getValue("evaluator");
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
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.placeholder",
                        Replacement.create("NAME", ConditionType.PLACEHOLDER.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.evaluator") + " ").color(C.baseColor))
                .append(TextComponent.builder(evaluator).color(C.highlightColor))
                .build();
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

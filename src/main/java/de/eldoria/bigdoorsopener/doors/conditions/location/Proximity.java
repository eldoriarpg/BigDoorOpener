package de.eldoria.bigdoorsopener.doors.conditions.location;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.functions.TriFunction;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.EnumUtil;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A condition which opens the door when the player is within a specific range of defined by geometric form
 */
@SerializableAs("proximityCondition")
public class Proximity implements Location {
    private final Vector dimensions;
    private final ProximityForm proximityForm;


    public Proximity(Vector dimensions, ProximityForm proximityForm) {
        this.dimensions = dimensions;
        this.proximityForm = proximityForm;
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return proximityForm.check.apply(door.getPosition(), player.getLocation().toVector(), dimensions);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.proximity",
                        Replacement.create("NAME", ConditionType.PROXIMITY.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.size") + " ").color(C.baseColor))
                .append(TextComponent.builder(dimensions.toString()).color(C.highlightColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.proximityForm") + " ").color(C.baseColor))
                .append(TextComponent.builder(localizer.getMessage(proximityForm.localKey)).color(C.highlightColor))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return COMMAND + door.getDoorUID() + " proximity "
                + dimensions.getX() + "," + dimensions.getY() + "," + dimensions.getZ()
                + " " + proximityForm.name().toLowerCase();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("dimensions", dimensions)
                .add("proximityForm", proximityForm)
                .build();
    }

    public static Proximity deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        Vector vector = resolvingMap.getValue("dimensions");
        String formString = resolvingMap.getValue("proximityForm");
        formString = formString.replaceAll("(?i)elipsoid", "ellipsoid");
        ProximityForm proximityForm = EnumUtil.parse(formString, ProximityForm.class);
        return new Proximity(vector, proximityForm);
    }

    public enum ProximityForm {
        CUBOID("conditionDesc.proximityForm.cuboid",
                (point, target, dimensions) -> {
                    if (Math.abs(point.getX() - target.getX()) > dimensions.getX()) return false;
                    if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
                    if (Math.abs(point.getZ() - target.getZ()) > dimensions.getZ()) return false;
                    return true;
                }),
        ELLIPSOID("conditionDesc.proximityForm.ellipsoid",
                (point, target, dimensions) ->
                        Math.pow((target.getX() - point.getX()) / dimensions.getX(), 2)
                                + Math.pow((target.getY() - point.getY()) / dimensions.getY(), 2)
                                + Math.pow((target.getZ() - point.getZ()) / dimensions.getZ(), 2) <= 1),
        CYLINDER("conditionDesc.proximityForm.cylinder",
                (point, target, dimensions) -> {
                    if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
                    return Math.pow(target.getX() - point.getX(), 2) / Math.pow(dimensions.getX(), 2)
                            + Math.pow(target.getZ() - point.getZ(), 2) / Math.pow(dimensions.getZ(), 2) <= 1;
                });

        public final String localKey;
        public TriFunction<Vector, Vector, Vector, Boolean> check;

        ProximityForm(String localKey, TriFunction<Vector, Vector, Vector, Boolean> check) {
            this.localKey = localKey;
            this.check = check;
        }
    }
}

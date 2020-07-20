package de.eldoria.bigdoorsopener.doors.conditions.location;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SerializableAs("simpleRegionCondition")
public class SimpleRegion implements Location {
    private final BlockVector minimum;
    private final BlockVector maximum;
    private final String world;

    private final Cache<org.bukkit.Location, Boolean> cache = C.getExpiringCache(10, TimeUnit.SECONDS);

    public SimpleRegion(BlockVector first, BlockVector second, String world) {
        this.world = world;
        this.minimum = BlockVector.getMinimum(first, second).toBlockVector();
        this.maximum = BlockVector.getMaximum(first, second).toBlockVector();
    }

    public SimpleRegion(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        minimum = resolvingMap.getValue("minimum");
        maximum = resolvingMap.getValue("maximum");
        world = resolvingMap.getValue("world");
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return cache.get(player.getLocation(), () -> {
                Vector pos = player.getLocation().toVector();
                if (pos.getX() > maximum.getX() || pos.getX() < minimum.getX()) return false;
                if (pos.getY() > maximum.getY() || pos.getY() < minimum.getY()) return false;
                return !(pos.getZ() > maximum.getZ()) && !(pos.getZ() < minimum.getZ());

            });
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute value", e);
        }
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.simpleRegion",
                        Replacement.create("NAME", ConditionType.SIMPLE_REGION.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(world)).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.minPoint") + " ").color(C.baseColor))
                .append(TextComponent.builder(minimum.toString())).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.maxPoint") + " ").color(C.baseColor))
                .append(TextComponent.builder(maximum.toString())).color(C.highlightColor)
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " simpleRegion";
    }

    @Override
    public void evaluated() {

    }

    @Override
    public DoorCondition clone() {
        return new SimpleRegion(minimum, maximum, world);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("minimum", minimum)
                .add("maximum", maximum)
                .add("world", world)
                .build();
    }
}

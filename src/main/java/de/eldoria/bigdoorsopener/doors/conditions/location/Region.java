package de.eldoria.bigdoorsopener.doors.conditions.location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A condition which opens the door, when a player is inside a world guard region.
 */
@SerializableAs("regionCondition")
public class Region implements Location {
    private final ProtectedRegion region;
    private final World world;
    private final String worldName;
    private final String regionId;

    public Region(ProtectedRegion region, World world) {
        this.region = region;
        this.world = world;
        this.worldName = world.getName();
        this.regionId = region.getId();
    }

    private Region(World world, ProtectedRegion region, String regionId, String worldName) {
        this.world = world;
        this.region = region;
        this.worldName = worldName;
        this.regionId = regionId;
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (region == null) return null;
        return region.contains(BukkitAdapter.asBlockVector(player.getLocation()));
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.region",
                        Replacement.create("NAME", ConditionType.REGION.conditionName)))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.region")).color(C.highlightColor))
                .append(TextComponent.builder(regionId))
                .build();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("world", worldName)
                .add("region", regionId)
                .build();
    }

    public static Region deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        String worldName = resolvingMap.getValue("world");
        String regionName = resolvingMap.getValue("region");
        if (BigDoorsOpener.getRegionContainer() != null) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return new Region(null, null, regionName, worldName);
            }
            ProtectedRegion region = BigDoorsOpener.getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(regionName);
            return new Region(world, region, regionName, worldName);
        }
        BigDoorsOpener.logger().warning("A region key is used but world guard was not found.");
        return new Region(null, null, regionName, worldName);
    }
}

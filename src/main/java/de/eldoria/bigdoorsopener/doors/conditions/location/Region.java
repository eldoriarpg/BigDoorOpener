package de.eldoria.bigdoorsopener.doors.conditions.location;

import com.google.common.cache.Cache;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * A condition which opens the door, when a player is inside a world guard region.
 */
@SerializableAs("regionCondition")
public class Region implements Location {
    private final ProtectedRegion region;
    private final World world;
    private final String worldName;
    private final String regionId;

    private final Cache<org.bukkit.Location, Boolean> cache = C.getExpiringCache();

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

    public Region(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        worldName = resolvingMap.getValue("world");
        regionId = resolvingMap.getValue("region");
        if (BigDoorsOpener.getRegionContainer() != null) {
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                region = null;
                return;
            }
            region = BigDoorsOpener.getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(regionId);
            return;
        }
        world = null;
        region = null;
        BigDoorsOpener.logger().warning("A region key is used but world guard was not found.");
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (world != this.world) return false;
        if (region == null) return null;
        try {
            return cache.get(player.getLocation(), () ->
                    region.contains(BukkitAdapter.asBlockVector(player.getLocation()))
            );
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute value", e);
        }
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.region",
                        Replacement.create("NAME", ConditionType.REGION.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.region") + " ").color(C.baseColor))
                .append(TextComponent.builder(regionId)).color(C.highlightColor)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(worldName)).color(C.highlightColor)
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " region " + regionId;
    }

    @Override
    public void evaluated() {

    }

    @Override
    public Region clone() {
        return new Region(world, region, worldName, regionId);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("world", worldName)
                .add("region", regionId)
                .build();
    }
}

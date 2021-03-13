package de.eldoria.bigdoorsopener.conditions.worldlocation;

import com.google.common.cache.Cache;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A condition which opens the door, when a player is inside a world guard region.
 */
@SerializableAs("worldRegionCondition")
public class WorldRegion implements WorldLocation {
    private final ProtectedRegion region;
    private final World world;
    private final String worldName;
    private final String regionId;

    private final Cache<Location, Boolean> cache = C.getExpiringCache();

    public WorldRegion(ProtectedRegion region, World world) {
        this.region = region;
        this.world = world;
        this.worldName = world.getName();
        this.regionId = region.getId();
    }

    private WorldRegion(World world, ProtectedRegion region, String regionId, String worldName) {
        this.world = world;
        this.region = region;
        this.worldName = worldName;
        this.regionId = regionId;
    }

    public WorldRegion(Map<String, Object> map) {
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

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldRegion.class, Scope.WORLD)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    final RegionContainer regionContainer = BigDoorsOpener.getRegionContainer();
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    if (regionContainer == null) {
                        messageSender.sendError(player, localizer.getMessage("error.wgNotEnabled"));
                        return;
                    }

                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("tabcomplete.regionName") + ">")) {
                        return;
                    }

                    if (player == null) {
                        messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                        return;
                    }
                    RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                    if (rm == null) {
                        messageSender.sendError(player, localizer.getMessage("error.regionNotFound"));
                        return;
                    }
                    ProtectedRegion region = rm.getRegion(arguments[0]);
                    if (region == null) {
                        messageSender.sendError(player, localizer.getMessage("error.regionNotFound"));
                        return;
                    }
                    conditionBag.accept(new WorldRegion(region, player.getWorld()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.region"));

                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.length == 1) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.regionName") + ">");
                    }
                    return Collections.emptyList();
                })
                .withMeta("worldRegion", "worldLocation", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (world != this.world) return false;
        if (region == null) return null;
        for (Player worldPlayer : world.getPlayers()) {
            if (region.contains(BukkitAdapter.asBlockVector(worldPlayer.getLocation()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.region",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.region") + " ", C.baseColor))
                .append(Component.text(regionId, C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.world") + " ", C.baseColor))
                .append(Component.text(worldName, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " region " + regionId;
    }

    @Override
    public WorldRegion clone() {
        return new WorldRegion(world, region, worldName, regionId);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("world", worldName)
                .add("region", regionId)
                .build();
    }

}

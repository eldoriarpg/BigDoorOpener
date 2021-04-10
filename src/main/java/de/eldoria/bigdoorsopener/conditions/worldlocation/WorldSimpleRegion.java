package de.eldoria.bigdoorsopener.conditions.worldlocation;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.listener.registration.InteractionRegistrationObject;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SerializableAs("worldSimpleRegionCondition")
public class WorldSimpleRegion implements WorldLocation {
    private final BlockVector minimum;
    private final BlockVector maximum;
    private final String world;

    private final Cache<Location, Boolean> cache = C.getExpiringCache(10, TimeUnit.SECONDS);

    public WorldSimpleRegion(BlockVector first, BlockVector second, String world) {
        this.world = world;
        this.minimum = BlockVector.getMinimum(first, second).toBlockVector();
        this.maximum = BlockVector.getMaximum(first, second).toBlockVector();
    }

    public WorldSimpleRegion(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        minimum = resolvingMap.getValue("minimum");
        maximum = resolvingMap.getValue("maximum");
        world = resolvingMap.getValue("world");
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldSimpleRegion.class, Scope.WORLD)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.firstPoint"));
                    RegisterInteraction.getInstance().register(player, new InteractionRegistrationObject() {
                        private String world;
                        private BlockVector first;

                        @Override
                        public boolean invoke(PlayerInteractEvent event, MessageSender messageSender) {
                            if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
                                return false;
                            }
                            BlockVector vec = event.getClickedBlock().getLocation().toVector().toBlockVector();
                            if (first == null) {
                                world = event.getPlayer().getWorld().getName();
                                first = vec;
                                event.setCancelled(true);
                                messageSender.sendMessage(player, localizer.getMessage("setCondition.secondPoint"));
                                return false;
                            }
                            conditionBag.accept(new WorldSimpleRegion(first, vec, world));
                            event.setCancelled(true);
                            messageSender.sendMessage(player, localizer.getMessage("setCondition.simpleRegionRegisterd"));
                            return true;
                        }
                    });

                })
                .onTabComplete((sender, localizer, args) -> Collections.emptyList())
                .withMeta("worldSimpleRegion", "worldLocation", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        for (Player worldPlayer : world.getPlayers()) {
            Vector pos = player.getLocation().toVector();
            if (pos.getX() > maximum.getX() || pos.getX() < minimum.getX()) continue;
            if (pos.getY() > maximum.getY() || pos.getY() < minimum.getY()) continue;
            if (!(pos.getZ() > maximum.getZ()) && !(pos.getZ() < minimum.getZ())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.simpleRegion",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.world") + " ", C.baseColor))
                .append(Component.text(world, C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.minPoint") + " ", C.baseColor))
                .append(Component.text(minimum.toString(), C.highlightColor))
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.maxPoint") + " ", C.baseColor))
                .append(Component.text(maximum.toString(), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " simpleRegion";
    }

    @Override
    public WorldSimpleRegion clone() {
        return new WorldSimpleRegion(minimum, maximum, world);
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

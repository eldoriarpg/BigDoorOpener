/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.listener.registration.InteractionRegistrationObject;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Opens the door for players inside an area. The area is selected by clicking two blocks.
 */
@JsonTypeName("simpleRegionCondition")
public class SimpleRegion implements Location {
    private final BlockVector minimum;
    private final BlockVector maximum;
    private final String world;

    @JsonIgnore
    private final Cache<org.bukkit.Location, Boolean> cache = C.getExpiringCache(10, TimeUnit.SECONDS);

    public SimpleRegion(BlockVector first, BlockVector second, String world) {
        this.world = world;
        this.minimum = BlockVector.getMinimum(first, second).toBlockVector();
        this.maximum = BlockVector.getMaximum(first, second).toBlockVector();
    }

    @JsonCreator
    private SimpleRegion(
            @JsonProperty("minimum") Vector minimum,
            @JsonProperty("maximum") Vector maximum,
            @JsonProperty("world") String world) {
        this(minimum.toBlockVector(), maximum.toBlockVector(), world);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(SimpleRegion.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    Player player = ConditionInput.player(sender);
                    messageSender.sendMessage(player, "setCondition.firstPoint");
                    RegisterInteraction.getInstance().register(player, new InteractionRegistrationObject() {
                        private String world;
                        private BlockVector first;

                        @Override
                        public boolean invoke(PlayerInteractEvent event, MessageSender messageSender) {
                            if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) {
                                return false;
                            }
                            BlockVector vec = event.getClickedBlock()
                                    .getLocation()
                                    .toVector()
                                    .toBlockVector();
                            event.setCancelled(true);
                            if (first == null) {
                                world = event.getPlayer().getWorld().getName();
                                first = vec;
                                messageSender.sendMessage(player, "setCondition.secondPoint");
                                return false;
                            }
                            conditionBag.accept(new SimpleRegion(first, vec, world));
                            messageSender.sendMessage(player, "setCondition.simpleRegionRegisterd");
                            return true;
                        }
                    });
                })
                .onTabComplete((sender, localizer, args) -> Collections.emptyList())
                .withMeta("simpleRegion", "location", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return cache.get(player.getLocation(), () -> contains(player));
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute value", e);
        }
        return null;
    }

    private boolean contains(Player player) {
        if (!player.getWorld().getName().equals(world)) return false;
        Vector pos = player.getLocation().toVector();
        if (pos.getX() > maximum.getX() || pos.getX() < minimum.getX()) return false;
        if (pos.getY() > maximum.getY() || pos.getY() < minimum.getY()) return false;
        return !(pos.getZ() > maximum.getZ()) && !(pos.getZ() < minimum.getZ());
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.simpleRegion", getClass())
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
        return SET_COMMAND + door.doorUID() + " simpleRegion";
    }

    @Override
    public SimpleRegion clone() {
        return new SimpleRegion(minimum, maximum, world);
    }
}

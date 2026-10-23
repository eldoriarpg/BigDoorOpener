/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.worldlocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
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

/**
 * Opens the door while any player of the world is inside an area. The area is selected by clicking two blocks.
 */
@JsonTypeName("worldSimpleRegionCondition")
public class WorldSimpleRegion implements WorldLocation {
    private final BlockVector minimum;
    private final BlockVector maximum;
    private final String world;

    public WorldSimpleRegion(BlockVector first, BlockVector second, String world) {
        this.world = world;
        this.minimum = BlockVector.getMinimum(first, second).toBlockVector();
        this.maximum = BlockVector.getMaximum(first, second).toBlockVector();
    }

    @JsonCreator
    private WorldSimpleRegion(@JsonProperty("minimum") Vector minimum, @JsonProperty("maximum") Vector maximum, @JsonProperty("world") String world) {
        this(minimum.toBlockVector(), maximum.toBlockVector(), world);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(WorldSimpleRegion.class, Scope.WORLD)
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
                            BlockVector vec = event.getClickedBlock().getLocation().toVector().toBlockVector();
                            event.setCancelled(true);
                            if (first == null) {
                                world = event.getPlayer().getWorld().getName();
                                first = vec;
                                messageSender.sendMessage(player, "setCondition.secondPoint");
                                return false;
                            }
                            conditionBag.accept(new WorldSimpleRegion(first, vec, world));
                            messageSender.sendMessage(player, "setCondition.simpleRegionRegisterd");
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
            if (contains(worldPlayer)) return true;
        }
        return false;
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
        return SET_COMMAND + door.doorUID() + " worldSimpleRegion";
    }

    @Override
    public WorldSimpleRegion clone() {
        return new WorldSimpleRegion(minimum, maximum, world);
    }
}

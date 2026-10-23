/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Opens the door when a MythicMobs mob of a type died since the last check.
 */
@JsonTypeName("mythicMobsCondition")
public class MythicMob implements DoorCondition {
    private final String mobType;

    @JsonIgnore
    private boolean state;

    @JsonCreator
    public MythicMob(@JsonProperty("mobType") String mobType) {
        this.mobType = mobType;
    }

    @SuppressWarnings("unchecked")
    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(MythicMob.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    if (!BigDoorsOpener.isMythicMobsEnabled()) throw CommandException.message("error.mythicMob");
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$syntax.mobType$>");
                    String mob = args.asString(0);
                    if (!MythicMobsAccess.exists(mob)) {
                        throw CommandException.message("error.invalidMob");
                    }
                    conditionBag.accept(new MythicMob(mob));
                    messageSender.sendMessage(sender, "setCondition.mythicMob");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (!BigDoorsOpener.isMythicMobsEnabled()) return Collections.emptyList();
                    if (args.sizeIs(1)) {
                        List<String> mythicMobs;
                        try {
                            mythicMobs = (List<String>) C.PLUGIN_CACHE.get("mythicMobs", MythicMobsAccess::mobTypes);
                        } catch (ExecutionException e) {
                            BigDoorsOpener.logger().log(Level.WARNING, "Could not build mob names.", e);
                            return Collections.emptyList();
                        }
                        return Completion.complete(args.asString(0), mythicMobs);
                    }
                    return Collections.emptyList();
                })
                .withMeta("mythicMob", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (BigDoorsOpener.isMythicMobsEnabled()) {
            return state;
        }
        BigDoorsOpener.logger()
                .warning("A mythic mobs condition on door " + door.doorUID()
                        + " was called but MythicMobs is not active.");
        return null;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.mythicMob", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.mythicMob") + " ", C.baseColor))
                .append(Component.text(mobType, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " mythicMob " + mobType;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " mythicMob";
    }

    @Override
    public void evaluated() {
        state = false;
    }

    @Override
    public MythicMob clone() {
        return new MythicMob(mobType);
    }

    /**
     * Called when a MythicMobs mob died.
     *
     * @param mobType internal name of the type of the mob
     */
    public void killed(String mobType) {
        if (this.mobType.equalsIgnoreCase(mobType)) {
            state = true;
        }
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;
import static de.eldoria.bigdoorsopener.conditions.DoorCondition.localizer;

@SerializableAs("mythicMobsCondition")
public class MythicMob implements DoorCondition {
    private final String mobType;
    private boolean state;

    public MythicMob(String mobType) {
        this.mobType = mobType;
    }

    public MythicMob(Map<String, Object> map) {
        mobType = SerializationUtil.mapOf(map).getValue("mobType");
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(MythicMob.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    if (!BigDoorsOpener.isMythicMobsEnabled()) {
                        messageSender.sendError(player, localizer().getMessage("error.mythicMob"));
                        return;
                    }

                    if (argumentsInvalid(player, messageSender, localizer(), arguments, 1,
                            "<" + localizer().getMessage("syntax.doorId") + "> <"
                                    + localizer().getMessage("syntax.condition") + "> <"
                                    + localizer().getMessage("syntax.mobType") + ">")) {
                        return;
                    }

                    String mob = arguments[0];

                    boolean exists = MythicMobs.inst().getAPIHelper().getMythicMob(mob) != null;

                    if (!exists) {
                        messageSender.sendError(player, localizer().getMessage("error.invalidMob"));
                        return;
                    }

                    conditionBag.accept(new MythicMob(mob));
                    messageSender.sendMessage(player, localizer().getMessage("setCondition.mythicMob"));

                })
                .onTabComplete((sender, localizer, args) -> {
                    if (!BigDoorsOpener.isMythicMobsEnabled()) return Collections.emptyList();
                    if (args.length == 1) {

                        List<String> mythicMobs;
                        try {
                            mythicMobs = (List<String>) C.PLUGIN_CACHE.get("mythicMobs", () -> MythicMobs.inst()
                                    .getMobManager().getMobTypes()
                                    .parallelStream()
                                    .map(m -> m.getInternalName())
                                    .collect(Collectors.toList()));
                        } catch (ExecutionException e) {
                            BigDoorsOpener.logger().log(Level.WARNING, "Could not build mob names.", e);
                            return Collections.emptyList();
                        }
                        return ArrayUtil.startingWithInArray(args[0], mythicMobs.toArray(new String[0])).collect(Collectors.toList());
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
        BigDoorsOpener.logger().warning("A mythic mobs condition on door " + door.doorUID() + " was called but MythicMobs is not active.");
        return null;
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer().getMessage("conditionDesc.type.mythicMob",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer().getMessage("conditionDesc.mythicMob") + " ", C.baseColor))
                .append(Component.text(mobType, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " mythicMobs " + mobType;
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

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("mobType", mobType)
                .build();
    }

    public void killed(MythicMobDeathEvent event, boolean availableToOpen) {
        if (event.getMob().getType().getInternalName().equalsIgnoreCase(mobType)) {
            state = true;
        }
    }
}

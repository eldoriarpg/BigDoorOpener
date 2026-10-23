/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.permission;

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
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.Completion;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Opens the door for players with at least an access level on the door in Big Doors.
 */
@JsonTypeName("doorPermissionCondition")
public class DoorPermission implements Permission {
    private static final List<String> LEVELS = List.of("owner", "editor", "user");
    private final int permissionLevel;

    @JsonIgnore
    private final Cache<UUID, Boolean> cache = C.getExpiringCache(30, TimeUnit.SECONDS);

    @JsonCreator
    public DoorPermission(@JsonProperty("permissionLevel") int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    /**
     * Parses an access level name.
     *
     * @param permission owner, editor or user
     * @return the level, or -1 for an unknown name
     */
    public static int parsePermissionLevel(String permission) {
        for (int level = 0; level < LEVELS.size(); level++) {
            if (LEVELS.get(level).equalsIgnoreCase(permission)) return level;
        }
        return -1;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(DoorPermission.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.doorPermission$>");
                    int level = DoorPermission.parsePermissionLevel(args.asString(0));
                    if (level < 0) throw CommandException.message("error.invalidAccessLevel");
                    conditionBag.accept(new DoorPermission(level));
                    messageSender.sendMessage(sender, "setCondition.doorPermission");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Completion.complete(args.asString(0), LEVELS);
                    }
                    return Collections.emptyList();
                })
                .withMeta("doorPerm", "permission", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return cache.get(player.getUniqueId(), () -> {
                Door d = BigDoorsOpener.getBigDoors().getCommander().getDoor(player.getUniqueId(), door.doorUID());
                if (d == null) return false;
                int permission = d.getPermission();
                return permission <= permissionLevel && permission >= 0;
            });
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Calucation of door ownership failed. Please report this.", e);
            return false;
        }
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.permission", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.doorPermission") + " ", C.baseColor))
                .append(Component.text(localizer.getMessage(getPermString()), C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        String level = permissionLevel >= 0 && permissionLevel < LEVELS.size() ? LEVELS.get(permissionLevel) : "";
        return SET_COMMAND + door.doorUID() + " doorPerm " + level;
    }

    @Override
    public DoorPermission clone() {
        return new DoorPermission(permissionLevel);
    }

    private String getPermString() {
        return switch (permissionLevel) {
            case 0 -> "conditionDesc.owner";
            case 1 -> "conditionDesc.editor";
            case 2 -> "conditionDesc.user";
            default -> "none";
        };
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * Opens the door for players with a permission.
 */
@JsonTypeName("permissionCondition")
public class PermissionNode implements Permission {
    private final String permission;

    @JsonCreator
    public PermissionNode(@JsonProperty("permission") String permission) {
        this.permission = permission;
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(PermissionNode.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    ConditionInput.require(args, 1, DoorCondition.localizer(), "<$tabcomplete.permissionNode$>");
                    conditionBag.accept(new PermissionNode(args.asString(0)));
                    messageSender.sendMessage(sender, "setCondition.permissionNode");
                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.sizeIs(1)) {
                        return Collections.singletonList(
                                "<" + localizer.getMessage("tabcomplete.permissionNode") + ">");
                    }
                    return Collections.emptyList();
                })
                .withMeta("permNode", "permission", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return player.hasPermission(permission);
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return DoorCondition.headline(localizer, "conditionDesc.type.permission", getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.permissionNode") + " ", C.baseColor))
                .append(Component.text(permission, C.highlightColor));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " permNode " + permission;
    }

    @Override
    public PermissionNode clone() {
        return new PermissionNode(permission);
    }
}

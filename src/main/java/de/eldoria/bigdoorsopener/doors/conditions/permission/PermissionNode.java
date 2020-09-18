package de.eldoria.bigdoorsopener.doors.conditions.permission;

import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A condition which opens the door, when a player has a specific permission.
 */
@SerializableAs("permissionCondition")
@ConditionScope(ConditionScope.Scope.PLAYER)
public class PermissionNode implements Permission {
    private final String permission;

    public PermissionNode(String permission) {
        this.permission = permission;
    }

    public PermissionNode(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        permission = resolvingMap.getValue("permission");
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return player.hasPermission(permission);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.permission",
                        Replacement.create("NAME", ConditionType.PERMISSION_NODE.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.permissionNode") + " ").color(C.baseColor))
                .append(TextComponent.builder(permission).color(C.highlightColor))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " permNode " + permission;
    }

    @Override
    public void evaluated() {

    }

    @Override
    public PermissionNode clone() {
        return new PermissionNode(permission);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("permission", permission)
                .build();
    }
}

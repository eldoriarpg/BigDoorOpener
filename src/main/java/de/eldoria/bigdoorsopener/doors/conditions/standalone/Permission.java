package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Permission implements DoorCondition {
    private final String permission;

    public Permission(String permission) {
        this.permission = permission;
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return player.hasPermission(permission);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.permission",
                        Replacement.create("NAME", ConditionType.PERMISSION.conditionName)))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.permission")).color(C.baseColor))
                .append(TextComponent.builder(permission).color(C.highlightColor))
                .build();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("permission", permission)
                .build();
    }

    public static Permission deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        String permission = resolvingMap.getValue("permission");
        return new Permission(permission);
    }
}

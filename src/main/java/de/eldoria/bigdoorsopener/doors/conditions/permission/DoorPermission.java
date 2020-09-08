package de.eldoria.bigdoorsopener.doors.conditions.permission;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.scheduler.BigDoorsAdapter;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.TextComponent;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A condition which opens the door, when a player has a access level for the door.
 */
@SerializableAs("doorPermissionCondition")
@ConditionScope(ConditionScope.Scope.PLAYER)
public class DoorPermission extends BigDoorsAdapter implements Permission {
    private final int permissionLevel;
    private final Cache<UUID, Boolean> cache = C.getExpiringCache(30, TimeUnit.SECONDS);

    public DoorPermission(int permissionLevel) {
        super(BigDoorsOpener.getBigDoors(), BigDoorsOpener.getLocalizer());
        this.permissionLevel = permissionLevel;
    }

    public DoorPermission(Map<String, Object> map) {
        super(BigDoorsOpener.getBigDoors(), BigDoorsOpener.getLocalizer());
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        permissionLevel = resolvingMap.getValue("permissionLevel");
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        try {
            return cache.get(player.getUniqueId(), () -> {
                Door d = getDoor(player, door.getDoorUID());
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
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.permission",
                        Replacement.create("NAME", ConditionType.PERMISSION_NODE.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.doorPermission") + " ").color(C.baseColor))
                .append(TextComponent.builder(localizer.getMessage(getPermString())).color(C.highlightColor))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " doorPerm " + permissionLevel;
    }

    @Override
    public void evaluated() {

    }

    @Override
    public DoorPermission clone() {
        return new DoorPermission(permissionLevel);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("permissionLevel", permissionLevel)
                .build();
    }

    private String getPermString() {
        switch (permissionLevel) {
            case 0:
                return "conditionDesc.owner";
            case 1:
                return "conditionDesc.editor";
            case 2:
                return "conditionDesc.user";
            default:
                return "none";
        }
    }

    public static int parsePermissionLevel(String permission) {
        if ("owner".equalsIgnoreCase(permission)) {
            return 0;
        }
        if ("editor".equalsIgnoreCase(permission)) {
            return 1;
        }
        if ("user".equalsIgnoreCase(permission)) {
            return 2;
        }
        return -1;
    }
}
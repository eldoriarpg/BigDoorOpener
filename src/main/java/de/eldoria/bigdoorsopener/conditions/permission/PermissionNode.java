package de.eldoria.bigdoorsopener.conditions.permission;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A condition which opens the door, when a player has a specific permission.
 */
@SerializableAs("permissionCondition")
public class PermissionNode implements Permission {
    private final String permission;

    public PermissionNode(String permission) {
        this.permission = permission;
    }

    public PermissionNode(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        permission = resolvingMap.getValue("permission");
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(PermissionNode.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();

                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                                    + localizer.getMessage("syntax.condition") + "> <"
                                    + localizer.getMessage("tabcomplete.permissionNode") + ">")) {
                        return;
                    }

                    conditionBag.accept(new PermissionNode(arguments[0]));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.permissionNode"));

                })
                .onTabComplete((sender, localizer, args) -> {
                    if (args.length == 1) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.permissionNode") + ">");
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
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                localizer.getMessage("conditionDesc.type.permission",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.permissionNode") + " ", C.baseColor))
                .append(Component.text(permission, C.highlightColor));
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

package de.eldoria.bigdoorsopener.doors.conditions.standalone;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.ConditionScope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@ConditionScope(ConditionScope.Scope.WORLD)
public class MythicMob implements DoorCondition {
    private final String mobType;
    private boolean state;

    public MythicMob(String mobType) {
        this.mobType = mobType;
    }

    public MythicMob(Map<String, Object> map) {
        mobType = SerializationUtil.mapOf(map).getValue("mobType");
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (BigDoorsOpener.isPlaceholderEnabled()) {
            return state;
        }
        BigDoorsOpener.logger().warning("A mythic mobs condition on door " + door.getDoorUID() + " was called but MythicMobs is not active.");
        return null;
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.mythicMob",
                        Replacement.create("NAME", ConditionType.PERMISSION.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("conditionDesc.mythicMob") + " ").color(C.baseColor))
                .append(TextComponent.builder(mobType).color(C.highlightColor))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " mythicMobs " + mobType;
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.getDoorUID() + "mythicMobs";
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

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.cooldown;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A cooldown per player: a player who opened the door has to wait before opening it again. Other players are not
 * affected.
 */
@JsonTypeName("cooldownCondition")
public class PlayerCooldown extends Cooldown {
    @JsonIgnore
    private final Map<UUID, Instant> opened = new HashMap<>();

    @JsonCreator
    public PlayerCooldown(@JsonProperty("seconds") int seconds) {
        super(seconds);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(PlayerCooldown.class, Scope.PLAYER)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    int seconds = parseSeconds(args);
                    conditionBag.accept(new PlayerCooldown(seconds));
                    messageSender.sendMessage(sender, "setCondition.cooldown", Replacement.create("SECONDS", seconds));
                })
                .onTabComplete(Cooldown::complete)
                .withMeta("cooldown", "cooldown", ConditionContainer.Builder.Cost.PLAYER_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        return state(opened.get(player.getUniqueId()), currentState);
    }

    @Override
    public void opened(@Nullable Player player) {
        if (player != null) opened.put(player.getUniqueId(), Instant.now());
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return describe(localizer, "conditionDesc.type.cooldown");
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " cooldown " + seconds();
    }

    @Override
    public PlayerCooldown clone() {
        return new PlayerCooldown(seconds());
    }
}

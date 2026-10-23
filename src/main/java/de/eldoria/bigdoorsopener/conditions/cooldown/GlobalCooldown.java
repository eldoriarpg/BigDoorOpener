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

/**
 * A cooldown for the door: after the door was opened, nobody can open it again until the cooldown is over.
 */
@JsonTypeName("globalCooldownCondition")
public class GlobalCooldown extends Cooldown {
    @JsonIgnore
    private Instant opened;

    @JsonCreator
    public GlobalCooldown(@JsonProperty("seconds") int seconds) {
        super(seconds);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(GlobalCooldown.class, Scope.WORLD)
                .withFactory((sender, messageSender, args, conditionBag) -> {
                    int seconds = parseSeconds(args);
                    conditionBag.accept(new GlobalCooldown(seconds));
                    messageSender.sendMessage(
                            sender, "setCondition.globalCooldown", Replacement.create("SECONDS", seconds));
                })
                .onTabComplete(Cooldown::complete)
                .withMeta("globalCooldown", "globalCooldown", ConditionContainer.Builder.Cost.WORLD_LOW.cost)
                .build();
    }

    @Override
    public Boolean isOpen(@Nullable Player player, World world, ConditionalDoor door, boolean currentState) {
        return state(opened, currentState);
    }

    @Override
    public void opened(@Nullable Player player) {
        opened = Instant.now();
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        return describe(localizer, "conditionDesc.type.globalCooldown");
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " globalCooldown " + seconds();
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " globalCooldown";
    }

    @Override
    public GlobalCooldown clone() {
        return new GlobalCooldown(seconds());
    }
}

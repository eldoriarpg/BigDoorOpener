/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.door;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.eldoutilities.localization.ILocalizer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomEvaluatorTest {

    @BeforeAll
    static void register() {
        if (ConditionRegistrar.getContainerByClass(Gate.class).isEmpty()) {
            ConditionRegistrar.registerCondition(ConditionContainer.ofClass(Gate.class, Scope.WORLD)
                    .withFactory((sender, messageSender, args, bag) -> {})
                    .withMeta("gate", "gate", 1)
                    .build());
            ConditionRegistrar.registerCondition(ConditionContainer.ofClass(WorldGate.class, Scope.WORLD)
                    .withFactory((sender, messageSender, args, bag) -> {})
                    .withMeta("worldGate", "worldGate", 1)
                    .build());
        }
    }

    @Test
    void groupInsideLongerGroupNameStaysIntact() {
        ConditionBag bag = new ConditionBag(List.of(new Gate(true), new WorldGate(false)));

        assertEquals("false&&true", bag.custom("worldGate&&gate", null, null, door(), false));
    }

    @Test
    void allConditionsOfAGroupCount() {
        ConditionBag bag = new ConditionBag(List.of(new Gate(true), new Gate(false)));

        assertEquals("false", bag.custom("gate", null, null, door(), false));
    }

    @Test
    void conditionsWithoutOpinionAreSkipped() {
        ConditionBag bag = new ConditionBag(List.of(new Gate(null), new Gate(true)));

        assertEquals("true", bag.custom("gate", null, null, door(), false));
        assertEquals("null", new ConditionBag(List.of(new Gate(null))).custom("gate", null, null, door(), false));
    }

    @Test
    void missingGroupsAndCurrentStateAreReplaced() {
        ConditionBag bag = new ConditionBag(List.of(new Gate(true)));

        assertEquals("null||true", bag.custom("worldGate||currentState", null, null, door(), true));
    }

    private static ConditionalDoor door() {
        return new ConditionalDoor(1, "world", new Vector());
    }

    @JsonTypeName("testGateCondition")
    static class Gate implements DoorCondition {
        private final Boolean open;

        Gate(Boolean open) {
            this.open = open;
        }

        @Override
        public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
            return open;
        }

        @Override
        public Component getDescription(ILocalizer localizer) {
            return Component.empty();
        }

        @Override
        public String getCreationCommand(ConditionalDoor door) {
            return "";
        }

        @Override
        public String getRemoveCommand(ConditionalDoor door) {
            return "";
        }

        @Override
        public DoorCondition clone() {
            return new Gate(open);
        }
    }

    @JsonTypeName("testWorldGateCondition")
    static class WorldGate extends Gate {
        WorldGate(Boolean open) {
            super(open);
        }
    }
}

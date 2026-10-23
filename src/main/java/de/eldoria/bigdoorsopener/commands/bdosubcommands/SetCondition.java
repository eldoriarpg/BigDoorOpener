/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands.bdosubcommands;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.plugin.Plugin;

/**
 * Sets a condition of a door. Replaces the conditions of the same group.
 */
public class SetCondition extends ConditionCreationCommand {

    public SetCondition(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, "setCondition", bigDoors, config);
    }

    @Override
    protected void apply(ConditionalDoor door, DoorCondition condition) {
        door.conditionBag().setCondition(door, condition);
    }
}

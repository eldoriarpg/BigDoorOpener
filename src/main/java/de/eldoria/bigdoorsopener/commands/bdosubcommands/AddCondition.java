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
 * Adds a condition to a door. Keeps the conditions of the same group.
 */
public class AddCondition extends ConditionCreationCommand {

    public AddCondition(Plugin plugin, BigDoors bigDoors, Config config) {
        super(plugin, "addCondition", bigDoors, config);
    }

    @Override
    protected void apply(ConditionalDoor door, DoorCondition condition) {
        door.conditionBag().addCondition(door, condition);
    }
}

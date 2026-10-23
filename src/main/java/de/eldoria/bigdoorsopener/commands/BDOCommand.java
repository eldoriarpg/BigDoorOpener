/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.commands;

import de.eldoria.bigdoorsopener.commands.bdosubcommands.AddCondition;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.CloneDoor;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.CopyCondition;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.DoorList;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.GiveKey;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.Help;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.Info;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.InvertOpen;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.Reload;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.RemoveCondition;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.SetCondition;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.SetEvaluator;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.SetState;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.StayOpen;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.Unregister;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.commands.command.AdvancedCommand;
import de.eldoria.eldoutilities.commands.command.CommandMeta;
import de.eldoria.eldoutilities.commands.defaultcommands.DefaultAbout;
import de.eldoria.eldoutilities.commands.defaultcommands.DefaultDebug;
import nl.pim16aap2.bigDoors.BigDoors;

/**
 * The {@code /bigdoorsopener} command. Shows the help without a subcommand.
 */
public class BDOCommand extends AdvancedCommand {
    private static final String DISCORD = "https://discord.gg/JJdx3xe";

    public BDOCommand(BigDoorsOpener plugin, BigDoors doors, Config config) {
        super(plugin);
        Help help = new Help(plugin);
        meta(CommandMeta.builder("bigdoorsopener")
                .withDefaultCommand(help)
                .buildSubCommands((cmds, builder) -> {
                    cmds.add(help);
                    cmds.add(new DefaultAbout(plugin, DISCORD, "about"));
                    cmds.add(new CloneDoor(plugin, doors, config));
                    cmds.add(new CopyCondition(plugin, doors, config));
                    cmds.add(new GiveKey(plugin, doors, config));
                    cmds.add(new Info(plugin, doors, config));
                    cmds.add(new InvertOpen(plugin, doors, config));
                    cmds.add(new DoorList(plugin, doors, config));
                    cmds.add(new Reload(plugin));
                    cmds.add(new RemoveCondition(plugin, doors, config));
                    cmds.add(new SetCondition(plugin, doors, config));
                    cmds.add(new AddCondition(plugin, doors, config));
                    cmds.add(new SetEvaluator(plugin, doors, config));
                    cmds.add(new StayOpen(plugin, doors, config));
                    cmds.add(new Unregister(plugin, doors, config));
                    cmds.add(new SetState(plugin, doors, config));
                    cmds.add(new DefaultDebug(plugin, Permissions.RELOAD));
                })
                .build());
    }
}

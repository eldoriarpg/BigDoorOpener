package de.eldoria.bigdoorsopener.commands;

import com.google.common.cache.Cache;
import de.eldoria.bigdoorsopener.commands.bdosubcommands.About;
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
import de.eldoria.bigdoorsopener.core.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.simplecommands.EldoCommand;
import de.eldoria.eldoutilities.simplecommands.commands.DefaultDebug;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BDOCommand extends EldoCommand {
    private final Cache<String, List<?>> pluginCache = C.getExpiringCache(30, TimeUnit.SECONDS);

    public BDOCommand(BigDoorsOpener plugin, BigDoors doors, Config config, DoorChecker doorChecker) {
        super(plugin);
        BukkitAudiences bukkitAudiences = BukkitAudiences.create(plugin);
        Help help = new Help(plugin);
        setDefaultCommand(help);
        registerCommand("help", help);
        registerCommand("about", new About(plugin));
        registerCommand("cloneDoor", new CloneDoor(doors, config));
        registerCommand("copyCondition", new CopyCondition(doors, config));
        registerCommand("giveKey", new GiveKey(doors, config));
        registerCommand("info", new Info(doors, plugin, config));
        registerCommand("invertOpen", new InvertOpen(doors, config));
        registerCommand("list", new DoorList(doors, config));
        registerCommand("reload", new Reload(config, doorChecker, plugin));
        registerCommand("removeCondition", new RemoveCondition(doors, config));
        registerCommand("setCondition", new SetCondition(doors, config));
        registerCommand("addCondition", new AddCondition(doors, config));
        registerCommand("setEvaluator", new SetEvaluator(doors, config));
        registerCommand("stayOpen", new StayOpen(doors, config));
        registerCommand("unregister", new Unregister(doors, config));
        registerCommand("setState", new SetState(doors, config));
        registerCommand("debug", new DefaultDebug(plugin, Permissions.RELOAD));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!super.onCommand(sender, command, label, args)) {
            messageSender().sendError(sender, localizer().getMessage("error.invalidCommand"));
            return true;
        }
        return true;
    }
}

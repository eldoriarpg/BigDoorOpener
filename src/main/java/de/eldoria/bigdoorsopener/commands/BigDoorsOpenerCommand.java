package de.eldoria.bigdoorsopener.commands;

import com.google.common.cache.Cache;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionBag;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.doors.conditions.item.Item;
import de.eldoria.bigdoorsopener.doors.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.scheduler.BigDoorsAdapter;
import de.eldoria.bigdoorsopener.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.ArgumentHelper;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import de.eldoria.eldoutilities.utils.EnumUtil;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BigDoorsOpenerCommand extends BigDoorsAdapter implements TabExecutor {
    private static final CachingJSEngine ENGINE;
    // Tabcomplete utils
    private static final String[] CONDITION_TYPES;
    private static final String[] CONDITION_GROUPS;
    private static final String[] PROXIMITY_FORM;
    private static final String[] WEATHER_TYPE;
    private static final String[] EVALUATOR_TYPES;
    private final BigDoorsOpener plugin;
    private final Config config;
    private final Localizer localizer;
    private final DoorChecker doorChecker;
    private final MessageSender messageSender;
    private final RegisterInteraction registerInteraction;
    private final RegionContainer regionContainer;
    private final BukkitAudiences bukkitAudiences;

    private final Cache<String, List<?>> pluginCache = C.getExpiringCache(30, TimeUnit.SECONDS);

    static {
        ENGINE = BigDoorsOpener.JS();
        CONDITION_TYPES = Arrays.stream(ConditionType.values())
                .map(v -> v.conditionName)
                .toArray(String[]::new);
        CONDITION_GROUPS = Arrays.stream(ConditionType.ConditionGroup.values())
                .map(v -> v.name().toLowerCase().replace("_", ""))
                .toArray(String[]::new);
        PROXIMITY_FORM = Arrays.stream(Proximity.ProximityForm.values())
                .map(v -> v.name().toLowerCase())
                .toArray(String[]::new);
        WEATHER_TYPE = Arrays.stream(WeatherType.values())
                .map(v -> v.name().toLowerCase())
                .toArray(String[]::new);
        EVALUATOR_TYPES = Arrays.stream(ConditionalDoor.EvaluationType.values())
                .map(v -> v.name().toLowerCase())
                .toArray(String[]::new);
    }


    public BigDoorsOpenerCommand(BigDoorsOpener plugin, BigDoors doors, Config config, Localizer localizer,
                                 DoorChecker doorChecker, RegisterInteraction registerInteraction) {
        super(doors, localizer);
        this.plugin = plugin;
        this.config = config;
        this.localizer = localizer;
        messageSender = MessageSender.get(plugin);
        this.doorChecker = doorChecker;
        this.registerInteraction = registerInteraction;
        regionContainer = BigDoorsOpener.getRegionContainer();
        this.bukkitAudiences = BukkitAudiences.create(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0 || (args.length == 1 && "help".equalsIgnoreCase(args[0]))) {
            return help(sender);
        }

        String cmd = args[0];

        if ("about".equalsIgnoreCase(cmd)) {
            return about(player);
        }

        // TODO: Password door. Enter a password to open a door when a player approaches it. https://hub.spigotmc.org/javadocs/spigot/org/bukkit/conversations/Conversation.html

        String[] arguments = new String[0];
        if (args.length > 1) {
            arguments = Arrays.copyOfRange(args, 1, args.length);
        }

        // <keyType> <doorId>
        if ("setCondition".equalsIgnoreCase(cmd)) {
            return setCondition(player, arguments);
        }

        // remove a key from a door:
        // <keyType> <doorID>
        if ("removeCondition".equalsIgnoreCase(cmd)) {
            return removeCondition(player, arguments);
        }

        // <doorID> <doorID> [condition]
        if ("copyCondition".equalsIgnoreCase(cmd)) {
            return copyCondition(player, arguments);
        }

        // <doorID> <doorID> [condition]
        if ("cloneDoor".equalsIgnoreCase(cmd)) {
            return copyCondition(player, arguments);
        }

        //bdo givekey <doorId> [amount] [target]
        if ("giveKey".equalsIgnoreCase(cmd)) {
            return giveKey(player, arguments);
        }

        // bdo info
        if ("info".equalsIgnoreCase(cmd)) {
            return info(sender, arguments, player);
        }

        // bdo unregister <doorId>
        if ("unregister".equalsIgnoreCase(cmd)) {
            return unregister(arguments, player);
        }

        // bdo invertOpen <doorId>
        if ("invertOpen".equalsIgnoreCase(cmd)) {
            return invertOpen(arguments, player);
        }

        // bdo setEvaluator <doorId> <type> <custom val>
        if ("setEvaluator".equalsIgnoreCase(cmd)) {
            return setEvaluator(player, arguments);
        }

        //bdo stayOpen <doorId> <seconds>
        if ("stayOpen".equalsIgnoreCase(cmd)) {
            return stayOpen(player, arguments);
        }

        //bdo list
        if ("list".equalsIgnoreCase(cmd)) {
            return list(player);
        }

        //bdo reload
        if ("reload".equalsIgnoreCase(cmd)) {
            return reload(player);
        }
        messageSender.sendError(player, localizer.getMessage("error.invalidCommand"));
        return true;
    }

    // bdo help
    private boolean help(CommandSender sender) {
        TextComponent component = TextComponent.builder()
                .append(TextComponent.builder(localizer.getMessage("help.title",
                        Replacement.create("PLUGIN_NAME", plugin.getDescription().getName())))
                        .style(Style.builder().decoration(TextDecoration.BOLD, true).color(TextColors.GOLD).build()).build())
                .append(TextComponent.newline())
                .append(TextComponent.builder("setCondition ")
                        .style(Style.builder()
                                .color(TextColors.GOLD)
                                .decoration(TextDecoration.BOLD, false)
                                .build()))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.condition") + "> <"
                        + localizer.getMessage("syntax.conditionValues") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.setCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("removeCondition")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.condition") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.removeCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("copyCondition")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + "> ["
                        + localizer.getMessage("syntax.condition") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.copyCondition"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("cloneDoor")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.cloneDoor"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("giveKey")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + "> ["
                        + localizer.getMessage("syntax.amount") + "] ["
                        + localizer.getMessage("syntax.player") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.giveKey"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("info")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.info"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("unregister")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.unregister"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("invertOpen").color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.invertOpen"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("setEvaluator")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.evaluationType") + "> ["
                        + localizer.getMessage("syntax.customEvaluator") + "]")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.setEvaluator"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("stayOpen")
                        .color(TextColors.GOLD))
                .append(TextComponent.builder(" <" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.seconds") + ">")
                        .color(TextColors.AQUA))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.stayOpen"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("list")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.list"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("reload")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.reload"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("about")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.about"))
                        .color(TextColors.DARK_GREEN))
                .append(TextComponent.newline())
                .append(TextComponent.builder("help")
                        .color(TextColors.GOLD))
                .append(TextComponent.newline())
                .append(TextComponent.builder("  " + localizer.getMessage("help.help"))
                        .color(TextColors.DARK_GREEN))
                .build();

        bukkitAudiences.audience(sender).sendMessage(component);
        return true;
    }

    // bdo about
    private boolean about(Player player) {
        PluginDescriptionFile descr = plugin.getDescription();
        String info = localizer.getMessage("about",
                Replacement.create("PLUGIN_NAME", "Big Doors Opener").addFormatting('b'),
                Replacement.create("AUTHORS", String.join(", ", descr.getAuthors())).addFormatting('b'),
                Replacement.create("VERSION", descr.getVersion()).addFormatting('b'),
                Replacement.create("WEBSITE", descr.getWebsite()).addFormatting('b'),
                Replacement.create("DISCORD", "https://discord.gg/JJdx3xe").addFormatting('b'));
        messageSender.sendMessage(player, info);
        return true;
    }

    // bdo setCondition <doorId> <condition> <condition values>
    private boolean setCondition(Player player, String[] args) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, args, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.condition") + "> ["
                        + localizer.getMessage("syntax.conditionValues") + "]")) {
            return true;
        }

        Door playerDoor = getPlayerDoor(args[0], player);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor conditionalDoor = getOrRegister(playerDoor, player);

        if (conditionalDoor == null) {
            return true;
        }

        Optional<ConditionContainer> conditionByName = ConditionRegistrar.getConditionByName(args[1]);

        if (!conditionByName.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        ConditionContainer condition = conditionByName.get();

        String group = condition.getGroup();

        if (denyAccess(player, Permissions.getConditionPermission(condition),
                Permissions.ALL_CONDITION)) {
            return true;
        }

        String[] conditionArgs = new String[0];
        if (args.length > 2) {
            conditionArgs = Arrays.copyOfRange(args, 2, args.length);
        }

        condition.create(player, messageSender, conditionalDoor.getConditionBag(), conditionArgs);

        if (conditionalDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(group, Pattern.CASE_INSENSITIVE);
            if (!compile.matcher(conditionalDoor.getEvaluator()).find()) {
                messageSender.sendError(player, localizer.getMessage("warning.valueNotInEvaluator",
                        Replacement.create("VALUE", group).addFormatting('6')));
            }
        }

        config.safeConfig();
        return true;
    }

    // bdo removeCondition <doorId> <condition>
    private boolean removeCondition(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, arguments, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.condition") + ">")) {
            return false;
        }

        Door playerDoor = getPlayerDoor(arguments[0], player);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor cDoor = getOrRegister(playerDoor, player);

        if (cDoor == null) {
            return true;
        }

        Optional<ConditionGroup> optionalGroup = ConditionRegistrar.getConditionGroup(arguments[1]);

        if (!optionalGroup.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }
        ConditionGroup container = optionalGroup.get();

        String group = container.getName();

        if (denyAccess(player, Permissions.getConditionPermission(group), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag conditionBag = cDoor.getConditionBag();

        if (!conditionBag.isConditionSet(container)) {
            messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
            return true;
        } else {
            conditionBag.removeCondition(container);
        }

        messageSender.sendMessage(player, localizer.getMessage("removeCondition." + group));

        // check if condition is in evaluator if a custom evaluator is present.
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(group, Pattern.CASE_INSENSITIVE);
            if (compile.matcher(cDoor.getEvaluator()).find()) {
                messageSender.sendError(player, localizer.getMessage("warning.valueStillUsed",
                        Replacement.create("VALUE", group).addFormatting('6')));
            }
        }

        if (conditionBag.isEmpty()) {
            messageSender.sendMessage(player, localizer.getMessage("warning.chainIsEmpty"));
        }

        config.safeConfig();
        return true;
    }

    // bdo copyCondition <sourceDoor> <targetDoor> [condition]
    private boolean copyCondition(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, arguments, 2,
                "<" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + "> ["
                        + localizer.getMessage("syntax.condition") + "]")) {
            return true;
        }

        Door playerSourceDoor = getPlayerDoor(arguments[0], player);

        if (playerSourceDoor == null) {
            return true;
        }

        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor, player);

        if (sourceDoor == null) {
            return true;
        }

        Door playerTargetDoor = getPlayerDoor(arguments[1], player);

        if (playerTargetDoor == null) {
            return true;
        }

        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor, player);

        if (targetDoor == null) {
            return true;
        }

        ConditionBag sourceBag = sourceDoor.getConditionBag();

        if (arguments.length == 2) {
            if (denyAccess(player, Permissions.ALL_CONDITION)) {
                return true;
            }

            targetDoor.setConditionBag(sourceBag.copy());
            messageSender.sendMessage(player, localizer.getMessage("copyCondition.copiedAll",
                    Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                    Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
            config.safeConfig();
            return true;
        }

        Optional<ConditionGroup> optionalGroup = ConditionRegistrar.getConditionGroup(arguments[2]);

        if (!optionalGroup.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        ConditionGroup conditionGroup = optionalGroup.get();

        if (denyAccess(player, Permissions.getConditionPermission(conditionGroup.getName()), Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionBag targetBag = targetDoor.getConditionBag();

        Optional<DoorCondition> condition = sourceBag.getCondition(conditionGroup);

        if (!condition.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
            return true;
        }

        targetBag.putCondition(condition.get().clone());

        config.safeConfig();
        messageSender.sendMessage(player, localizer.getMessage("copyCondition.copiedSingle",
                Replacement.create("CONDITION", conditionGroup.getName()).addFormatting('6'),
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
        return true;
    }

    private boolean cloneDoor(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) return true;
        if (denyAccess(player, Permissions.ALL_CONDITION)) return true;

        if (argumentsInvalid(player, arguments, 2,
                "<" + localizer.getMessage("syntax.sourceDoor") + "> <"
                        + localizer.getMessage("syntax.targetDoor") + ">")) {
            return true;
        }

        Door playerSourceDoor = getPlayerDoor(arguments[0], player);

        if (playerSourceDoor == null) return true;

        ConditionalDoor sourceDoor = getOrRegister(playerSourceDoor, player);

        if (sourceDoor == null) return true;

        Door playerTargetDoor = getPlayerDoor(arguments[1], player);

        if (playerTargetDoor == null) return true;

        ConditionalDoor targetDoor = getOrRegister(playerTargetDoor, player);

        if (targetDoor == null) return true;

        targetDoor.setConditionBag(sourceDoor.getConditionBag().copy());

        targetDoor.setStayOpen(sourceDoor.getStayOpen());

        if (sourceDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            targetDoor.setEvaluator(sourceDoor.getEvaluator());
        } else {
            targetDoor.setEvaluator(sourceDoor.getEvaluationType());
        }

        targetDoor.setInvertOpen(sourceDoor.isInvertOpen());

        config.safeConfig();
        messageSender.sendMessage(player, localizer.getMessage("cloneDoor.message",
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
        return true;
    }

    // bdo giveKey <doorId> <amount> <player>
    private boolean giveKey(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (player == null) {
            if (argumentsInvalid(null, arguments, 3,
                    "<" + localizer.getMessage("syntax.doorId") + "> <"
                            + localizer.getMessage("syntax.amount") + "> <"
                            + localizer.getMessage("syntax.player") + ">")) {
                return true;
            }
        } else {
            if (argumentsInvalid(player, arguments, 1,
                    "<" + localizer.getMessage("syntax.doorId") + "> ["
                            + localizer.getMessage("syntax.amount") + "] ["
                            + localizer.getMessage("syntax.player") + "]")) {
                return true;
            }
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(arguments[0], player);

        if (door == null) {
            return true;
        }

        Optional<DoorCondition> condition = door.first.getConditionBag().getCondition("item");

        if (!condition.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.noItemConditionSet"));
            return true;
        }

        ItemStack item = ((Item) condition.get()).getItem();

        OptionalInt amount = ArgumentUtils.getOptionalParameter(arguments, 1, OptionalInt.of(64), Parser::parseInt);

        if (!amount.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
            return true;
        }

        Player target = ArgumentUtils.getOptionalParameter(arguments, 2, player, Bukkit::getPlayer);

        if (target == null) {
            messageSender.sendError(player, localizer.getMessage("error.playerNotFound"));
            return true;
        }

        ItemStack clone = item.clone();
        clone.setAmount(amount.getAsInt());
        target.getInventory().addItem(clone);
        if (target != player) {
            messageSender.sendMessage(player, localizer.getMessage("giveKey.send",
                    Replacement.create("AMOUNT", amount.getAsInt()),
                    Replacement.create("ITEMNAME", item.hasItemMeta() ? (item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : item.getType().name().toLowerCase())
                            : item.getType().name().toLowerCase()),
                    Replacement.create("TARGET", target.getDisplayName())));
        }
        messageSender.sendMessage(target, localizer.getMessage("giveKey.received",
                Replacement.create("AMOUNT", amount.getAsInt()),
                Replacement.create("ITEMNAME", item.hasItemMeta() ? (item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name().toLowerCase())
                        : item.getType().name().toLowerCase())));
        return true;
    }

    //bdo info <doorid>
    private boolean info(CommandSender sender, String[] args, Player player) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, args, 1, "<" + localizer.getMessage("syntax.doorId") + ">")) {
            return true;
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], player);
        if (door == null) {
            return true;
        }

        ConditionalDoor cDoor = door.first;
        TextComponent.Builder builder = TextComponent.builder()
                .append(TextComponent.builder(door.second.getName() + " ").color(C.highlightColor).decoration(TextDecoration.BOLD, true))
                .append(TextComponent.builder("(Id:" + door.second.getDoorUID() + ") ").decoration(TextDecoration.BOLD, true)).color(C.highlightColor)
                .append(TextComponent.builder(localizer.getMessage("info.info")).color(C.baseColor).decoration(TextDecoration.BOLD, true))
                .append(TextComponent.newline()).decoration(TextDecoration.BOLD, false)
                .append(TextComponent.builder(localizer.getMessage("info.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getWorld()).color(C.highlightColor))
                .append(TextComponent.newline()
                        .append(TextComponent.builder("")));

        // append evaluator
        builder.append(TextComponent.builder(localizer.getMessage("info.evaluator") + " ").color(C.baseColor));
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            builder.append(TextComponent.builder(cDoor.getEvaluator() + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                            .style(Style.builder().decoration(TextDecoration.UNDERLINED, true)
                                    .color(TextColors.GREEN).build())
                            .clickEvent(ClickEvent.suggestCommand("/bdo setEvaluator " + cDoor.getDoorUID() + " custom " + cDoor.getEvaluator())));
        } else {
            builder.append(TextComponent.builder(cDoor.getEvaluationType().name()).color(C.highlightColor));
        }
        builder.append(TextComponent.newline());

        // append open time
        builder.append(TextComponent.builder(localizer.getMessage("info.stayOpen") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getStayOpen() + " ").color(C.highlightColor))
                .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                        .style(Style.builder().decoration(TextDecoration.UNDERLINED, true)
                                .color(TextColors.GREEN).build())
                        .clickEvent(ClickEvent.suggestCommand("/bdo stayOpen " + cDoor.getDoorUID() + " " + cDoor.getStayOpen())))
                .append(TextComponent.newline());

        // start of key list
        builder.append(TextComponent.builder(localizer.getMessage("info.conditions"))
                .style(Style.builder().color(C.highlightColor).decoration(TextDecoration.BOLD, true).build()));

        ConditionBag conditionBag = cDoor.getConditionBag();

        for (DoorCondition condition : conditionBag.getConditions()) {
            builder.append(TextComponent.newline())
                    .append(condition.getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .style(Style.builder().color(TextColors.DARK_RED)
                                    .decoration(TextDecoration.UNDERLINED, true).build())
                            .clickEvent(ClickEvent.runCommand(condition.getRemoveCommand(cDoor))))
                    .append(TextComponent.builder(" "))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                            .style(Style.builder().color(TextColors.GREEN)
                                    .decoration(TextDecoration.UNDERLINED, true).build())
                            .clickEvent(ClickEvent.suggestCommand(condition.getCreationCommand(cDoor))));
        }

        bukkitAudiences.audience(sender).sendMessage(builder.build());
        return true;
    }

    //bdo unregister <doorid>
    private boolean unregister(String[] args, Player player) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, args, 1, "<" + localizer.getMessage("syntax.doorId") + ">")) {
            return true;
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], player);

        if (door == null) {
            return true;
        }

        config.getDoors().remove(door.second.getDoorUID());
        doorChecker.unregister(door.first);
        config.safeConfig();

        messageSender.sendMessage(player, localizer.getMessage("unregister.message",
                Replacement.create("DOOR_NAME", door.second.getName()).addFormatting('6')));
        return true;
    }

    //bdo invertOpen <doorid>
    private boolean invertOpen(String[] args, Player player) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, args, 1, "<" + localizer.getMessage("syntax.doorId") + ">")) {
            return true;
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[0], player);
        if (door == null) {
            return true;
        }

        door.first.invertOpen();
        messageSender.sendMessage(player, localizer.getMessage("invertOpen.inverted"));
        config.safeConfig();
        return true;
    }

    //bod setEvaluator <doorId> <type> [args]
    private boolean setEvaluator(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, arguments, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.evaluationType") + "> ["
                        + localizer.getMessage("syntax.customEvaluator") + "]")) {
            return true;
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(arguments[0], player);

        if (door == null) {
            return true;
        }

        ConditionalDoor.EvaluationType type = EnumUtil.parse(arguments[1], ConditionalDoor.EvaluationType.class, false);
        if (type == null) {
            messageSender.sendMessage(player, localizer.getMessage("error.invalidEvaluationType"));
            return true;
        }

        if (type != ConditionalDoor.EvaluationType.CUSTOM) {
            door.first.setEvaluator(type);
            if (type == ConditionalDoor.EvaluationType.AND) {
                messageSender.sendMessage(player, localizer.getMessage("setEvaluator.and"));
            } else {
                messageSender.sendMessage(player, localizer.getMessage("setEvaluator.or"));
            }
            config.safeConfig();
            return true;
        }

        if (denyAccess(player, Permissions.CUSTOM_EVALUATOR)) {
            return true;
        }

        if (arguments.length < 3) {
            messageSender.sendError(player, localizer.getMessage("error.noEvaluatorFound"));
            return true;
        }
        String evaluator = String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length));
        Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.validateEvaluator(evaluator, ENGINE);

        switch (result.first) {
            case UNBALANCED_PARENTHESIS:
                messageSender.sendError(player, localizer.getMessage("error.unbalancedParenthesis"));
                return true;
            case INVALID_VARIABLE:
                messageSender.sendError(player, localizer.getMessage("error.invalidVariable",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case INVALID_OPERATOR:
                messageSender.sendError(player, localizer.getMessage("error.invalidOperator",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case INVALID_SYNTAX:
                messageSender.sendError(player, localizer.getMessage("error.invalidSyntax",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case EXECUTION_FAILED:
                messageSender.sendError(player, localizer.getMessage("error.executionFailed",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case NON_BOOLEAN_RESULT:
                messageSender.sendError(player, localizer.getMessage("error.nonBooleanResult",
                        Replacement.create("ERROR", result.second).addFormatting('6')));
                return true;
            case FINE:
                door.first.setEvaluator(JsSyntaxHelper.translateEvaluator(evaluator));
                break;
        }
        config.safeConfig();
        messageSender.sendMessage(player, localizer.getMessage("setEvaluator.custom",
                Replacement.create("EVALUATOR", door.first.getEvaluator()).addFormatting('6')));
        return true;
    }

    // bdo stayOpen <doorId> <seconds>
    private boolean stayOpen(Player player, String[] arguments) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        if (argumentsInvalid(player, arguments, 2,
                "<" + localizer.getMessage("syntax.doorId") + "> <"
                        + localizer.getMessage("syntax.seconds") + ">")) {
            return true;
        }

        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(arguments[0], player);

        if (door == null) {
            return true;
        }

        OptionalInt optionalInt = Parser.parseInt(arguments[1]);
        if (!optionalInt.isPresent()) {
            messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
            return true;
        }
        door.first.setStayOpen(optionalInt.getAsInt());
        messageSender.sendMessage(player, localizer.getMessage("stayOpen.message",
                Replacement.create("SECONDS", optionalInt.getAsInt())));
        config.safeConfig();
        return true;
    }

    // bdo list
    private boolean list(Player player) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        Map<Long, ConditionalDoor> doors = config.getDoors();
        StringBuilder builder = new StringBuilder(localizer.getMessage("list.title")).append("\n");

        if (player.hasPermission(Permissions.ACCESS_ALL)) {
            for (ConditionalDoor value : doors.values()) {
                Door door = getDoor(String.valueOf(value.getDoorUID()), null);
                builder.append(value.getDoorUID()).append(" | ")
                        .append("§6").append(door.getName()).append("§r")
                        .append(" (").append(door.getWorld().getName()).append(")\n");
            }
        } else {
            List<Door> registeredDoors = getDoors(player, null)
                    .stream()
                    .filter(d -> doors.containsKey(d.getDoorUID()))
                    .collect(Collectors.toList());
            for (Door value : registeredDoors) {
                builder.append(value.getDoorUID()).append(" | ")
                        .append("§6").append(value.getName()).append("§r")
                        .append(" (").append(value.getWorld().getName()).append(")\n");
            }
        }

        messageSender.sendMessage(player, builder.toString());
        return true;
    }

    // bdo reload
    private boolean reload(Player player) {
        if (denyAccess(player, Permissions.RELOAD)) {
            return true;
        }

        config.reloadConfig();
        doorChecker.reload();
        plugin.onEnable();
        messageSender.sendMessage(player, localizer.getMessage("reload.completed"));
        return true;
    }

    /**
     * Returns the door from config or creates it if not in config.
     *
     * @param door   door to get.
     * @param player player which wants to get the door.
     * @return door or null if the door does not exist in BD.
     */
    private ConditionalDoor getOrRegister(Door door, Player player) {
        World world = door.getWorld();

        if (world == null) {
            messageSender.sendError(player, localizer.getMessage("error.worldNotLoaded"));
            return null;
        }

        if (config.getDoors().containsKey(door.getDoorUID())) {
            return config.getDoors().get(door.getDoorUID());
        }

        Location max = door.getMaximum();
        Location min = door.getMinimum();
        Vector vector = new Vector(
                (max.getX() + min.getX()) / 2,
                (max.getY() + min.getY()) / 2,
                (max.getZ() + min.getZ()) / 2);

        ConditionalDoor conditionalDoor = config.getDoors().computeIfAbsent(door.getDoorUID(),
                doorId -> new ConditionalDoor(doorId, world.getName(), vector));

        doorChecker.register(conditionalDoor);
        return conditionalDoor;
    }

    /**
     * Tries to find the door for a player.
     * Will call {@link #getPlayerDoor(String, Player)} to retrieve a door.
     * Will check if the door is already registered.
     *
     * @param doorUID uid or name of the door
     * @param player  player which requests this door.
     * @return door with conditional door or null if the door is not registered or the user has no access.
     */
    private Pair<ConditionalDoor, Door> getConditionalPlayerDoor(String doorUID, Player player) {
        Door door = getPlayerDoor(doorUID, player);
        if (door == null) {
            return null;
        }

        if (door.getPermission() > 1) {
            messageSender.sendError(player, localizer.getMessage("error.notYourDoor"));
            return null;
        }

        ConditionalDoor timedDoor = config.getDoors().get(door.getDoorUID());
        if (timedDoor == null) {
            messageSender.sendMessage(player, localizer.getMessage("error.doorNotRegistered"));
            return null;
        }
        return new Pair<>(timedDoor, door);
    }

    /**
     * Tries to find a door.
     * Will search for a door of the player.
     * If no door is found a search in all doors by id is performed.
     * If the player has the {@link Permissions#ACCESS_ALL} permission,
     * a door will be returned even when its not owned by the player.
     *
     * @param doorUID uid or name of the door.
     * @param player  player which performed the request.
     * @return door if the door exists and the player is allowed to access it. Otherwise null.
     */
    private Door getPlayerDoor(String doorUID, Player player) {
        if (player == null) {
            // requester is console. should always have access to all doors.
            Door door = getDoor(doorUID, null);
            if (door == null) {
                messageSender.sendError(null, localizer.getMessage("error.doorNotFound"));
                return null;
            }
            return door;
        }

        // sender id not console. retrieve door of player.
        List<Door> doors = getDoors(player, doorUID);

        if (doors.isEmpty()) {
            // door is null. check if door exists anyway
            Door door = getDoor(doorUID, null);
            if (door == null) {
                messageSender.sendError(player, localizer.getMessage("error.doorNotFound"));
                return null;
            }
            // when the door exists and the player has access to all doors return it.
            if (player.hasPermission(Permissions.ACCESS_ALL)) {
                return door;
            } else {
                messageSender.sendError(player, localizer.getMessage("error.notYourDoor"));
            }
            return null;
        }

        if (doors.size() != 1) {
            messageSender.sendMessage(player, localizer.getMessage("error.ambiguousDoorName"));
            return null;
        }

        return doors.get(0);
    }

    /**
     * Checks if the provided arguments are invalid.
     *
     * @param player player which executed the command.
     * @param args   arguments to check
     * @param length min amount of arguments.
     * @param syntax correct syntax
     * @return true if the arguments are invalid
     */
    private boolean argumentsInvalid(Player player, String[] args, int length, String syntax) {
        return ArgumentHelper.argumentsInvalid(player, messageSender, localizer, args, length, syntax);
    }

    private boolean denyAccess(CommandSender sender, String... permissions) {
        return denyAccess(sender, false, permissions);
    }

    private boolean denyAccess(CommandSender sender, boolean silent, String... permissions) {
        if (sender == null) {
            return false;
        }

        Player player = null;

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (player == null) {
            return false;
        }
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return false;
            }
        }
        if (!silent) {
            messageSender.sendMessage(player, localizer.getMessage("error.permission",
                    Replacement.create("PERMISSION", String.join(", ", permissions)).addFormatting('6')));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = args[0];

        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        if (args.length > 10) {
            return Collections.singletonList("(╯°□°）╯︵ ┻━┻");
        }

        if (args.length == 1) {
            return ArrayUtil.startingWithInArray(cmd,
                    new String[] {"help", "about",
                            "setCondition", "removeCondition", "copyCondition", "cloneDoor",
                            "info", "giveKey", "unregister", "invertOpen", "setEvaluator",
                            "stayOpen", "list", "reload"})
                    .collect(Collectors.toList());
        }

        if ("setCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], CONDITION_TYPES).collect(Collectors.toList());
            }

            Optional<ConditionContainer> conditionByName = ConditionRegistrar.getConditionByName(args[2]);

            if (!conditionByName.isPresent()) {
                return Collections.singletonList(localizer.getMessage("error.invalidConditionType"));
            }

            ConditionContainer container = conditionByName.get();

            if (denyAccess(sender, true, Permissions.getConditionPermission(container.getGroup()), Permissions.ALL_CONDITION)) {
                return Collections.singletonList(localizer.getMessage("error.permission",
                        Replacement.create("PERMISSION", Permissions.getConditionPermission(container.getGroup()) + ", " + Permissions.ALL_CONDITION)));
            }
        }

        if ("removeCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], CONDITION_GROUPS).collect(Collectors.toList());
            }
        }

        if ("copyCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return getDoorCompletion(player, args[2]);
            }
            if (args.length == 4) {
                return ArrayUtil.startingWithInArray(args[3], CONDITION_GROUPS).collect(Collectors.toList());
            }
        }

        if ("cloneDoor".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return getDoorCompletion(player, args[2]);
            }
        }

        if ("info".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
        }

        if ("unregister".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
        }

        if ("invertOpen".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
        }

        if ("setEvaluator".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], EVALUATOR_TYPES).collect(Collectors.toList());
            }

            ConditionalDoor.EvaluationType parse = EnumUtil.parse(args[2], ConditionalDoor.EvaluationType.class);

            if (parse == null) {
                return Collections.singletonList(localizer.getMessage("error.invalidEvaluationType"));
            }

            if (parse == ConditionalDoor.EvaluationType.CUSTOM) {
                if (denyAccess(sender, true, Permissions.CUSTOM_EVALUATOR)) {
                    return Collections.singletonList(localizer.getMessage("error.permission",
                            Replacement.create("PERMISSION", Permissions.CUSTOM_EVALUATOR)));
                }
                ArrayList<String> list = new ArrayList<>(Arrays.asList(CONDITION_GROUPS));
                list.add("<" + localizer.getMessage("tabcomplete.validValues") + ">");
                list.add("currentState");
                return list;
            }
        }

        if ("giveKey".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }

            if (args.length == 3) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
            }

            if (args.length == 4) {
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getName().toLowerCase().startsWith(args[3]))
                        .map(HumanEntity::getName)
                        .collect(Collectors.toList());
            }
        }

        if ("stayOpen".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return getDoorCompletion(player, args[1]);
            }
            if (args.length == 3) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
            }
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getDoorCompletion(Player player, String name) {
        if (player == null) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }
        List<Door> doors;
        try {
            doors = (List<Door>) pluginCache.get("doors",
                    () -> {
                        List<Door> d = new ArrayList<>();
                        d.addAll(getDoors());
                        return d;
                    });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }
        List<String> doorNames;
        try {
            doorNames = (List<String>) pluginCache.get(player.getName() + "doors",
                    () -> {
                        // Map door names for doors where the player is the creator and can use the door name
                        Map<Long, String> doorNamesMap = new HashMap<>();
                        doors.stream()
                                .filter(door -> door.getPlayerUUID().equals(player.getUniqueId()))
                                .forEach(d -> doorNamesMap.put(d.getDoorUID(), d.getName()));

                        List<String> result = new ArrayList<>(doorNamesMap.values());

                        // Add not owned doors as door ID if the player has the permission.
                        if (player.hasPermission(Permissions.ACCESS_ALL)) {
                            doors.stream()
                                    .filter(d -> !doorNamesMap.containsKey(d.getDoorUID()))
                                    .forEach(d -> result.add(String.valueOf(d.getDoorUID())));
                        }
                        return result;
                    });
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }
        List<String> doorNames;
        try {
            doorNames = (List<String>) pluginCache.get(player.getName() + "doors",
                    () -> {
                        if (player.hasPermission(Permissions.ACCESS_ALL)) {
                            return doors.stream()
                                    .map(d -> d.getPlayerUUID().equals(player.getUniqueId())
                                            ? d.getName() : String.valueOf(d.getDoorUID()))
                                    .collect(Collectors.toList());
                        }

                        // Map door names for doors where the player is the creator and can use the door name
                        return getDoors(player).stream()
                                .map(d -> d.getPermission() == 0 ? d.getName() : String.valueOf(d.getDoorUID()))
                                .collect(Collectors.toList());
                    });
        } catch (
                ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Could not build tab completion cache for door names.", e);
            return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
        }

        return ArrayUtil.startingWithInArray(name,
                doorNames.toArray(new String[0]))
                .collect(Collectors.toList());
    }
}

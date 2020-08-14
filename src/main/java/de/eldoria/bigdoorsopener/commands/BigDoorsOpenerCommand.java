package de.eldoria.bigdoorsopener.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionChain;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.doors.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.doors.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.doors.conditions.location.Region;
import de.eldoria.bigdoorsopener.doors.conditions.location.SimpleRegion;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.listener.registration.InteractionRegistrationObject;
import de.eldoria.bigdoorsopener.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.scheduler.DoorChecker;
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
import nl.pim16aap2.bigDoors.Commander;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BigDoorsOpenerCommand implements TabExecutor {
    private static final CachingJSEngine ENGINE;
    // Tabcomplete utils
    private static final String[] CONDITION_TYPES;
    private static final String[] CONDITION_GROUPS;
    private static final String[] PROXIMITY_FORM;
    private static final String[] WEATHER_TYPE;
    private static final String[] EVALUATOR_TYPES;
    private final BigDoorsOpener plugin;
    private final Commander commander;
    private final Config config;
    private final Localizer localizer;
    private final DoorChecker doorChecker;
    private final MessageSender messageSender;
    private final RegisterInteraction registerInteraction;
    private final RegionContainer regionContainer;
    private final BukkitAudiences bukkitAudiences;

    static {
        ENGINE = BigDoorsOpener.JS();
        CONDITION_TYPES = Arrays.stream(ConditionType.values())
                .map(v -> v.conditionName)
                .toArray(String[]::new);
        CONDITION_GROUPS = Arrays.stream(ConditionType.ConditionGroup.values())
                .map(v -> v.name().toLowerCase())
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


    public BigDoorsOpenerCommand(BigDoorsOpener plugin, Commander commander, Config config, Localizer localizer,
                                 DoorChecker doorChecker, RegisterInteraction registerInteraction) {
        this.plugin = plugin;
        this.commander = commander;
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

        ConditionType type = EnumUtil.parse(args[1], ConditionType.class, true);

        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        if (denyAccess(player, type.conditionGroup.permission, Permissions.ALL_CONDITION)) {
            return true;
        }

        ItemStack itemInMainHand = null;
        if (player != null) {
            itemInMainHand = player.getInventory().getItemInMainHand().clone();
        }

        ConditionChain conditionChain = conditionalDoor.getConditionChain();

        String[] conditionArgs = new String[0];
        if (args.length > 2) {
            conditionArgs = Arrays.copyOfRange(args, 2, args.length);
        }

        switch (type) {
            // <amount> <consumed>
            case ITEM_CLICK:
                // <amount> <consumed>
            case ITEM_BLOCK:
                // <amount> <consumed>
            case ITEM_HOLDING:
                // <amount> <consumed>
            case ITEM_OWNING:
                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }

                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("syntax.amount") + "> ["
                                + localizer.getMessage("tabcomplete.consumed") + "]")) {
                    return true;
                }

                // parse amount
                OptionalInt amount = Parser.parseInt(conditionArgs[0]);
                if (!amount.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                    return true;
                }

                if (amount.getAsInt() > 64 || amount.getAsInt() < 1) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                            Replacement.create("MIN", 1).addFormatting('6'),
                            Replacement.create("MAX", 64).addFormatting('6')));
                    return true;
                }

                Optional<Boolean> consume = ArgumentUtils.getOptionalParameter(conditionArgs, 1, Optional.of(false), Parser::parseBoolean);
                if (!consume.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;
                }

                itemInMainHand.setAmount(amount.getAsInt());
                if (type == ConditionType.ITEM_BLOCK) {
                    ItemBlock itemBlock = new ItemBlock(itemInMainHand, consume.get());
                    // Register Keyhole object at registration listener.
                    registerInteraction.register(player, event -> {
                        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                            return false;
                        }
                        if (event.getClickedBlock() == null) return false;
                        BlockVector blockVector = event.getClickedBlock().getLocation().toVector().toBlockVector();
                        itemBlock.setPosition(blockVector);
                        conditionalDoor.getConditionChain().setItem(itemBlock);
                        config.safeConfig();
                        event.setCancelled(true);
                        messageSender.sendMessage(player, localizer.getMessage("setCondition.itemBlockRegistered"));
                        return true;
                    });
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemblock"));
                } else if (type == ConditionType.ITEM_CLICK) {
                    conditionChain.setItem(new ItemClick(itemInMainHand, consume.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemClick"));
                } else if (type == ConditionType.ITEM_OWNING) {
                    conditionChain.setItem(new ItemOwning(itemInMainHand, consume.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemOwning"));
                } else {
                    conditionChain.setItem(new ItemHolding(itemInMainHand, consume.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemHolding"));
                }
                break;
            // <dimensions> <form>
            case PROXIMITY:
                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("tabcomplete.dimensions") + "> ["
                                + localizer.getMessage("syntax.proximityForm") + "]")) {
                    return true;
                }

                Vector vector;
                String[] coords = conditionArgs[0].split(",");

                // parse the size.
                if (coords.length == 1) {
                    OptionalDouble size = Parser.parseDouble(conditionArgs[0]);
                    if (!size.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidNumber"));
                        return true;
                    }
                    vector = new Vector(size.getAsDouble(), size.getAsDouble(), size.getAsDouble());
                } else if (coords.length == 3) {
                    OptionalDouble x = Parser.parseDouble(coords[0]);
                    OptionalDouble y = Parser.parseDouble(coords[1]);
                    OptionalDouble z = Parser.parseDouble(coords[2]);
                    if (x.isPresent() && y.isPresent() && z.isPresent()) {
                        vector = new Vector(x.getAsDouble(), y.getAsDouble(), z.getAsDouble());
                    } else {
                        messageSender.sendError(player, localizer.getMessage("error.invalidNumber"));
                        return true;
                    }
                } else {
                    messageSender.sendError(player, localizer.getMessage("error.invalidVector"));
                    return true;
                }

                // check if vector is inside bounds.
                if (vector.getX() < 1 || vector.getX() > 100
                        || vector.getY() < 1 || vector.getY() > 100
                        || vector.getZ() < 1 || vector.getZ() > 100) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                            Replacement.create("MIN", 1).addFormatting('6'),
                            Replacement.create("MAX", 100).addFormatting('6')));
                    return true;
                }

                Proximity.ProximityForm form = ArgumentUtils.getOptionalParameter(conditionArgs, 1, Proximity.ProximityForm.CUBOID, (s) -> EnumUtil.parse(s, Proximity.ProximityForm.class));

                if (form == null) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidForm"));
                    return true;
                }

                conditionChain.setLocation(new Proximity(vector, form));

                // TODO: display region

                messageSender.sendMessage(player, localizer.getMessage("setCondition.proximity"));
                break;
            // <regionName>
            case REGION:
                if (regionContainer == null) {
                    messageSender.sendError(player, localizer.getMessage("error.wgNotEnabled"));
                    return true;
                }

                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("tabcomplete.regionName") + ">")) {
                    return true;
                }

                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }
                RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                if (rm == null) {
                    messageSender.sendError(player, localizer.getMessage("error.regionNotFound"));
                    return true;
                }
                ProtectedRegion region = rm.getRegion(conditionArgs[0]);
                if (region == null) {
                    messageSender.sendError(player, localizer.getMessage("error.regionNotFound"));
                    return true;
                }
                conditionChain.setLocation(new Region(region, player.getWorld()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.region"));
                break;
            case SIMPLE_REGION:
                messageSender.sendMessage(player, localizer.getMessage("setCondition.firstPoint"));
                registerInteraction.register(player, new InteractionRegistrationObject() {
                    private String world;
                    private BlockVector first;

                    @Override
                    public boolean register(PlayerInteractEvent event) {
                        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
                            return false;
                        }
                        BlockVector vec = event.getClickedBlock().getLocation().toVector().toBlockVector();
                        if (first == null) {
                            world = event.getPlayer().getWorld().getName();
                            first = vec;
                            event.setCancelled(true);
                            messageSender.sendMessage(player, localizer.getMessage("setCondition.secondPoint"));
                            return false;
                        }
                        conditionalDoor.getConditionChain().setLocation(new SimpleRegion(first, vec, world));
                        config.safeConfig();
                        event.setCancelled(true);
                        messageSender.sendMessage(player, localizer.getMessage("setCondition.simpleRegionRegisterd"));
                        return true;
                    }
                });
                break;
            // permission
            case PERMISSION:
                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("tabcomplete.permission") + ">")) {
                    return true;
                }

                conditionChain.setPermission(new Permission(conditionArgs[0]));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.permission"));
                break;
            case TIME:
                if (argumentsInvalid(player, conditionArgs, 2,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("syntax.openTime") + "> <"
                                + localizer.getMessage("syntax.closeTime") + "> ["
                                + localizer.getMessage("tabcomplete.forceState") + "]")) {
                    return true;
                }

                // parse time
                OptionalInt open = Parser.parseInt(conditionArgs[0]);
                if (!open.isPresent()) {
                    open = Parser.parseTimeToTicks(conditionArgs[0]);
                    if (!open.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidOpenTime"));
                        return true;
                    }
                }

                OptionalInt close = Parser.parseInt(conditionArgs[1]);
                if (!close.isPresent()) {
                    close = Parser.parseTimeToTicks(conditionArgs[1]);
                    if (!close.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidCloseTime"));
                        return true;
                    }
                }

                if (close.getAsInt() < 0 || close.getAsInt() > 24000
                        || open.getAsInt() < 0 || open.getAsInt() > 24000) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                            Replacement.create("MIN", 0).addFormatting('6'),
                            Replacement.create("MAX", 24000).addFormatting('6')));
                    return true;
                }

                // parse optional force argument.
                Optional<Boolean> force = ArgumentUtils.getOptionalParameter(conditionArgs, 2, Optional.of(false), Parser::parseBoolean);

                if (!force.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;
                }
                conditionChain.setTime(new Time(open.getAsInt(), close.getAsInt(), force.get()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.time",
                        Replacement.create("OPEN", Parser.parseTicksToTime(open.getAsInt())),
                        Replacement.create("CLOSE", Parser.parseTicksToTime(close.getAsInt()))));
                break;
            case WEATHER:
                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("syntax.weatherType") + ">")) {
                    return true;
                }

                WeatherType weatherType = null;
                for (WeatherType value : WeatherType.values()) {
                    if (value.name().equalsIgnoreCase(conditionArgs[0])) {
                        weatherType = value;
                    }
                }
                if (weatherType == null) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidWeatherType"));
                    return true;
                }

                Optional<Boolean> forceWeather = ArgumentUtils.getOptionalParameter(conditionArgs, 1, Optional.of(false), Parser::parseBoolean);

                if (!forceWeather.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;
                }

                conditionChain.setWeather(new Weather(weatherType, forceWeather.get()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.weather",
                        Replacement.create("OPEN", weatherType == WeatherType.CLEAR
                                ? localizer.getMessage("conditionDesc.clear")
                                : localizer.getMessage("conditionDesc.downfall"))));
                break;
            case PLACEHOLDER:
                if (!BigDoorsOpener.isPlaceholderEnabled()) {
                    messageSender.sendError(player, localizer.getMessage("error.placeholderNotFound"));
                    return true;
                }

                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }

                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("syntax.customEvaluator") + ">")) {
                    return true;
                }

                String evaluator = String.join(" ", conditionArgs);

                Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.checkExecution(evaluator, BigDoorsOpener.JS(), player, false);

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
                        conditionChain.setPlaceholder(new Placeholder(JsSyntaxHelper.translateEvaluator(evaluator)));
                        break;
                }

                messageSender.sendMessage(player, localizer.getMessage("setCondition.placeholder"));
                break;
            case MYTHIC_MOBS:
                if (!BigDoorsOpener.isMythicMobsEnabled()) {
                    messageSender.sendError(player, localizer.getMessage("error.mythicMob"));
                    return true;
                }

                if (argumentsInvalid(player, conditionArgs, 1,
                        "<" + localizer.getMessage("syntax.doorId") + "> <"
                                + localizer.getMessage("syntax.condition") + "> <"
                                + localizer.getMessage("syntax.mobType") + ">")) {
                    return true;
                }

                String mob = conditionArgs[0];

                boolean exists = MythicMobs.inst().getAPIHelper().getMythicMob(mob) != null;

                if (!exists) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidMob"));
                    return true;
                }

                conditionChain.setCondition(ConditionType.ConditionGroup.MYTHIC_MOB, new MythicMob(mob));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.mythicMob"));
                break;
            default:
                messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
                return true;
        }

        // check if condition is in evaluator if a custom evaluator is present.
        if (conditionalDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(type.conditionGroup.conditionParameter, Pattern.CASE_INSENSITIVE);
            if (!compile.matcher(conditionalDoor.getEvaluator()).find()) {
                messageSender.sendError(player, localizer.getMessage("warning.valueNotInEvaluator",
                        Replacement.create("VALUE", type.conditionGroup.conditionParameter).addFormatting('6')));
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

        ConditionType.ConditionGroup type = EnumUtil.parse(arguments[1], ConditionType.ConditionGroup.class, true);
        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        if (denyAccess(player, type.permission, Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionChain conditionChain = cDoor.getConditionChain();

        if (conditionChain.getCondition(type) == null) {
            messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
            return true;
        } else {
            conditionChain.removeCondition(type);
        }

        switch (type) {
            case ITEM:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.item"));
                break;
            case LOCATION:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.location"));
                break;
            case PERMISSION:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.permission"));
                break;
            case TIME:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.time"));
                break;
            case WEATHER:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.weather"));
                break;
            case PLACEHOLDER:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.placeholder"));
                break;
            case MYTHIC_MOB:
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.mythicMob"));
                break;
        }

        // check if condition is in evaluator if a custom evaluator is present.
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(type.conditionParameter, Pattern.CASE_INSENSITIVE);
            if (compile.matcher(cDoor.getEvaluator()).find()) {
                messageSender.sendError(player, localizer.getMessage("warning.valueStillUsed",
                        Replacement.create("VALUE", type.conditionParameter).addFormatting('6')));
            }
        }

        if (conditionChain.isEmpty()) {
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

        ConditionChain sourceChain = sourceDoor.getConditionChain();

        if (arguments.length == 2) {
            if (denyAccess(player, Permissions.ALL_CONDITION)) {
                return true;
            }

            targetDoor.setConditionChain(sourceChain.copy());
            messageSender.sendMessage(player, localizer.getMessage("copyCondition.copiedAll",
                    Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                    Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
            config.safeConfig();
            return true;
        }

        ConditionType.ConditionGroup type = EnumUtil.parse(arguments[2], ConditionType.ConditionGroup.class);

        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        if (denyAccess(player, type.permission, Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionChain targetChain = targetDoor.getConditionChain();

        DoorCondition condition = sourceChain.getCondition(type);

        if (condition == null) {
            messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
            return true;
        }

        targetChain.setCondition(type, sourceChain.getCondition(type));

        config.safeConfig();
        messageSender.sendMessage(player, localizer.getMessage("copyCondition.copiedSingle",
                Replacement.create("CONDITION", type.conditionParameter).addFormatting('6'),
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

        targetDoor.setConditionChain(sourceDoor.getConditionChain().copy());

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

        if (door.first.getConditionChain().getItem() == null) {
            messageSender.sendError(player, localizer.getMessage("error.noItemConditionSet"));
            return true;
        }

        ItemStack item = door.first.getConditionChain().getItem().getItem();

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

        ConditionChain conditionChain = cDoor.getConditionChain();

        for (Pair<DoorCondition, ConditionType.ConditionGroup> condition : conditionChain.getConditionsWrapped()) {
            builder.append(TextComponent.newline());
            if (condition.first != null) {
                builder.append(condition.first.getDescription(localizer))
                        .append(TextComponent.newline())
                        .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                                .style(Style.builder().color(TextColors.DARK_RED)
                                        .decoration(TextDecoration.UNDERLINED, true).build())
                                .clickEvent(ClickEvent.runCommand(condition.first.getRemoveCommand(cDoor))))
                        .append(TextComponent.builder(" "))
                        .append(TextComponent.builder("[" + localizer.getMessage("info.edit") + "]")
                                .style(Style.builder().color(TextColors.GREEN)
                                        .decoration(TextDecoration.UNDERLINED, true).build())
                                .clickEvent(ClickEvent.suggestCommand(condition.first.getCreationCommand(cDoor))));
            } else {
                builder.append(TextComponent.builder(localizer.getMessage(condition.second.infoKey) + " ").color(TextColors.AQUA))
                        .append(TextComponent.newline())
                        .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                                .color(TextColors.GREEN)
                                .clickEvent(ClickEvent.suggestCommand(condition.second.getBaseSetCommand(cDoor))));
            }
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
                Door door = commander.getDoor(String.valueOf(value.getDoorUID()), null);
                builder.append(value.getDoorUID()).append(" | ")
                        .append("§6").append(door.getName()).append("§r")
                        .append(" (").append(door.getWorld().getName()).append(")\n");
            }
        } else {
            List<Door> registeredDoors = commander.getDoors(player.getUniqueId().toString(), null)
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
            Door door = commander.getDoor(doorUID, null);
            if (door == null) {
                messageSender.sendError(null, localizer.getMessage("error.doorNotFound"));
                return null;
            }
            return door;
        }

        // sender id not console. retrieve door of player.
        ArrayList<Door> doors = commander.getDoors(player.getUniqueId().toString(), doorUID);

        if (doors.isEmpty()) {
            // door is null. check if door exists anyway
            Door door = commander.getDoor(doorUID, null);
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
        if (args.length < length) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments",
                    Replacement.create("SYNTAX", syntax).addFormatting('6')));
            return true;
        }
        return false;
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
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], CONDITION_TYPES).collect(Collectors.toList());
            }

            ConditionType type = EnumUtil.parse(args[2], ConditionType.class, true);
            if (type == null) {
                return Collections.singletonList(localizer.getMessage("error.invalidConditionType"));
            }

            if (denyAccess(sender, true, type.conditionGroup.permission, Permissions.ALL_CONDITION)) {
                return Collections.singletonList(localizer.getMessage("error.permission",
                        Replacement.create("PERMISSION", type.conditionGroup.permission + ", " + Permissions.ALL_CONDITION)));
            }

            switch (type) {
                case ITEM_CLICK:
                case ITEM_BLOCK:
                case ITEM_HOLDING:
                case ITEM_OWNING:
                    if (args.length == 4) {
                        return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
                    }
                    if (args.length == 5) {
                        if (args[4].isEmpty()) {
                            return Arrays.asList("true", "false");
                        }
                        return Arrays.asList("[" + localizer.getMessage("tabcomplete.consumed") + "]", "true", "false");
                    }
                    break;
                case PROXIMITY:
                    if (args.length == 4) {
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.dimensions") + ">", "<x,y,z>");
                    }
                    if (args.length == 5) {
                        return ArrayUtil.startingWithInArray(args[4], PROXIMITY_FORM).collect(Collectors.toList());
                    }
                    break;
                case REGION:
                    if (args.length == 4) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.regionName") + ">");
                    }
                    break;
                case PERMISSION:
                    if (args.length == 4) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.permission") + ">");
                    }
                    break;
                case TIME:
                    if (args.length == 4) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.open") + ">");
                    }
                    if (args.length == 5) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.setTimed.close") + ">");
                    }
                    if (args.length == 6) {
                        if (args[5].isEmpty()) {
                            return Arrays.asList("true", "false");
                        }
                        return Arrays.asList("[" + localizer.getMessage("tabcomplete.forceState") + "]", "true", "false");
                    }
                    break;
                case WEATHER:
                    if (args.length == 4) {
                        return ArrayUtil.startingWithInArray(args[3], WEATHER_TYPE).collect(Collectors.toList());
                    }
                    break;
                case PLACEHOLDER:
                    return Collections.singletonList("<" + localizer.getMessage("syntax.customEvaluator") + ">");
                case MYTHIC_MOBS:
                    List<String> mythicMobs;
                    try {
                        mythicMobs = pluginCache.get("mythicMobs", () -> MythicMobs.inst()
                                .getMobManager().getMobTypes()

                                .parallelStream()
                                .map(m -> m.getInternalName())
                                .collect(Collectors.toList()));
                    } catch (ExecutionException e) {
                        plugin.getLogger().log(Level.WARNING, "Could not build mob names.", e);
                        return Collections.emptyList();
                    }
                    return ArrayUtil.startingWithInArray(args[3], mythicMobs.toArray(new String[0])).collect(Collectors.toList());
            }
        }
            }
        }

        if ("removeCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], CONDITION_GROUPS).collect(Collectors.toList());
            }
        }

        if ("copyCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.sourceDoor") + ">");
            }
            if (args.length == 3) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.targetDoor") + ">");
            }
            if (args.length == 4) {
                return ArrayUtil.startingWithInArray(args[3], CONDITION_GROUPS).collect(Collectors.toList());
            }
        }

        if ("cloneDoor".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.sourceDoor") + ">");
            }
            if (args.length == 3) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.targetDoor") + ">");
            }
        }

        if ("info".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
        }

        if ("unregister".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
        }

        if ("invertOpen".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
        }

        if ("setEvaluator".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
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
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
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
                return Collections.singletonList("<" + localizer.getMessage("syntax.doorId") + ">");
            }
            if (args.length == 3) {
                return Collections.singletonList("<" + localizer.getMessage("syntax.amount") + ">");
            }
        }

        return Collections.emptyList();
    }
}

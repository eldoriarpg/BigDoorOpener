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
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.doors.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.doors.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.doors.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.doors.conditions.location.Region;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Permission;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.doors.conditions.standalone.Weather;
import de.eldoria.bigdoorsopener.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.scheduler.DoorChecker;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.bigdoorsopener.util.JsSyntaxHelper;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.ArrayUtil;
import de.eldoria.eldoutilities.utils.EnumUtil;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.Style;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import nl.pim16aap2.bigDoors.Commander;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
    private final BigDoorsOpener plugin;
    private final Commander commander;
    private final Config config;
    private final Localizer localizer;
    private final DoorChecker doorChecker;
    private final MessageSender messageSender;
    private final RegisterInteraction registerInteraction;
    private final RegionContainer regionContainer;
    private static final String[] CONDITION_TYPES;
    private static final String[] CONDITION_GROUPS;
    private static final String[] PROXIMITY_FORM;
    private static final String[] WEATHER_TYPE;
    private static final String[] EVALUATOR_TYPES;

    static {
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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0 || (args.length == 1 && "help".equalsIgnoreCase(args[0]))) {
            return help(player);
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
        if ("copyConditions".equalsIgnoreCase(cmd)) {
            return copyCondition(player, arguments);
        }

        if ("info".equalsIgnoreCase(cmd)) {
            return info(sender, arguments, player);
        }

        if ("unregister".equalsIgnoreCase(cmd)) {
            return unregister(arguments, player);
        }

        if ("invertOpen".equalsIgnoreCase(cmd)) {
            return invertOpen(arguments, player);
        }

        if ("setEvaluator".equalsIgnoreCase(cmd)) {
            return setEvaluator(player, arguments);
        }

        if ("stayOpen".equalsIgnoreCase(cmd)) {
            return stayOpen(player, arguments);
        }

        if ("list".equalsIgnoreCase(cmd)) {
            return list(player);
        }

        if ("reload".equalsIgnoreCase(cmd)) {
            return reload(player);
        }
        messageSender.sendError(player, localizer.getMessage("error.invalidCommand"));
        return true;
    }

    // bdo help
    private boolean help(Player player) {
        messageSender.sendMessage(player,
                localizer.getMessage("help.title",
                        Replacement.create("PLUGIN_NAME", plugin.getDescription().getName()).addFormatting('6')) + "\n"
                        + "§setCondition <condition> <doorId> <condition values>\n"
                        + "  §r" + localizer.getMessage("help.setCondition") + "\n"
                        + "§removeCondition <condition> <doorId>\n"
                        + "  §r" + localizer.getMessage("help.removeCondition") + "\n"
                        + "§6invertOpen <doorId> <true|false>\n"
                        + "  §r" + localizer.getMessage("help.invertOpen") + "\n"
                        + "§6unregister <doorId>\n"
                        + "  §r" + localizer.getMessage("help.unregister") + "\n"
                        + "§6info <doorId>\n"
                        + "  §r" + localizer.getMessage("help.info") + "\n"
                        + "§6list\n"
                        + "  §r" + localizer.getMessage("help.list") + "\n"
                        + "§6reload\n"
                        + "  §r" + localizer.getMessage("help.reload") + "\n"
                        + "§6about\n"
                        + "  §r" + localizer.getMessage("help.about"));
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
                        + localizer.getMessage("syntax.condition") + "> <"
                        + localizer.getMessage("syntax.conditionValues") + ">")) {
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

        ConditionType type = EnumUtil.parse(args[1], ConditionType.class);

        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        if (denyAccess(player, type.conditionGroup.permission, Permissions.ALL_CONDITION)) {
            return true;
        }

        ItemStack itemInMainHand = null;
        if (player != null) {
            itemInMainHand = player.getInventory().getItemInMainHand();
        }

        ConditionChain conditionChain = conditionalDoor.getConditionChain();


        String[] conditionArgs = Arrays.copyOfRange(args, 2, args.length);

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

                Optional<Boolean> consume = Parser.parseBoolean(conditionArgs[1]);
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
                            Replacement.create("MIN", 0).addFormatting('6'),
                            Replacement.create("MAX", 100).addFormatting('6')));
                    return true;
                }

                // check proximity form
                Proximity.ProximityForm form = EnumUtil.parse(conditionArgs[1], Proximity.ProximityForm.class);

                if (form == null) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidForm"));
                    return true;
                }
                conditionChain.setLocation(new Proximity(vector, form));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.proximity"));
                break;
            // <regionName>
            case REGION:
                if (regionContainer == null) {
                    messageSender.sendError(player, localizer.getMessage("error.wgNotEnabled"));
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
            // permission
            case PERMISSION:
                conditionChain.setPermission(new Permission(conditionArgs[0]));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.permission"));
                break;
            case TIME:
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
                messageSender.sendMessage(player, localizer.getMessage("setCondition.weather"));
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

        ConditionalDoor targetDoor = getOrRegister(playerSourceDoor, player);

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
        switch (type) {
            case ITEM:
                if (sourceChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                targetChain.setItem(sourceChain.getItem());
                break;
            case LOCATION:
                if (sourceChain.getLocation() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                targetChain.setLocation(sourceChain.getLocation());
                break;
            case PERMISSION:
                if (sourceChain.getPermission() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                targetChain.setPermission(sourceChain.getPermission());
                break;
            case TIME:
                if (sourceChain.getTime() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                targetChain.setTime(sourceChain.getTime());
                break;
            case WEATHER:
                if (sourceChain.getWeather() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                targetChain.setWeather(sourceChain.getWeather());
                break;
        }
        messageSender.sendMessage(player, localizer.getMessage("copyCondition.copiedSingle",
                Replacement.create("CONDITION", type.conditionParameter).addFormatting('6'),
                Replacement.create("SOURCE", playerSourceDoor.getName()).addFormatting('6'),
                Replacement.create("TARGET", playerTargetDoor.getName()).addFormatting('6')));
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

        ConditionType type = EnumUtil.parse(arguments[1], ConditionType.class);
        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        if (denyAccess(player, type.conditionGroup.permission, Permissions.ALL_CONDITION)) {
            return true;
        }

        ConditionChain conditionChain = cDoor.getConditionChain();
        switch (type.conditionGroup) {
            case ITEM:
                if (conditionChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                conditionChain.setItem(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.item"));
                break;
            case LOCATION:
                if (conditionChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                conditionChain.setLocation(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.location"));
                break;
            case PERMISSION:
                if (conditionChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                conditionChain.setPermission(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.permission"));
                break;
            case TIME:
                if (conditionChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                conditionChain.setTime(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.time"));
                break;
            case WEATHER:
                if (conditionChain.getItem() == null) {
                    messageSender.sendError(player, localizer.getMessage("error.conditionNotSet"));
                    return true;
                }
                conditionChain.setWeather(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.weather"));
                break;
        }

        // check if condition is in evaluator if a custom evaluator is present.
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            Pattern compile = Pattern.compile(type.conditionGroup.conditionParameter, Pattern.CASE_INSENSITIVE);
            if (compile.matcher(cDoor.getEvaluator()).find()) {
                messageSender.sendError(player, localizer.getMessage("warning.valueStillUsed",
                        Replacement.create("VALUE", type.conditionGroup.conditionParameter).addFormatting('6')));
            }
        }

        if (conditionChain.isEmpty()) {
            messageSender.sendMessage(player, localizer.getMessage("warning.chainIsEmpty"));
        }

        config.safeConfig();
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
                .append(TextComponent.builder(door.second.getName()).color(C.highlightColor))
                .append(TextComponent.builder("(Id:" + door.second.getDoorUID() + ") ")).color(C.highlightColor)
                .append(TextComponent.builder(localizer.getMessage("info.info")).color(C.baseColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("info.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getWorld()).color(C.highlightColor))
                .append(TextComponent.newline());

        // append evaluator
        builder.append(TextComponent.builder(localizer.getMessage("info.evaluator") + " ").color(C.baseColor));
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            builder.append(TextComponent.builder(cDoor.getEvaluator()).color(C.highlightColor));
        } else {
            builder.append(TextComponent.builder(cDoor.getEvaluationType().name()).color(C.highlightColor));
        }
        builder.append(TextComponent.newline());

        // append open time
        builder.append(TextComponent.builder(localizer.getMessage("info.stayOpen") + " ").color(C.baseColor))
                .append(TextComponent.builder(String.valueOf(cDoor.getStayOpen())).color(C.highlightColor))
                .append(TextComponent.newline());

        // start of key list
        builder.append(TextComponent.builder(localizer.getMessage("info.conditions"))
                .style(Style.builder().color(C.highlightColor).decoration(TextDecoration.BOLD, true).build()))
                .append(TextComponent.newline());

        ConditionChain conditionChain = cDoor.getConditionChain();
        if (conditionChain.getItem() != null) {
            builder.append(conditionChain.getItem().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("/bdo removeCondition item " + cDoor.getDoorUID())));

        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.itemCondition") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setCondition item")));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getLocation() != null) {
            builder.append(conditionChain.getLocation().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("/bdo removeCondition location " + cDoor.getDoorUID())));

        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.location") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setCondition ")));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getPermission() != null) {
            builder.append(conditionChain.getPermission().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("/bdo removeCondition permission " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.permission") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setCondition permission " + cDoor.getDoorUID())));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getTime() != null) {
            builder.append(conditionChain.getTime().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("/bdo removeCondition time " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.time") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setCondition time " + cDoor.getDoorUID())));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getWeather() != null) {
            builder.append(conditionChain.getWeather().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("/bdo removeCondition weather " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.weather") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("/bdo setCondition weather " + cDoor.getDoorUID())));
        }

        TextAdapter.sendMessage(sender, builder.build());
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

        if (player != null && !player.hasPermission(Permissions.CUSTOM_EVALUATOR)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
            return true;
        }

        if (arguments.length < 3) {
            messageSender.sendError(player, localizer.getMessage("error.noEvaluatorFound"));
            return true;
        }
        String evaluator = String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length));
        Pair<JsSyntaxHelper.ValidatorResult, String> result = JsSyntaxHelper.validateEvaluator(evaluator);

        switch (result.first) {
            case UNBALANCED_PARENTHESIS:
                messageSender.sendError(player, localizer.getMessage("error.unbalancedParenthesis"));
                break;
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
                Replacement.create("EVALUATOR", JsSyntaxHelper.translateEvaluator(evaluator))));
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
        messageSender.sendError(player, localizer.getMessage("stayOpen.message",
                Replacement.create("SECONDS", optionalInt.getAsInt())));
        return true;
    }

    // bdo list
    private boolean list(Player player) {
        if (denyAccess(player, Permissions.USE)) {
            return true;
        }

        Map<Long, ConditionalDoor> doors = config.getDoors();
        StringBuilder builder = new StringBuilder(localizer.getMessage("list.title"));

        for (ConditionalDoor value : doors.values()) {
            Door door = commander.getDoor(String.valueOf(value.getDoorUID()), null);
            builder.append(value.getDoorUID()).append(" | ")
                    .append(door.getName())
                    .append("(").append(door.getWorld().getName()).append(")\n");
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
     *
     * @param doorUID uid or name of the door
     * @param player
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
            messageSender.sendMessage(player, localizer.getMessage("error.ambigiousDoorName"));
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

    private boolean denyAccess(Player player, String... permissions) {
        if (player == null) {
            return false;
        }
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return false;
            }
        }
        messageSender.sendMessage(player, localizer.getMessage("error.permission",
                Replacement.create("PERMISSION", String.join(", ", permissions))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = args[0];

        if (args.length == 1) {
            return ArrayUtil.startingWithInArray(cmd,
                    new String[] {"help", "about", "setCondition", "removeCondition", "info",
                            "unregister", "invertOpen", "setEvaluator", "stayOpen", "list", "reload"})
                    .collect(Collectors.toList());
        }

        if ("setCondition".equalsIgnoreCase(cmd)) {


            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                String conditionType = args[2];
                return ArrayUtil.startingWithInArray(conditionType, CONDITION_TYPES).collect(Collectors.toList());
            }

            ConditionType type = EnumUtil.parse(args[2], ConditionType.class);
            if (type == null) {
                return Collections.singletonList(localizer.getMessage("error.invalidConditionType"));
            }

            if (!sender.hasPermission(type.conditionGroup.permission)) {
                return Collections.singletonList(localizer.getMessage("error.permission"));
            }

            switch (type) {
                case ITEM_CLICK:
                case ITEM_BLOCK:
                case ITEM_HOLDING:
                case ITEM_OWNING:
                    if (args.length == 4) {
                        return Collections.singletonList("<" + localizer.getMessage("tabcomplete.amount") + ">");
                    }
                    if (args.length == 5) {
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.consumed") + ">", "true", "false");
                    }
                    break;
                case PROXIMITY:
                    if (args.length == 4) {
                        return Arrays.asList(localizer.getMessage("tabcomplete.dimensions"), "<x,y,z>");
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
                        return Arrays.asList("<" + localizer.getMessage("tabcomplete.forceState") + ">", "true", "false");
                    }
                    break;
                case WEATHER:
                    if (args.length == 4) {
                        return ArrayUtil.startingWithInArray(args[3], WEATHER_TYPE).collect(Collectors.toList());
                    }
                    break;
            }
        }

        if ("removeCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                ArrayUtil.startingWithInArray(args[2], CONDITION_GROUPS);
            }
        }

        if ("copyCondition".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("syntax.sourceDoor"));
            }
            if (args.length == 3) {
                return Collections.singletonList(localizer.getMessage("syntax.targetDoor"));
            }
            if (args.length == 4) {
                return ArrayUtil.startingWithInArray(args[3], CONDITION_TYPES).collect(Collectors.toList());
            }
        }

        if ("info".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
        }

        if ("unregister".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
        }

        if ("invertOpen".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
        }

        if ("setEvaluator".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return ArrayUtil.startingWithInArray(args[2], EVALUATOR_TYPES).collect(Collectors.toList());
            }

            ConditionalDoor.EvaluationType parse = EnumUtil.parse(args[2], ConditionalDoor.EvaluationType.class);

            if (parse == null) {
                return Collections.singletonList(localizer.getMessage("error.invalidEvaluationType"));
            }

            if (parse == ConditionalDoor.EvaluationType.CUSTOM) {
                if (!sender.hasPermission(Permissions.CUSTOM_EVALUATOR)) {
                    return Collections.singletonList(localizer.getMessage("error.permission"));
                }
                ArrayList<String> list = new ArrayList<>(Arrays.asList(CONDITION_GROUPS));
                list.add("currentState");
                list.add(localizer.getMessage("tabcomplete.validValues"));
                return list;
            }
        }

        if ("stayOpen".equalsIgnoreCase(cmd)) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.amount"));
            }
        }
        return Collections.emptyList();
    }
}

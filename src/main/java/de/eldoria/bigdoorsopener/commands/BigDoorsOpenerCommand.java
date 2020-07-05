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
import de.eldoria.bigdoorsopener.util.Pair;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import de.eldoria.eldoutilities.utils.ArrayUtil;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
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

        String cmd = args[0];

        if ("about".equalsIgnoreCase(cmd)) {
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

        if ("info".equalsIgnoreCase(cmd)) {
            return info(sender, arguments, player);
        }

        if ("unregister".equalsIgnoreCase(cmd)) {
            return unregister(arguments, player);
        }

        if ("invertOpen".equalsIgnoreCase(cmd)) {
            return invertOpen(arguments, player);
        }

        if ("list".equalsIgnoreCase(cmd)) {
            return list(player);
        }

        if ("reload".equalsIgnoreCase(cmd)) {
            return reload(player);
        }
        return false;
    }

    private boolean reload(Player player) {
        if (player != null && !player.hasPermission(Permissions.RELOAD)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
            return true;
        }
        config.reloadConfig();
        doorChecker.reload();
        plugin.onEnable();
        messageSender.sendMessage(player, localizer.getMessage("reload.completed"));
        return true;
    }

    private boolean list(Player player) {
        if (player != null && !player.hasPermission(Permissions.USE)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
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

    private boolean invertOpen(String[] args, Player player) {
        if (player != null && !player.hasPermission(Permissions.USE)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
            return true;
        }
        if (args.length != 1) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
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

    private boolean unregister(String[] args, Player player) {
        if (player != null && !player.hasPermission(Permissions.USE)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
            return true;
        }
        if (args.length != 1) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
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

    private boolean info(CommandSender sender, String[] args, Player player) {
        if (player != null && !player.hasPermission(Permissions.USE)) {
            messageSender.sendError(player, localizer.getMessage("error.permission"));
            return true;
        }

        if (args.length != 1) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
            return true;
        }
        Pair<ConditionalDoor, Door> door = getConditionalPlayerDoor(args[1], player);
        if (door == null) {
            return true;
        }

        ConditionalDoor cDoor = door.first;
        TextComponent.Builder builder = TextComponent.builder()
                .append(TextComponent.builder(localizer.getMessage(door.second.getName() + " ")).color(C.highlightColor))
                .append(TextComponent.builder("(Id:" + door.second.getDoorUID() + ") ")).color(C.highlightColor)
                .append(TextComponent.builder(localizer.getMessage("info.info")).color(C.baseColor))
                .append(TextComponent.newline())
                .append(TextComponent.builder(localizer.getMessage("info.world") + " ").color(C.baseColor))
                .append(TextComponent.builder(cDoor.getWorld()).color(C.highlightColor))
                .append(TextComponent.newline());

        // append evaluator
        builder.append(TextComponent.builder(localizer.getMessage("info.evaluator")).color(C.baseColor));
        if (cDoor.getEvaluationType() == ConditionalDoor.EvaluationType.CUSTOM) {
            builder.append(TextComponent.builder(cDoor.getEvaluator()).color(C.highlightColor));
        } else {
            builder.append(TextComponent.builder(cDoor.getEvaluationType().name()).color(C.highlightColor));
        }
        builder.append(TextComponent.newline());

        // append open time
        builder.append(TextComponent.builder(localizer.getMessage("info.stayOpen")).color(C.baseColor))
                .append(TextComponent.builder(String.valueOf(cDoor.getStayOpen())).color(C.highlightColor))
                .append(TextComponent.newline());

        // start of key list
        builder.append(TextComponent.builder(localizer.getMessage("info.conditions"))
                .style(Style.builder().color(C.highlightColor).decoration(TextDecoration.BOLD, true).build()))
                .append(TextComponent.newline());

        ConditionChain conditionChain = cDoor.getConditionChain();
        if (conditionChain.getItemKey() != null) {
            builder.append(conditionChain.getItemKey().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("bdo removeCondition item " + cDoor.getDoorUID())));

        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.itemCondition") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("bdo setCondition item")));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getLocation() != null) {
            builder.append(conditionChain.getLocation().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("bdo removeCondition location " + cDoor.getDoorUID())));

        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.location") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("bdo setCondition ")));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getPermission() != null) {
            builder.append(conditionChain.getLocation().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("bdo removeCondition permission " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.permission") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("bdo setCondition permission " + cDoor.getDoorUID())));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getTime() != null) {
            builder.append(conditionChain.getTime().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("bdo removeCondition time " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.time") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("bdo setCondition time " + cDoor.getDoorUID())));
        }
        builder.append(TextComponent.newline());

        if (conditionChain.getWeather() != null) {
            builder.append(conditionChain.getTime().getDescription(localizer))
                    .append(TextComponent.newline())
                    .append(TextComponent.builder("[" + localizer.getMessage("info.remove") + "]")
                            .color(TextColor.DARK_RED)
                            .clickEvent(ClickEvent.runCommand("bdo removeCondition weather " + cDoor.getDoorUID())));
        } else {
            builder.append(TextComponent.builder(localizer.getMessage("info.weather") + " ").color(C.highlightColor))
                    .append(TextComponent.builder("[" + localizer.getMessage("info.add") + "]")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand("bdo setCondition weather " + cDoor.getDoorUID())));
        }

        TextAdapter.sendMessage(sender, builder.build());
        return true;
    }

    private boolean removeCondition(Player player, String[] arguments) {
        Door playerDoor = getPlayerDoor(arguments[3], player);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor cDoor = getOrRegister(playerDoor, player);

        if (cDoor == null) {
            return true;
        }

        ConditionType type = ConditionType.getType(arguments[4]);
        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        ConditionChain conditionChain = cDoor.getConditionChain();
        switch (type.conditionGroup) {
            case ITEM:
                if (conditionChain.getItemKey() == null) {
                    messageSender.sendError(player, localizer.getMessage("removeCondition.notSet"));
                    return true;
                }
                conditionChain.setItemKey(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.item"));
                break;
            case LOCATION:
                if (conditionChain.getItemKey() == null) {
                    messageSender.sendError(player, localizer.getMessage("removeCondition.notSet"));
                    return true;
                }
                conditionChain.setLocation(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.location"));
                break;
            case PERMISSION:
                if (conditionChain.getItemKey() == null) {
                    messageSender.sendError(player, localizer.getMessage("removeCondition.notSet"));
                    return true;
                }
                conditionChain.setPermission(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.permission"));
                break;
            case TIME:
                if (conditionChain.getItemKey() == null) {
                    messageSender.sendError(player, localizer.getMessage("removeCondition.notSet"));
                    return true;
                }
                conditionChain.setTime(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.time"));
                break;
            case WEATHER:
                if (conditionChain.getItemKey() == null) {
                    messageSender.sendError(player, localizer.getMessage("removeCondition.notSet"));
                    return true;
                }
                conditionChain.setWeather(null);
                messageSender.sendMessage(player, localizer.getMessage("removeCondition.weather"));
                break;
        }
        return true;
    }

    private boolean setCondition(Player player, String[] arguments) {
        if (arguments.length < 3) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments"));
            return true;
        }

        Door playerDoor = getPlayerDoor(arguments[2], player);

        if (playerDoor == null) {
            return true;
        }

        ConditionalDoor conditionalDoor = getOrRegister(playerDoor, player);

        if (conditionalDoor == null) {
            return true;
        }

        ConditionType type = ConditionType.getType(arguments[1]);

        if (type == null) {
            messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
            return true;
        }

        ItemStack itemInMainHand = null;
        if (player != null) {
            itemInMainHand = player.getInventory().getItemInMainHand();
        }

        OptionalInt amount;
        Optional<Boolean> consume;

        ConditionChain conditionChain = conditionalDoor.getConditionChain();

        switch (type) {
            // <amount> <consumed>
            case ITEM_CLICK:
                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }
                amount = Parser.parseInt(arguments[3]);
                if (!amount.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                    return true;
                }
                consume = Parser.parseBoolean(arguments[4]);
                if (!consume.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;

                }
                itemInMainHand.setAmount(amount.getAsInt());
                conditionChain.setItemKey(new ItemClick(itemInMainHand, consume.get()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.itemClick"));
                break;
            // <amount> <consumed>
            case ITEM_BLOCK:
                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }
                amount = Parser.parseInt(arguments[3]);
                if (!amount.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                    return true;
                }
                consume = Parser.parseBoolean(arguments[4]);
                if (!consume.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;

                }
                itemInMainHand.setAmount(amount.getAsInt());
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
                break;
            // <amount> <consumed>
            case ITEM_HOLDING:
                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }
                amount = Parser.parseInt(arguments[3]);
                if (!amount.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                    return true;
                }
                consume = Parser.parseBoolean(arguments[4]);
                if (!consume.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;

                }
                itemInMainHand.setAmount(amount.getAsInt());
                conditionChain.setItemKey(new ItemHolding(itemInMainHand, consume.get()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.itemHolding"));
                break;
            // <amount> <consumed>
            case ITEM_OWNING:
                if (player == null) {
                    messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                    return true;
                }
                amount = Parser.parseInt(arguments[3]);
                if (!amount.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                    return true;
                }
                consume = Parser.parseBoolean(arguments[4]);
                if (!consume.isPresent()) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                    return true;

                }
                itemInMainHand.setAmount(amount.getAsInt());
                conditionChain.setItemKey(new ItemOwning(itemInMainHand, consume.get()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.itemOwning"));
                break;
            // <dimensions> <form>
            case PROXIMITY:
                Vector vector;
                String[] coords = arguments[3].split(",");
                if (coords.length == 1) {
                    OptionalDouble size = Parser.parseDouble(arguments[3]);
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
                Proximity.ProximityForm form = Proximity.ProximityForm.parse(arguments[4]);
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
                ProtectedRegion region = rm.getRegion(arguments[3]);
                if (region == null) {
                    messageSender.sendError(player, localizer.getMessage("error.regionNotFound"));
                    return true;
                }
                conditionChain.setLocation(new Region(region, player.getWorld()));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.region"));
                break;
            // permission
            case PERMISSION:
                conditionChain.setPermission(new Permission(arguments[3]));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.permission"));
                break;
            case TIME:
                OptionalInt open = Parser.parseInt(arguments[3]);
                if (!open.isPresent()) {
                    open = Parser.parseTimeToTicks(arguments[3]);
                    if (!open.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidOpenTime"));
                        return true;
                    }
                }
                OptionalInt close = Parser.parseInt(arguments[4]);
                if (!close.isPresent()) {
                    close = Parser.parseTimeToTicks(arguments[3]);
                    if (!close.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidCloseTime"));
                        return true;
                    }
                }
                Optional<Boolean> force = Parser.parseBoolean(arguments[4]);
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
                    if (value.name().equalsIgnoreCase(arguments[3])) {
                        weatherType = value;
                    }
                }
                if (weatherType == null) {
                    messageSender.sendError(player, localizer.getMessage("error.invalidWeatherType"));
                    return true;
                }
                conditionChain.setWeather(new Weather(weatherType));
                messageSender.sendMessage(player, localizer.getMessage("setCondition.weather"));
                break;
            default:
                messageSender.sendError(player, localizer.getMessage("error.invalidConditionType"));
                return true;
        }

        config.safeConfig();
        return false;
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

    private Pair<ConditionalDoor, Door> getConditionalPlayerDoor(String doorUID, Player player) {
        Door door = getDoor(doorUID, player);
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
     * Get a door which is not associated to the player.
     *
     * @param doorUID id of door
     * @param player  player which requested the door.
     * @return a door or null if no door with this id was found.
     */
    private Door getDoor(String doorUID, Player player) {
        Door door = commander.getDoor(doorUID, null);
        if (door == null) {
            messageSender.sendError(player, localizer.getMessage("error.doorNotFound"));
            return null;
        }
        return door;
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
            return getDoor(doorUID, null);
        }
        Door door = commander.getDoor(doorUID, player);
        if (door == null) {
            door = getDoor(doorUID, player);
            if (door != null && player.hasPermission(Permissions.ACCESS_ALL)) {
                return door;
            } else {
                messageSender.sendError(player, localizer.getMessage("error.notYourDoor"));
            }
            return null;
        }
        return door;
    }

    private boolean checkArgumentLength(Player player, String[] args, int length, String syntax) {
        if (args.length != length) {
            messageSender.sendError(player, localizer.getMessage("error.invalidArguments",
                    Replacement.create("SYNTAX", syntax)));
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("setTimed".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList("<doorId>");
            }
            if (args.length == 3) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.setTimed.open"));
            }
            if (args.length == 4) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.setTimed.close"));
            }
            return Collections.emptyList();
        }

        if ("setClosed".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("setRange".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.range"));
            }
            return Collections.emptyList();
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("unregister".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            return Collections.emptyList();
        }

        if ("invertOpen".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false");
            }
            return Collections.emptyList();
        }

        if ("requiresPermission".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return Collections.singletonList(localizer.getMessage("tabcomplete.doorId"));
            }
            if (args.length == 3) {
                return Arrays.asList("true", "false", localizer.getMessage("tabcomplete.permission"));
            }
            return Collections.emptyList();
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }

        if (args.length == 1) {

            return ArrayUtil.startingWithInArray(args[0],
                    new String[] {"setTimed", "setClosed", "setRange", "unregister", "info", "invertOpen", "list", "requiresPermission", "reload"})
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

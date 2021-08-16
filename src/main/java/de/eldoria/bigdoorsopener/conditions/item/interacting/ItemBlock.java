package de.eldoria.bigdoorsopener.conditions.item.interacting;

import de.eldoria.bigdoorsopener.conditions.item.Item;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.listener.registration.RegisterInteraction;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A key which opens a door, when the player is clicking at a specific block
 */
@SerializableAs("itemBlockCondition")
public class ItemBlock extends ItemInteraction {

    private BlockVector position;

    /**
     * Creates a new item block condition without a set position. This object is incomplete and has to be initialized
     * with the {@link #position(BlockVector)} method before using.
     *
     * @param item
     * @param consumed
     */
    public ItemBlock(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    /**
     * Constructor used for serialization.
     *
     * @param position position of block
     * @param stack    item stack of block
     * @param consumed true if item should be consumed.
     */
    private ItemBlock(BlockVector position, ItemStack stack, boolean consumed) {
        super(stack, consumed);
        this.position = position;
    }

    public static ItemBlock deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        BlockVector position = resolvingMap.getValue("position");
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemBlock(position, stack, consumed);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemBlock.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    ILocalizer localizer = BigDoorsOpener.localizer();
                    if (player == null) {
                        messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                        return;
                    }

                    if (argumentsInvalid(player, arguments, 1,
                            "<" + localizer.getMessage("syntax.doorId") + "> <"
                            + localizer.getMessage("syntax.condition") + "> <"
                            + localizer.getMessage("syntax.amount") + "> ["
                            + localizer.getMessage("tabcomplete.consumed") + "]")) {
                        return;
                    }

                    // parse amount
                    OptionalInt amount = Parser.parseInt(arguments[0]);
                    if (!amount.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidAmount"));
                        return;
                    }

                    if (amount.getAsInt() > 64 || amount.getAsInt() < 1) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidRange",
                                Replacement.create("MIN", 1).addFormatting('6'),
                                Replacement.create("MAX", 64).addFormatting('6')));
                        return;
                    }

                    Optional<Boolean> consume = ArgumentUtils.getOptionalParameter(arguments, 1, Optional.of(false), Parser::parseBoolean);
                    if (!consume.isPresent()) {
                        messageSender.sendError(player, localizer.getMessage("error.invalidBoolean"));
                        return;
                    }

                    ItemStack itemInMainHand = player.getInventory().getItemInMainHand().clone();

                    itemInMainHand.setAmount(amount.getAsInt());
                    ItemBlock itemBlock = new ItemBlock(itemInMainHand, consume.get());

                    messageSender.sendLocalizedMessage(player, "setCondition.clickKeyBlock");
                    // Register Keyhole object at registration listener.
                    RegisterInteraction.getInstance().register(player, (event, mSender) -> {
                        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                            return false;
                        }
                        if (event.getClickedBlock() == null) return false;
                        BlockVector blockVector = event.getClickedBlock().getLocation().toVector().toBlockVector();
                        itemBlock.position(blockVector);
                        conditionBag.accept(itemBlock);
                        event.setCancelled(true);
                        mSender.sendMessage(player, localizer.getMessage("setCondition.itemBlockRegistered"));
                        return true;
                    });
                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemBlock", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        takeFromInventory(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        if (hasPlayerItemInHand(player)) {
            return super.isOpen(player, world, door, currentState);
        }
        return false;
    }

    @Override
    public void clicked(PlayerInteractEvent event) {
        event.setCancelled(true);
        super.clicked(event);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder(super.serialize())
                .add("position", position)
                .build();
    }

    @Override
    public Component getDescription(ILocalizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());

        return Component.text(
                        localizer.getMessage("conditionDesc.type.itemBlock",
                                Replacement.create("NAME", containerByClass
                                        .map(ConditionContainer::getName).orElse("undefined"))), NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.keyhole") + " ", C.baseColor))
                .append(Component.text(position.toString(), C.highlightColor))
                .append(Component.newline())
                .append(super.getDescription(localizer));
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.doorUID() + " itemBlock " + item().getAmount() + " " + isConsumed();
    }

    @Override
    public ItemBlock clone() {
        return new ItemBlock(position, item(), isConsumed());
    }

    public void position(BlockVector position) {
        this.position = position;
    }

    public BlockVector position() {
        return position;
    }
}

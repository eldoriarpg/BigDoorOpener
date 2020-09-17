package de.eldoria.bigdoorsopener.doors.conditions.item;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.bigdoorsopener.util.TextColors;
import de.eldoria.eldoutilities.localization.Localizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.ArgumentUtils;
import de.eldoria.eldoutilities.utils.Parser;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static de.eldoria.bigdoorsopener.util.ArgumentHelper.argumentsInvalid;

/**
 * A key which will be open when the player is holding a key in his hand.
 */
@SerializableAs("itemHoldingCondition")
public class ItemHolding extends Item {
    public ItemHolding(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    public static ItemHolding deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemHolding(stack, consumed);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemHolding.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    Localizer localizer = BigDoorsOpener.localizer();
                    if (player == null) {
                        messageSender.sendError(null, localizer.getMessage("error.notAllowedFromConsole"));
                        return;
                    }

                    if (argumentsInvalid(player, messageSender, localizer, arguments, 1,
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
                    conditionBag.putCondition(new ItemHolding(itemInMainHand, consume.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemHolding"));

                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemHolding", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        tryTakeFromHands(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInHand(player);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemHolding",
                        Replacement.create("NAME", ConditionType.ITEM_HOLDING.conditionName))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " itemHolding " + getItem().getAmount() + " " + isConsumed();
    }

    @Override
    public void evaluated() {

    }

}

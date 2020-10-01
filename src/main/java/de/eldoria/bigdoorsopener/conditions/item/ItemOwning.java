package de.eldoria.bigdoorsopener.conditions.item;

import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
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

import static de.eldoria.bigdoorsopener.commands.CommandHelper.argumentsInvalid;

/**
 * A key which opens a doow, when the player has it in his inventory.
 */
@SerializableAs("itemOwningCondition")
public class ItemOwning extends Item {
    public ItemOwning(ItemStack item, boolean consumed) {
        super(item, consumed);
    }

    public static ConditionContainer getConditionContainer() {
        return ConditionContainer.ofClass(ItemOwning.class, Scope.PLAYER)
                .withFactory((player, messageSender, conditionBag, arguments) -> {
                    Localizer localizer = BigDoorsOpener.localizer();
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
                    conditionBag.putCondition(new ItemOwning(itemInMainHand, consume.get()));
                    messageSender.sendMessage(player, localizer.getMessage("setCondition.itemOwning"));
                })
                .onTabComplete(Item::onTabComplete)
                .withMeta("itemOwning", "item", ConditionContainer.Builder.Cost.PLAYER_MEDIUM.cost)
                .build();
    }

    @Override
    public void opened(Player player) {
        if (!isConsumed()) return;
        takeFromInventory(player);
    }

    @Override
    public Boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return hasPlayerItemInInventory(player);
    }

    public ItemOwning deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        ItemStack stack = resolvingMap.getValue("item");
        boolean consumed = resolvingMap.getValue("consumed");
        return new ItemOwning(stack, consumed);
    }

    @Override
    public TextComponent getDescription(Localizer localizer) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(getClass());


        return TextComponent.builder(
                localizer.getMessage("conditionDesc.type.itemOwning",
                        Replacement.create("NAME", containerByClass
                                .map(ConditionContainer::getName).orElse("undefined")))).color(TextColors.AQUA)
                .append(TextComponent.newline())
                .append(super.getDescription(localizer))
                .build();
    }

    @Override
    public String getCreationCommand(ConditionalDoor door) {
        return SET_COMMAND + door.getDoorUID() + " itemOwning " + getItem().getAmount() + " " + isConsumed();
    }
}

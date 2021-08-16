package de.eldoria.bigdoorsopener.door;

import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionChain;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.EnumUtil;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A conditional door consists of some basic settings and a condition chain. A conditional door can be open or closed.
 * This dependes on several conditions. Hint: Thats why its called conditional door.
 */
@SerializableAs("conditionalDoor")
public class ConditionalDoor implements ConfigurationSerializable {
    /**
     * UID of the door from {@link Door}.
     */
    private final long doorUID;

    /**
     * Name of the world the door is in.
     */
    private final String world;

    /**
     * Mass center of the door.
     */
    private final Vector position;

    private boolean enabled = true;

    private Instant openTill;

    /**
     * JS evaluator as string
     */
    private String evaluator = "";

    /**
     * the type the condition chain uses to evaluate the conditions
     */
    private EvaluationType evaluationType = EvaluationType.AND;

    @NotNull
    private ConditionBag conditionBag;

    /**
     * Amount of time in seconds a door will stay open when opened.
     */
    private int stayOpen = 0;

    private boolean waitForOpen = false;

    /**
     * True if the door was registered in open state.
     */
    private boolean invertOpen = false;


    public ConditionalDoor(long doorUID, String world, Vector position, ConditionBag conditionBag) {
        this.doorUID = doorUID;
        this.world = world;
        this.position = position;
        this.conditionBag = conditionBag;
    }

    public ConditionalDoor(long doorUID, String world, Vector position) {
        this(doorUID, world, position, new ConditionBag());
    }

    @SuppressWarnings({"casting", "RedundantCast"})
    public ConditionalDoor(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        doorUID = (int) resolvingMap.getValue("doorUID");
        world = resolvingMap.getValue("world");
        BigDoorsOpener.logger().fine("Loading door \"" + doorUID + "\".");
        position = resolvingMap.getValue("position");
        enabled = resolvingMap.getValueOrDefault("enabled", true);
        invertOpen = resolvingMap.getValue("invertOpen");
        evaluator = resolvingMap.getValue("evaluator");
        evaluationType = EnumUtil.parse(resolvingMap.getValue("evaluationType"), EvaluationType.class);
        if (resolvingMap.containsKey("conditionChain")) {
            BigDoorsOpener.logger().fine("Converting condition chain to condition bag");
            ConditionChain conditionChain = resolvingMap.getValue("conditionChain");
            conditionBag = new ConditionBag();
            conditionChain.getConditions().stream().filter(java.util.Objects::nonNull).forEach(c -> conditionBag.setCondition(this, c));
        } else {
            conditionBag = resolvingMap.getValueOrDefault("conditionBag", new ConditionBag());
        }
        stayOpen = resolvingMap.getValue("stayOpen");
    }

    /**
     * Get the state of the door.
     *
     * @param player       player for player sensitive calculations
     * @param world        world of the door
     * @param currentState the current state of the door.
     * @return true if the door should be open or false if not.
     */
    public boolean getState(Player player, World world, boolean currentState) {
        if (openTill != null && openTill.isAfter(Instant.now())) return true;

        if (waitForOpen) {
            return true;
        }

        switch (evaluationType) {
            case CUSTOM:
                String custom = conditionBag.custom(evaluator, player, world, this, currentState);
                return BigDoorsOpener.JS().eval(custom, currentState);
            case AND:
                return conditionBag.and(player, world, this, currentState);
            case OR:
                return conditionBag.or(player, world, this, currentState);
            default:
                throw new IllegalStateException("Unexpected value: " + evaluationType);
        }
    }

    public boolean openInverted(boolean open) {
        if (invertOpen) return !open;
        return open;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalDoor door = (ConditionalDoor) o;
        return doorUID == door.doorUID();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(doorUID);
    }

    /**
     * This method is called when a door is toggled to change its state from closed to open.
     *
     * @param player player which opened the door.
     */
    public void opened(@Nullable Player player) {
        waitForOpen = true;
        if (player == null) return;
        conditionBag.opened(player);
    }

    /**
     * Called when a door is fully opened.
     */
    public void opened() {
        waitForOpen = false;
        openTill = Instant.now().plus(stayOpen, SECONDS);
    }

    public void evaluated() {
        conditionBag.evaluated();
    }

    public boolean requiresPlayerEvaluation() {
        return conditionBag.requiresPlayerEvaluation();
    }

    public void setEvaluator(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
        if (evaluationType == EvaluationType.OR || evaluationType == EvaluationType.AND) {
            evaluator = "";
        }
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    public void setEvaluator(String evaluator) {
        this.evaluator = evaluator;
        evaluationType = EvaluationType.CUSTOM;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    /**
     * Forces a door to stay open the amount of seconds after it was opened. Will skip any checks in this time.
     *
     * @param stayOpen amount of seconds the door should stay open before checking the conditions again.
     */
    public void setStayOpen(int stayOpen) {
        this.stayOpen = stayOpen;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    /**
     * Toggle the invert open state.
     */
    public void invertOpen() {
        invertOpen = !invertOpen;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        BigDoorsOpener.logger().fine("Saving door \"" + doorUID + "\".");
        return SerializationUtil.newBuilder()
                .add("doorUID", doorUID)
                .add("world", world)
                .add("position", position)
                .add("enabled", enabled)
                .add("invertOpen", invertOpen)
                .add("evaluator", evaluator)
                .add("evaluationType", evaluationType)
                .add("stayOpen", stayOpen)
                .add("conditionBag", conditionBag)
                .build();
    }

    public void conditionBag(@NotNull ConditionBag conditionBag) {
        this.conditionBag = conditionBag;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    public void invertOpen(boolean invertOpen) {
        this.invertOpen = invertOpen;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    public void enabled(Boolean state) {
        enabled = state;
        Bukkit.getPluginManager().callEvent(new DoorModifiedEvent(this));
    }

    public long doorUID() {
        return doorUID;
    }

    public String world() {
        return world;
    }

    public Vector position() {
        return position;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant openTill() {
        return openTill;
    }

    public String evaluator() {
        return evaluator;
    }

    public EvaluationType evaluationType() {
        return evaluationType;
    }

    public ConditionBag conditionBag() {
        return conditionBag;
    }

    public int stayOpen() {
        return stayOpen;
    }

    public boolean isWaitForOpen() {
        return waitForOpen;
    }

    public boolean isInvertOpen() {
        return invertOpen;
    }

    public enum EvaluationType {
        CUSTOM, AND, OR
    }
}

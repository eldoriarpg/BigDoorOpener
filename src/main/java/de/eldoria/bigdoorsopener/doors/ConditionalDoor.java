package de.eldoria.bigdoorsopener.doors;

import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionChain;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import de.eldoria.eldoutilities.utils.EnumUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A conditional door consists of some basic settings and a condition chain.
 * A conditional door can be open or closed. This dependes on several conditions.
 * Hint: Thats why its called conditional door.
 */
@Getter
@SerializableAs("conditionalDoor")
public class ConditionalDoor implements ConfigurationSerializable {
    /**
     * UID of the door from {@link nl.pim16aap2.bigDoors.Door}.
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

    private Instant openTill;

    /**
     * JS evaluator as string
     */
    private String evaluator = "";

    /**
     * the type the condition chain uses to evaluate the conditions
     */
    private EvaluationType evaluationType = EvaluationType.OR;

    @Getter
    @Setter
    @Nonnull
    private ConditionChain conditionChain;

    /**
     * Amount of time in seconds a door will stay open when opened.
     */
    private int stayOpen = 0;


    /**
     * True if the door was registered in open state.
     */
    private boolean invertOpen = false;

    private static final CachingJSEngine JS;

    static {
        JS = new CachingJSEngine(200);
    }

    public ConditionalDoor(long doorUID, String world, Vector position, ConditionChain conditionChain) {
        this.doorUID = doorUID;
        this.world = world;
        this.position = position;
        this.conditionChain = conditionChain;
    }

    public ConditionalDoor(long doorUID, String world, Vector position) {
        this(doorUID, world, position, new ConditionChain());
    }

    public ConditionalDoor(long doorUID, String world, Vector position, boolean invertOpen, String evaluator,
                           EvaluationType evaluationType, ConditionChain conditionChain, int stayOpen) {
        this(doorUID, world, position, conditionChain);
        this.invertOpen = invertOpen;
        this.evaluator = evaluator;
        this.evaluationType = evaluationType;
        this.stayOpen = stayOpen;
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

        switch (evaluationType) {
            case CUSTOM:
                String custom = conditionChain.custom(evaluator, player, world, this, currentState);
                return JS.eval(custom, currentState);
            case AND:
                return conditionChain.and(player, world, this, currentState);
            case OR:
                return conditionChain.or(player, world, this, currentState);
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
        return doorUID == door.getDoorUID();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(doorUID);
    }

    /**
     * This method is called when a door changes its state from closed to open.
     *
     * @param player player which opened the door.
     */
    public void opened(Player player) {
        openTill = Instant.now().plus(stayOpen, SECONDS);
        conditionChain.opened(player);
    }

    public void evaluated() {
        conditionChain.evaluated();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder()
                .add("doorUID", doorUID)
                .add("world", world)
                .add("position", position)
                .add("invertOpen", invertOpen)
                .add("evaluator", evaluator)
                .add("evaluationType", evaluationType)
                .add("stayOpen", stayOpen)
                .add("conditionChain", conditionChain)
                .build();
    }

    public static ConditionalDoor deserialize(Map<String, Object> map) {
        TypeResolvingMap resolvingMap = SerializationUtil.mapOf(map);
        int doorUID = resolvingMap.getValue("doorUID");
        String world = resolvingMap.getValue("world");
        Vector position = resolvingMap.getValue("position");
        boolean invertOpen = resolvingMap.getValue("invertOpen");
        String evaluator = resolvingMap.getValue("evaluator");
        EvaluationType evaluationType = EnumUtil.parse(resolvingMap.getValue("evaluationType"), EvaluationType.class);
        ConditionChain conditionChain = resolvingMap.getValue("conditionChain");
        int stayOpen = resolvingMap.getValue("stayOpen");
        return new ConditionalDoor(doorUID, world, position, invertOpen, evaluator, evaluationType, conditionChain, stayOpen);
    }

    public boolean requiresPlayerEvaluation() {
        return conditionChain.requiresPlayerEvaluation();
    }

    public void setEvaluator(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
        if (evaluationType == EvaluationType.OR || evaluationType == EvaluationType.AND) {
            evaluator = "";
        }
    }

    public void setEvaluator(String evaluator) {
        this.evaluator = evaluator;
        evaluationType = EvaluationType.CUSTOM;
    }

    /**
     * Forces a door to stay open the amount of seconds after it was opened.
     * Will skip any checks in this time.
     *
     * @param stayOpen amount of seconds the door should stay open before checking the conditions again.
     */
    public void setStayOpen(int stayOpen) {
        this.stayOpen = stayOpen;
    }

    /**
     * Toggle the invert open state.
     */
    public void invertOpen() {
        invertOpen = !invertOpen;
    }

    public enum EvaluationType {
        CUSTOM, AND, OR
    }
}


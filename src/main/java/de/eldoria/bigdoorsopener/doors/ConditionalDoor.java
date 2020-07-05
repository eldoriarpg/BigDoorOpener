package de.eldoria.bigdoorsopener.doors;

import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionChain;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

@Getter
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

    private String evaluator;

    private EvaluationType evaluationType;

    @Getter
    @Nonnull
    private final ConditionChain conditionChain;

    private int stayOpen = 0;

    private Instant openTill;

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

    public boolean getState(Player player, World world, boolean currentState) {
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

    public void opened(Player player) {
        //TODO stay open
        conditionChain.opened(player);
    }

    public void evaluated() {
        conditionChain.evaluated();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    public enum EvaluationType {
        CUSTOM, AND, OR
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
}

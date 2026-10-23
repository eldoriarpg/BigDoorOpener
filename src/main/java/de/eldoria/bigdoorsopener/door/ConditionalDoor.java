/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.door;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.events.ConditionAddedEvent;
import de.eldoria.bigdoorsopener.core.events.ConditionRemovedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.door.conditioncollections.ConditionBag;
import nl.pim16aap2.bigDoors.Door;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A conditional door consists of some basic settings and a condition chain. A conditional door can be open or closed.
 * This dependes on several conditions. Hint: Thats why its called conditional door.
 * <p>
 * Stored in {@code doors.yml}. The conditions are stored as one list, their groups are derived from their types.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ConditionalDoor {
    @JsonIgnore
    private boolean locked;
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

    @JsonIgnore
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
    @JsonIgnore
    private ConditionBag conditionBag;

    /**
     * Amount of time in seconds a door will stay open when opened.
     */
    private int stayOpen = 0;

    @JsonIgnore
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

    @JsonCreator
    private ConditionalDoor(
            @JsonProperty("doorUID") long doorUID,
            @JsonProperty("world") String world,
            @JsonProperty("position") Vector position,
            @JsonProperty("conditions") List<DoorCondition> conditions) {
        this(doorUID, world, position, new ConditionBag(conditions == null ? List.of() : conditions));
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
     * @param player player which opened the door, or null when no player was involved
     */
    public void opened(@Nullable Player player) {
        waitForOpen = true;
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

    @JsonProperty("conditions")
    private List<DoorCondition> conditions() {
        return new ArrayList<>(conditionBag.getConditions());
    }

    /**
     * Replaces the conditions of the door. Every replaced condition is announced as removed and every new condition as
     * added, so listeners keeping track of conditions see the change.
     *
     * @param conditionBag the new conditions
     */
    public void conditionBag(@NotNull ConditionBag conditionBag) {
        ConditionBag old = this.conditionBag;
        this.conditionBag = conditionBag;
        old.getConditions().forEach(c -> Bukkit.getPluginManager().callEvent(new ConditionRemovedEvent(this, old, c)));
        conditionBag
                .getConditions()
                .forEach(c -> Bukkit.getPluginManager().callEvent(new ConditionAddedEvent(this, conditionBag, c)));
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

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public boolean isLocked() {
        return locked;
    }

    public enum EvaluationType {
        CUSTOM,
        AND,
        OR
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.door.conditioncollections;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.events.ConditionAddedEvent;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.ConditionRemovedEvent;
import de.eldoria.bigdoorsopener.core.exceptions.ConditionCreationException;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.Words;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The conditions of a door, grouped by condition group and scope.
 */
public class ConditionBag implements ConditionCollection {
    private final Map<String, List<DoorCondition>> playerScope = new LinkedHashMap<>();
    private final Map<String, List<DoorCondition>> worldScope = new LinkedHashMap<>();

    private ConditionBag(Collection<DoorCondition> playerScope, Collection<DoorCondition> worldScope) {
        playerScope.forEach(this::addConditionSilent);
        worldScope.forEach(this::addConditionSilent);
    }

    public ConditionBag() {
    }

    /**
     * Creates a bag with the given conditions. Missing conditions are skipped.
     *
     * @param conditions conditions of the bag
     */
    public ConditionBag(Collection<DoorCondition> conditions) {
        conditions.stream().filter(Objects::nonNull).forEach(this::addConditionSilent);
    }

    public void setCondition(ConditionalDoor door, DoorCondition condition) {
        List<DoorCondition> conditions = getConditions(condition);
        for (DoorCondition doorCondition : conditions) {
            Bukkit.getPluginManager().callEvent(new ConditionRemovedEvent(door, this, doorCondition));
        }
        conditions.clear();
        conditions.add(condition);
        Bukkit.getPluginManager().callEvent(new ConditionAddedEvent(door, this, condition));
    }

    public void addCondition(ConditionalDoor door,DoorCondition condition) {
        getConditions(condition).add(condition);
        Bukkit.getPluginManager().callEvent(new ConditionAddedEvent(door, this, condition));
    }

    public void addConditionSilent(DoorCondition condition) {
        getConditions(condition).add(condition);
    }

    public boolean removeCondition(ConditionalDoor door,ConditionGroup group, int index) {
        if (index < 0 || index >= getConditions(group).size()) {
            return false;
        }
        DoorCondition condition = getConditions(group).remove(index);
        Bukkit.getPluginManager().callEvent(new ConditionRemovedEvent(door, this, condition));
        return true;
    }

    public List<DoorCondition> getConditions(DoorCondition condition) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(condition.getClass());
        if (!containerByClass.isPresent()) {
            throw new ConditionCreationException("The requested condition " + condition.getClass().getName() + "is not registered");
        }
        ConditionContainer container = containerByClass.get();
        return getConditions(container.getGroup());
    }

    public List<DoorCondition> getConditions(ConditionGroup group) {
        if (group.getScope() == Scope.PLAYER) {
            return playerScope.computeIfAbsent(group.name(), k -> new LinkedList<>());
        }
        if (group.getScope() == Scope.WORLD) {
            return worldScope.computeIfAbsent(group.name(), k -> new LinkedList<>());
        }
        return getConditions(group.name());
    }

    public List<DoorCondition> getConditions(String group) {
        Optional<ConditionGroup> conditionGroup = ConditionRegistrar.getConditionGroup(group);
        if (conditionGroup.isPresent()) {
            return getConditions(conditionGroup.get());
        }
        throw new IllegalArgumentException("The requested group does not exist.");
    }

    public boolean isConditionSet(ConditionGroup container) {
        return !getConditions(container).isEmpty();
    }

    /**
     * Replaces the condition groups in a custom evaluator with their state. The conditions of a group are combined
     * like the {@code and} evaluator does: conditions without an opinion are skipped, the group is {@code null} when no
     * condition has one. Groups without conditions become {@code null}.
     */
    @Override
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        Map<String, Boolean> groupStates = new HashMap<>();
        for (DoorCondition condition : getConditions()) {
            ConditionContainer container = ConditionRegistrar.getContainerByClass(condition.getClass())
                    .orElseThrow(() -> new ConditionCreationException("The requested condition is not registered"));

            Boolean state;
            if (container.getScope() == Scope.PLAYER && player == null) {
                state = false;
            } else {
                state = condition.isOpen(player, world, door, currentState);
            }
            Boolean groupState = groupStates.get(container.getGroup());
            if (state != null) {
                groupState = groupState == null ? state : groupState && state;
            }
            groupStates.put(container.getGroup(), groupState);
        }

        String evaluationString = Words.replace(string, "currentState", String.valueOf(currentState));
        for (String group : ConditionRegistrar.getGroups()) {
            evaluationString = Words.replace(evaluationString, group, String.valueOf(groupStates.get(group)));
        }
        return evaluationString;
    }

    @Override
    public void evaluated() {
        getConditions().forEach(DoorCondition::evaluated);
    }

    @Override
    public void opened(Player player) {
        getPlayerConditions().forEach(c -> c.opened(player));
    }

    @Override
    public boolean requiresPlayerEvaluation() {
        return !playerScope.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return worldScope.isEmpty() && playerScope.isEmpty();
    }

    @Override
    public ConditionBag copy() {
        return new ConditionBag(
                getPlayerConditions().stream().map(DoorCondition::clone).collect(Collectors.toList()),
                getWorldConditions().stream().map(DoorCondition::clone).collect(Collectors.toList()));
    }

    public Collection<DoorCondition> getPlayerConditions() {
        return playerScope.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public Collection<DoorCondition> getWorldConditions() {
        return worldScope.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public Collection<DoorCondition> getConditions() {
        Collection<DoorCondition> values = new ArrayList<>(getPlayerConditions());
        values.addAll(getWorldConditions());
        return values;
    }
}

package de.eldoria.bigdoorsopener.door.conditioncollections;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionContainer;
import de.eldoria.bigdoorsopener.core.conditions.ConditionGroup;
import de.eldoria.bigdoorsopener.core.conditions.ConditionRegistrar;
import de.eldoria.bigdoorsopener.core.conditions.Scope;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.exceptions.ConditionCreationException;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SerializableAs("bdoConditionBag")
public class ConditionBag implements ConditionCollection {
    private final Map<String, List<DoorCondition>> playerScope = new LinkedHashMap<>();
    private final Map<String, List<DoorCondition>> worldScope = new LinkedHashMap<>();

    private ConditionBag(Collection<DoorCondition> playerScope, Collection<DoorCondition> worldScope) {
        playerScope.forEach(this::addCondition);
        worldScope.forEach(this::addCondition);
    }

    public ConditionBag() {
    }

    public ConditionBag(Map<String, Object> map) {
        TypeResolvingMap typeResolvingMap = SerializationUtil.mapOf(map);
        List<DoorCondition> conditions = typeResolvingMap.getValueOrDefault("conditions", Collections.emptyList());
        conditions.forEach(this::addCondition);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationUtil.newBuilder().add("conditions", getConditions()).build();
    }

    public void setCondition(DoorCondition condition) {
        List<DoorCondition> conditions = getConditions(condition);
        conditions.clear();
        conditions.add(condition);
        Bukkit.getPluginManager().callEvent(new ConditionBagModifiedEvent(this));
    }

    public void addCondition(DoorCondition condition) {
        getConditions(condition).add(condition);
        Bukkit.getPluginManager().callEvent(new ConditionBagModifiedEvent(this));
    }

    public boolean removeCondition(ConditionGroup group, int index) {
        if (getConditions(group).size() < index) {
            return false;
        }
        getConditions(group).remove(index);
        Bukkit.getPluginManager().callEvent(new ConditionBagModifiedEvent(this));
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
            return playerScope.computeIfAbsent(group.getName(), k -> new LinkedList<>());
        }
        if (group.getScope() == Scope.WORLD) {
            return worldScope.computeIfAbsent(group.getName(), k -> new LinkedList<>());
        }
        return getConditions(group.getName());
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

    @Override
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorCondition condition : getConditions()) {
            Boolean state;

            Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(condition.getClass());
            if (!containerByClass.isPresent()) {
                throw new ConditionCreationException("The requested condition is not registered");
            }
            ConditionContainer container = containerByClass.get();

            if (container.getScope() == Scope.PLAYER && player == null) {
                state = false;
            } else {
                state = condition.isOpen(player, world, door, currentState);
            }
            evaluationString = evaluationString.replaceAll("(?i)" + container.getGroup(),
                    String.valueOf(state));
        }

        evaluationString = evaluationString.replaceAll("(?i)currentState",
                String.valueOf(currentState));

        // make sure that calculation does not fail even when the condition is not set.
        for (String value : ConditionRegistrar.getGroups()) {
            evaluationString = evaluationString.replaceAll("(?i)" + value, "null");
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

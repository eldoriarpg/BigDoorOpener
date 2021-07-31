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
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
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
        playerScope.forEach(this::addConditionSilent);
        worldScope.forEach(this::addConditionSilent);
    }

    public ConditionBag() {
    }

    public ConditionBag(Map<String, Object> objectMap) {
        TypeResolvingMap typeResolvingMap = SerializationUtil.mapOf(objectMap);
        ArrayList<DoorCondition> conditions = typeResolvingMap.getValueOrDefault("conditions", new ArrayList<>());
        for (DoorCondition condition : conditions) {
            if (condition == null) {
                BigDoorsOpener.logger().fine("Condition is null. Skipping.");
                continue;
            }
            BigDoorsOpener.logger().fine("Added condition \"" + condition.getClass().getSimpleName() + "\" to condition bag");
            addConditionSilent(condition);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        BigDoorsOpener.logger().fine("Saving Condition bag with " + getConditions().size() + " conditions.");
        getConditions().forEach(c -> BigDoorsOpener.logger().fine("Saving Condition \"" + c.getClass().getSimpleName() + "\"."));
        return SerializationUtil.newBuilder()
                .add("conditions", new ArrayList<>(getConditions()))
                .build();
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
        if (getConditions(group).size() < index) {
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

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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@SerializableAs("bdoConditionBag")
public class ConditionBag implements ConditionCollection {
    private final Map<String, DoorCondition> playerScope = new TreeMap<>();
    private final Map<String, DoorCondition> worldScope = new TreeMap<>();

    private ConditionBag(Collection<DoorCondition> playerScope, Collection<DoorCondition> worldScope) {
        playerScope.forEach(c -> {
            ConditionRegistrar.getContainerByClass(c.getClass()).ifPresent(g -> this.playerScope.put(g.getGroup(), c));
        });
        worldScope.forEach(c -> {
            ConditionRegistrar.getContainerByClass(c.getClass()).ifPresent(g -> this.worldScope.put(g.getGroup(), c));
        });
    }

    public ConditionBag() {

    }

    public ConditionBag(Map<String, Object> map) {
        TypeResolvingMap typeResolvingMap = SerializationUtil.mapOf(map);
        List<DoorCondition> conditions = (List<DoorCondition>) typeResolvingMap.get("conditions");
        conditions.forEach(this::putCondition);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Collection<DoorCondition> values = playerScope.values();
        values.addAll(worldScope.values());
        return SerializationUtil.newBuilder().add("conditions", values).build();
    }

    public void putCondition(DoorCondition condition) {
        Optional<ConditionContainer> containerByClass = ConditionRegistrar.getContainerByClass(condition.getClass());
        if (!containerByClass.isPresent()) {
            throw new ConditionCreationException("The requested condition " + condition.getClass().getName() + "is not registered");
        }
        ConditionContainer container = containerByClass.get();
        if (container.getScope() == Scope.PLAYER) {
            playerScope.put(container.getGroup(), condition);
        } else {
            worldScope.put(container.getGroup(), condition);
        }
        Bukkit.getPluginManager().callEvent(new ConditionBagModifiedEvent(this));
    }

    public boolean removeCondition(ConditionGroup container) {
        boolean result = playerScope.remove(container.getName()) != null || worldScope.remove(container.getName()) != null;
        Bukkit.getPluginManager().callEvent(new ConditionBagModifiedEvent(this));
        return result;
    }

    public Optional<DoorCondition> getCondition(ConditionGroup container) {
        return getCondition(container.getName());
    }

    public Optional<DoorCondition> getCondition(String group) {
        Optional<DoorCondition> worldCon = Optional.ofNullable(worldScope.get(group));
        if (worldCon.isPresent()) return worldCon;
        return Optional.ofNullable(playerScope.get(group));
    }

    public boolean isConditionSet(ConditionGroup container) {
        return getCondition(container).isPresent();
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
        playerScope.values().forEach(DoorCondition::evaluated);
        worldScope.values().forEach(DoorCondition::evaluated);
    }

    @Override
    public void opened(Player player) {
        playerScope.values().forEach(c -> c.opened(player));
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
                playerScope.values().stream().map(DoorCondition::clone).collect(Collectors.toList()),
                worldScope.values().stream().map(DoorCondition::clone).collect(Collectors.toList()));
    }

    @Override
    public Collection<DoorCondition> getConditions() {
        Collection<DoorCondition> values = playerScope.values();
        values.addAll(worldScope.values());
        return values;
    }
}

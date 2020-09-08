package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.doors.Condition;
import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.eldoutilities.serialization.SerializationUtil;
import de.eldoria.eldoutilities.serialization.TypeResolvingMap;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ConditionBag implements ConditionCollection {
    private final Map<String, DoorCondition> playerScope = new TreeMap<>();
    private final Map<String, DoorCondition> worldScope = new TreeMap<>();

    private ConditionBag(Collection<DoorCondition> playerScope, Collection<DoorCondition> worldScope) {
        playerScope.forEach(c -> this.playerScope.put(ConditionHelper.getGroup(c.getClass()), c));
        worldScope.forEach(c -> this.worldScope.put(ConditionHelper.getGroup(c.getClass()), c));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Collection<DoorCondition> values = playerScope.values();
        values.addAll(worldScope.values());
        return SerializationUtil.newBuilder().add("conditions", values).build();
    }

    public ConditionBag() {

    }

    public ConditionBag(Map<String, Object> map) {
        TypeResolvingMap typeResolvingMap = SerializationUtil.mapOf(map);
        List<DoorCondition> conditions = (List<DoorCondition>) typeResolvingMap.get("conditions");
        conditions.forEach(this::putCondition);
    }

    public void putCondition(DoorCondition condition) {
        if (ConditionHelper.isCondition(condition.getClass())) return;
        Condition.Scope scope = ConditionHelper.getScope(condition.getClass());
        if (scope == Condition.Scope.PLAYER) {
            playerScope.put(ConditionHelper.getGroup(condition.getClass()), condition);
        }
        if (scope == Condition.Scope.WORLD) {
            worldScope.put(ConditionHelper.getGroup(condition.getClass()), condition);
        }
    }

    public boolean removeCondition(Class<? extends DoorCondition> clazz) {
        if (ConditionHelper.isCondition(clazz)) return false;
        Condition.Scope scope = ConditionHelper.getScope(clazz);
        if (scope == Condition.Scope.PLAYER) {
            return playerScope.remove(ConditionHelper.getGroup(clazz)) != null;
        }
        if (scope == Condition.Scope.WORLD) {
            return worldScope.remove(ConditionHelper.getGroup(clazz)) != null;
        }
        return false;
    }

    public Optional<DoorCondition> getCondition(Class<? extends DoorCondition> clazz) {
        if (ConditionHelper.isCondition(clazz)) return Optional.empty();

        switch (ConditionHelper.getScope(clazz)) {
            case WORLD:
                return Optional.ofNullable(worldScope.get(ConditionHelper.getGroup(clazz)));
            case PLAYER:
                return Optional.ofNullable(playerScope.get(ConditionHelper.getGroup(clazz)));
            default:
                throw new IllegalStateException("Unexpected value: " + ConditionHelper.getScope(clazz));
        }
    }

    public boolean isConditionSet(Class<? extends DoorCondition> clazz) {
        return getCondition(clazz).isPresent();
    }

    @Override
    public String custom(String string, Player player, World world, ConditionalDoor door, boolean currentState) {
        String evaluationString = string;

        for (DoorCondition condition : getConditions()) {
            Boolean state;

            if (ConditionHelper.getScope(condition.getClass()) == Condition.Scope.PLAYER && player == null) {
                state = false;
            } else {
                state = condition.isOpen(player, world, door, currentState);
            }
            evaluationString = evaluationString.replaceAll("(?i)" + ConditionHelper.getGroup(condition.getClass()),
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

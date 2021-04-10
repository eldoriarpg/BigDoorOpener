package de.eldoria.bigdoorsopener.core.conditions;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConditionGroup {
    private final Map<String, ConditionContainer> conditions = new HashMap<>();
    @Getter
    private final String name;

    public ConditionGroup(String name) {
        this.name = name;
    }

    void addCondition(ConditionContainer condition) {
        conditions.put(condition.getName(), condition);
    }

    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions.keySet());
    }

    public Optional<ConditionContainer> getConditionByName(String name) {
        return Optional.ofNullable(conditions.get(name));
    }

    public Scope getScope() {
        return conditions.values().stream().findFirst().map(ConditionContainer::getScope).orElse(null);
    }
}

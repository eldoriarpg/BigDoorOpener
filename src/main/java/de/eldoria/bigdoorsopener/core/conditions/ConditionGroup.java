/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.conditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConditionGroup {
    private final Map<String, ConditionContainer> conditions = new HashMap<>();
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

    public String name() {
        return name;
    }
}

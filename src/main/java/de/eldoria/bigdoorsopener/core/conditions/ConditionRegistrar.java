package de.eldoria.bigdoorsopener.core.conditions;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.exceptions.ConditionRegistrationException;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConditionRegistrar {
    private static final Map<String, ConditionGroup> GROUPS = new HashMap<>();
    private static final Map<Class<? extends DoorCondition>, ConditionContainer> CONTAINER = new HashMap<>();

    private ConditionRegistrar() {
    }

    /**
     * This will register a condition container at the condition registrat.
     * <p>The provided class in the container requires the {@link SerializableAs} annotation.
     *
     * @param conditionContainer condition container to register
     *
     * @throws ConditionRegistrationException when the condition or its serialized name is already implemented
     * @throws IllegalStateException          when the annotation {@link SerializableAs} is not present
     */
    public static void registerCondition(ConditionContainer conditionContainer) throws ConditionRegistrationException, IllegalStateException {
        GROUPS.computeIfAbsent(conditionContainer.getGroup(), ConditionGroup::new).addCondition(conditionContainer);

        if (CONTAINER.containsKey(conditionContainer.getClazz())) {
            throw new ConditionRegistrationException(conditionContainer.getClazz());
        }

        CONTAINER.put(conditionContainer.getClazz(), conditionContainer);

        ConfigurationSerialization.registerClass(conditionContainer.getClazz());
    }

    /**
     * Get a condition container by class. Only registered classes will return a container. Subclasses and interfaces
     * are ambigous and will not return a container.
     *
     * @param clazz class to request
     *
     * @return optional condition container
     */
    public static Optional<ConditionContainer> getContainerByClass(Class<? extends DoorCondition> clazz) {
        return Optional.ofNullable(CONTAINER.get(clazz));
    }

    /**
     * Get the condition group with the provided name.
     *
     * @param string name of group
     *
     * @return optional group with group if present
     */
    public static Optional<ConditionGroup> getConditionGroup(String string) {
        for (Map.Entry<String, ConditionGroup> entry : GROUPS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(string)) {
                return Optional.ofNullable(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Get all registered condition groups
     *
     * @return set of groups
     */
    public static Set<String> getGroups() {
        return Collections.unmodifiableSet(GROUPS.keySet());
    }

    /**
     * Get all registered conditions
     *
     * @return set of conditions.
     */
    public static Set<String> getConditions() {
        Set<String> conditions = new HashSet<>();
        GROUPS.values().forEach(c -> conditions.addAll(c.getConditions()));
        return Collections.unmodifiableSet(conditions);
    }

    /**
     * Get a condition by name.
     *
     * @param name name of condition
     *
     * @return option condition container
     */
    public static Optional<ConditionContainer> getConditionByName(String name) {
        for (ConditionGroup group : GROUPS.values()) {
            for (String condition : group.getConditions()) {
                if (condition.equalsIgnoreCase(name)) {
                    return group.getConditionByName(name);
                }
            }
        }
        return Optional.empty();
    }

}

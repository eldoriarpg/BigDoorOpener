package de.eldoria.bigdoorsopener.doors.conditions;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static de.eldoria.bigdoorsopener.doors.conditions.ConditionHelper.getGroup;
import static de.eldoria.bigdoorsopener.doors.conditions.ConditionHelper.isCondition;
import static de.eldoria.bigdoorsopener.doors.conditions.ConditionHelper.serializedName;
import static io.lumine.xikage.mythicmobs.util.reflections.util.Utils.name;

public final class ConditionRegistrar {
    private static final Map<String, ConditionGroup> GROUPS = new HashMap<>();

    private ConditionRegistrar() {
    }

    public static void registerCondition(Function<String, ? extends DoorCondition> factory) {

    }

    public static void registerCondition(Class<? extends DoorCondition> conClazz) {
        if (isCondition(conClazz)) return;

        GROUPS.computeIfAbsent(getGroup(conClazz), k -> new ConditionGroup()).addCondition(conClazz);

        Class<? extends ConfigurationSerializable> clazz = ConfigurationSerialization.getClassByAlias(serializedName(conClazz));
        if (clazz != null) {
            BigDoorsOpener.logger().warning("Could not register class "
                    + conClazz.getPackage() + conClazz.getPackage()
                    + ".\nThe serialized name is already used by " + clazz.getPackage() + clazz.getPackage());
        }

        ConfigurationSerialization.registerClass(conClazz, serializedName(conClazz));
    }

    public static void unregisterCondition(Class<? extends DoorCondition> conClazz) {
        if (isCondition(conClazz)) return;

        ConfigurationSerialization.unregisterClass(conClazz);

        GROUPS.computeIfPresent(getGroup(conClazz), (k, v) -> v.removeCondion(conClazz));
    }

    private static boolean isRegistered(Class<? extends DoorCondition> conClazz) {
        if (GROUPS.containsKey(getGroup(conClazz))) {
            return GROUPS.get(getGroup(conClazz)).isInGroup(conClazz);
        }
        return false;
    }


    public static Set<String> getGroups() {
        return Collections.unmodifiableSet(GROUPS.keySet());
    }

    public static Set<String> getConditions() {
        Set<String> conditions = new HashSet<>();
        GROUPS.values().forEach(c -> conditions.addAll(c.getConditions()));
        return Collections.unmodifiableSet(conditions);
    }

    public static Optional<Class<? extends DoorCondition>> getConditionByName(String name) {
        for (ConditionGroup group : GROUPS.values()) {
            for (String condition : group.getConditions()) {
                if (condition.equalsIgnoreCase(name)) {
                    return group.getConditionByName(name);
                }
            }
        }
        return Optional.empty();
    }

    private static class ConditionGroup {
        private final Map<String, Class<? extends DoorCondition>> conditions = new HashMap<>();

        void addCondition(Class<? extends DoorCondition> condition) {
            conditions.put(name(condition), condition);
        }

        /**
         * Removes a registered condition.
         *
         * @param condition condition to unregister
         * @return condition group instance or null if the group is now empty
         */
        ConditionGroup removeCondion(Class<? extends DoorCondition> condition) {
            conditions.remove(name(condition));
            if (conditions.isEmpty()) return null;
            return this;
        }

        public boolean isInGroup(Class<? extends DoorCondition> condition) {
            return conditions.containsValue(condition);
        }

        public Set<String> getConditions() {
            return Collections.unmodifiableSet(conditions.keySet());
        }

        public Optional<Class<? extends DoorCondition>> getConditionByName(String name) {
            return Optional.ofNullable(conditions.get(name));
        }
    }

}

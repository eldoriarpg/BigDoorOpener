package de.eldoria.bigdoorsopener.util.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class KebabNamingStrategy implements NamingStrategy {
    private static final Map<Class<?>, String> keyLookupCache = new HashMap<>();
    private static final Pattern pattern = Pattern.compile("([a-z0-9])([A-Z])");

    @Override
    public String adapt(Class<?> type) {
        Class<?> actualType = type;
        if (type.isAnonymousClass()) {
            actualType = type.getSuperclass();
        }
        return keyLookupCache.computeIfAbsent(actualType, clazz -> {
            String configKey = annotation(clazz).map(ConfigKey::value).orElse(clazz.getSimpleName());
            return pattern.matcher(configKey).replaceAll("$1-$2").toLowerCase();
        });
    }

    private static Optional<ConfigKey> annotation(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(ConfigKey.class));
    }
}
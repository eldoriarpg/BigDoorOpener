package de.eldoria.bigdoorsopener.util.serialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SerializationUtil {
    private static final NamingStrategy namingStrategy = new KebabNamingStrategy();

    private SerializationUtil() {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static <T, U> BiFunction<T, U, String> keyToString() {
        return (k, v) -> k.toString();
    }

    public static <T, U> BiFunction<T, U, Object> valueOnly(Function<U, ?> valueFunction) {
        return (k, v) -> valueFunction.apply(v);
    }

    public static <T, U> BiFunction<T, U, String> keyToPrefixedString(String prefix) {
        return (k, v) -> prefix + k.toString();
    }

    public static TypeResolvingMap mapOf(Map<String, Object> serialized) {
        return new TypeResolvingMap(serialized);
    }

    public static final class Builder {
        private final Map<String, Object> serialized = new LinkedHashMap<>();

        public Builder add(String key, Object value) {
            this.serialized.put(key, value);
            return this;
        }

        public <T> Builder add(String key, T value, Function<T, String> toString) {
            return add(key, toString.apply(value));
        }

        public Builder add(String key, Enum<?> enumValue) {
            return add(key, enumValue.name());
        }

        public Builder add(String key, Collection<?> collection) {
            this.serialized.put(key, new ArrayList<>(collection)); // serialize collection as list
            return this;
        }

        public Builder add(Object value) {
            return add(namingStrategy.adapt(value.getClass()), value);
        }

        public Builder add(Enum<?> enumValue) {
            return add(namingStrategy.adapt(enumValue.getClass()), enumValue);
        }

        public <K, V> Builder add(Map<K, V> map, BiFunction<K, V, String> keyFunction,
                                  BiFunction<K, V, Object> valueFunction) {
            map.forEach((k, v) -> add(keyFunction.apply(k, v), valueFunction.apply(k, v)));
            return this;
        }

        public Map<String, Object> build() {
            return this.serialized;
        }
    }

}
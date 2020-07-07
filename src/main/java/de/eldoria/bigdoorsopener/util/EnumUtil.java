package de.eldoria.bigdoorsopener.util;

public final class EnumUtil {
    private EnumUtil() {
    }

    public static  <T extends Enum<T>> T parse(String string, T[] values, boolean stripStrings) {
        for (T value : values) {
            if (string.equalsIgnoreCase(stripStrings ? value.name().replace("_", "") : value.name())) {
                return value;
            }
        }
        return null;
    }
}

package de.eldoria.bigdoorsopener.util;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class Parser {
    public static OptionalInt parseInt(String s) {
        try {
            return OptionalInt.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public static OptionalDouble parseDouble(String s) {
        try {
            return OptionalDouble.of(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Parses a time in format H24:mm to ticks.
     *
     * @param s string to parse
     * @return time as ticks or null if value could not be parsed.
     */
    public static OptionalInt parseTimeToTicks(String s) {
        String[] split = s.split(":");
        if (split.length != 2) return OptionalInt.empty();
        OptionalInt hour = parseInt(split[0]);
        OptionalInt min = parseInt(split[1]);

        if (!hour.isPresent() || !min.isPresent()) return OptionalInt.empty();

        int hourTicks = (hour.getAsInt() - 6) * 1000 % 24000;
        if (hourTicks < 0) hourTicks = 24000 + hourTicks;
        int minTicks = (int) Math.floor(1000 / 60d * min.getAsInt());

        return OptionalInt.of(hourTicks + minTicks);
    }

    public static String parseTicksToTime(long ticks) {
        long time = ticks % 24000;
        int hours = ((int) Math.floor(time / 1000d) + 6) % 24;
        int min = (int) Math.floor(((time % 1000) + 1) / (1000 / 60d));
        if (min < 10) return hours + ":0" + min;
        return hours + ":" + min;
    }
}

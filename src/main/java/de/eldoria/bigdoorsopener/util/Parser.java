package de.eldoria.bigdoorsopener.util;

public class Parser {
    public static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a time in format H24:mm to ticks.
     *
     * @param s string to parse
     * @return time as ticks or null if value could not be parsed.
     */
    public static Integer parseTimeToTicks(String s) {
        String[] split = s.split(":");
        if (split.length != 2) return null;
        Integer hour = parseInt(split[0]);
        Integer min = parseInt(split[1]);

        if (hour == null || min == null) return null;

        int hourTicks = (hour - 6) * 1000 % 24000;
        if (hourTicks < 0) hourTicks = 24000 + hourTicks;
        int minTicks = (int) Math.floor(1000 / 60d * min);

        return hourTicks + minTicks;
    }

    public static String parseTicksToTime(long ticks) {
        long time = ticks % 24000;
        int hours = ((int) Math.floor(time / 1000d) + 6) % 24;
        int min = (int) Math.floor(((time % 1000) + 1) / (1000 / 60d));
        if (min < 10) return hours + ":0" + min;
        return hours + ":" + min;
    }
}

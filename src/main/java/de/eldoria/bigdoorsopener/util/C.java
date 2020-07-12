package de.eldoria.bigdoorsopener.util;

import net.kyori.adventure.text.format.TextColor;

/**
 * Pure utility class to save some global constants.
 */
public final class C {
    private C() {
    }

    public static TextColor baseColor = TextColor.of(0,170,0);
    public static TextColor highlightColor = TextColor.of(255,170,0);
}

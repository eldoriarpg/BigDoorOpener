package de.eldoria.bigdoorsopener.util;

/**
 * Creates a immutable pair of two values
 *
 * @param <A> value A
 * @param <B> value B
 */
public class Pair<A, B> {
    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

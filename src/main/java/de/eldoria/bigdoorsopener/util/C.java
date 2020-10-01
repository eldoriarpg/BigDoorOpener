package de.eldoria.bigdoorsopener.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Pure utility class to save some global constants.
 */
public final class C {
    /**
     * A chaotic cache. Handle with care. ANARCHY!
     */
    public static final Cache<String, List<?>> PLUGIN_CACHE = C.getExpiringCache(30, TimeUnit.SECONDS);
    public static TextColor baseColor = TextColor.of(0, 170, 0);
    public static TextColor highlightColor = TextColor.of(255, 170, 0);

    private C() {
    }

    public static <A, B> LoadingCache<A, B> getRefreshingCache(Function<A, B> function) {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .refreshAfterWrite(5, TimeUnit.SECONDS)
                .build(new CacheLoader<A, B>() {
                    @Override
                    public B load(A a) throws Exception {
                        return function.apply(a);
                    }
                });
    }

    public static <A, B> LoadingCache<A, B> getRefreshingCache(CacheLoader<A, B> cacheLoader) {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .refreshAfterWrite(5, TimeUnit.SECONDS)
                .build(cacheLoader);
    }

    public static <A, B> Cache<A, B> getExpiringCache() {
        return getExpiringCache(30, TimeUnit.SECONDS);
    }

    public static <A, B> Cache<A, B> getShortExpiringCache() {
        return getExpiringCache(5, TimeUnit.SECONDS);
    }

    public static <A, B> Cache<A, B> getExpiringCache(int duration, TimeUnit timeUnit) {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(duration, timeUnit)
                .build();
    }

    public static <R, A> R nonNullOrElse(A obj, Function<A, R> function, R defaultVal) {
        if (obj != null) {
            return function.apply(obj);
        }
        return defaultVal;
    }
}

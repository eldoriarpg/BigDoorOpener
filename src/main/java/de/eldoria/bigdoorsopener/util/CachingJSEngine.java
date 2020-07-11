package de.eldoria.bigdoorsopener.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eldoria.bigdoorsopener.BigDoorsOpener;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A partly illegal JS engine which can evaluate java scripts.
 * This engine can cache n results defined by cache size of {@link #CachingJSEngine(int)}.
 * As a key the evaluated string is used. This assumes that a result is deterministic, which it should be anyway.
 */
public class CachingJSEngine {
    private final ScriptEngine engine;
    private final Cache<String, Object> cache;

    public CachingJSEngine(int cacheSize) {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine("--no-deprecation-warning");
        ;
        cache = CacheBuilder.newBuilder().expireAfterAccess(24, TimeUnit.MINUTES).maximumSize(500).build();
    }

    /**
     * Evaluates a string with a js engine.
     *
     * @param string       string to evaluate
     * @param defaultValue default value which should be returned if anything goes wrong.
     * @param <T>          type which should be returned
     * @return evaluated value or default value
     */
    @SuppressWarnings("unchecked")
    public <T> T eval(String string, T defaultValue) {
        try {
            return evalUnsafe(string, defaultValue);
        } catch (ScriptException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "An error occurred while evaluating \"" + string + "\"", e);
            return defaultValue;
        } catch (ClassCastException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "Could not map result.", e);
            return defaultValue;
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "Could not compute cache value for " + string + ".", e);
            return defaultValue;
        }
    }

    /**
     * @param string       string to evaluate
     * @param defaultValue default value which should be returned if anything goes wrong.
     * @param <T>          type which should be returned
     * @return evaluated value or default value if evaluated value is null.
     * @throws ExecutionException if a exception occured while creating the cached value
     * @throws ScriptException    if a exception orccured while executing the js script
     * @throws ClassCastException if the received object cant be cast on the requested type
     */
    @SuppressWarnings("unchecked")
    public <T> T evalUnsafe(String string, T defaultValue) throws ExecutionException, ScriptException, ClassCastException {
        Object t = cache.get(string, () -> engine.eval(string));
        if (t == null) return defaultValue;
        return (T) t;
    }
}

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

public class CachingJSEngine {
    private final ScriptEngine engine;
    private final Cache<String, Object> cache;

    public CachingJSEngine(int cacheSize) {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine("--no-deprecation-warning");;
        cache = CacheBuilder.newBuilder().expireAfterAccess(24, TimeUnit.MINUTES).maximumSize(500).build();
    }

    @SuppressWarnings("unchecked")
    public <T> T eval(String string, T defaultValue) {
        try {
            Object t = cache.get(string, () -> {
                try {
                    return engine.eval(string);
                } catch (ScriptException e) {
                    BigDoorsOpener.logger().log(Level.WARNING,
                            "An error occurred while evaluating \"" + string + "\"", e);
                    return null;
                }
            });
            if (t == null) return defaultValue;
            return (T) t;
        } catch (ClassCastException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "Could not map result.", e);
            return defaultValue;
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "Could not compute cache value.", e);
            return defaultValue;
        }
    }
}

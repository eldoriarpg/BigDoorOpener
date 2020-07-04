package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.BigDoorsOpener;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Level;

public class CachingJSEngine {
    private final ScriptEngine engine;
    private final HeatCache<String, Object> cache;

    public CachingJSEngine() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        cache = new HeatCache<>(200);
    }

    @SuppressWarnings("unchecked")
    public <T> T eval(String string, T defaultValue) {
        try {
            Object t = cache.computeIfAbsent(string, (k) -> {
                try {
                    return engine.eval(string);
                } catch (ScriptException e) {
                    BigDoorsOpener.logger().log(Level.WARNING,
                            "An error occurred while evaluating " + string, e);
                    return null;
                }
            });
            if (t == null) return defaultValue;
            return (T) t;
        } catch (ClassCastException e) {
            BigDoorsOpener.logger().log(Level.WARNING,
                    "Could not map result.", e);
            return defaultValue;
        }
    }
}

/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A partly illegal JS engine which can evaluate java scripts. This engine can cache n results defined by cache size of
 * {@link #CachingJSEngine(int)}. As a key the evaluated string is used. This assumes that a result is deterministic,
 * which it should be anyway.
 */
public class CachingJSEngine {
    private final Cache<String, Object> cache;
    private ScriptEngine engine;

    public CachingJSEngine(int cacheSize) {
        engine = findEngine();
        if (engine == null) {
            Logger logger = BigDoorsOpener.logger();
            logger.severe("--------------------------------------------------------------------------");
            logger.severe("-> No script engine found.                                              <-");
            logger.severe("-> Custom evaluator and placeholder will not work.                      <-");
            logger.severe("-> Install the NashornJs plugin to use them.                            <-");
            logger.severe("-> Download here: https://www.spigotmc.org/resources/91204/             <-");
            logger.severe("--------------------------------------------------------------------------");
        } else {
            BigDoorsOpener.logger().info("Using the " + engine.getFactory().getEngineName() + " script engine.");
        }

        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(24, TimeUnit.MINUTES)
                .maximumSize(cacheSize)
                .build();
    }

    /**
     * Looks up a JavaScript engine provided by another plugin, usually NashornJs.
     *
     * @return the engine, or null if no plugin provides one
     */
    private static ScriptEngine findEngine() {
        RegisteredServiceProvider<ScriptEngineManager> registration =
                Bukkit.getServicesManager().getRegistration(ScriptEngineManager.class);
        if (registration == null) return null;
        for (String name : List.of("js", "JavaScript", "nashornjs", "nashorn")) {
            ScriptEngine engine = registration.getProvider().getEngineByName(name);
            if (engine != null) return engine;
        }
        return null;
    }

    public <T> T eval(String string, T defaultValue) {
        try {
            return evalUnsafe(string, defaultValue);
        } catch (ScriptException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "An error occurred while evaluating \"" + string + "\"", e);
            return defaultValue;
        } catch (ClassCastException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not map result.", e);
            return defaultValue;
        } catch (ExecutionException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not compute cache value for " + string + ".", e);
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
    public <T> T evalUnsafe(String string, T defaultValue)
            throws ExecutionException, ScriptException, ClassCastException {
        if (engine == null) return defaultValue;
        Object t = cache.get(string, () -> engine.eval(string));
        if (t == null) return defaultValue;
        return (T) t;
    }
}

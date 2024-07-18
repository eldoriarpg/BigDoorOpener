/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eldoria.bigdoorsopener.core.BigDoorsOpener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A partly illegal JS engine which can evaluate java scripts. This engine can cache n results defined by cache size of
 * {@link #CachingJSEngine(int)}. As a key the evaluated string is used. This assumes that a result is deterministic,
 * which it should be anyway.
 */
public class CachingJSEngine {
    private final Cache<String, Object> cache;
    private ScriptEngine engine;

    public CachingJSEngine(int cacheSize) {
        try {
            BigDoorsOpener.logger().config("Java version: " + System.getProperty("java.specification.version"));
            String[] versionString = System.getProperty("java.specification.version").split("\\.");
            String version = versionString[versionString.length - 1];
            int verInt = Integer.parseInt(version);
            BigDoorsOpener.logger().config("Detected version: " + verInt);
            if (verInt < 11) {
                BigDoorsOpener.logger().info("Detected legacy java version below 11.");
                engine = new ScriptEngineManager().getEngineByName("nashorn");
                BigDoorsOpener.logger().info("Support for java version below 16 will be dropped with the release of MC 1.17.");
            } else if (verInt < 15) {
                BigDoorsOpener.logger().info("Java 11 or newer detected.");
                engine = new ScriptEngineManager().getEngineByName("nashorn");
                BigDoorsOpener.logger().info("Support for java version below 16 will be dropped with the release of MC 1.17.");
            } else {
                BigDoorsOpener.logger().info("Java 15 or newer detected. Searching for external Engine.");
                RegisteredServiceProvider<ScriptEngineManager> registration = Bukkit.getServer().getServicesManager().getRegistration(ScriptEngineManager.class);
                if (registration != null) {
                    for (String currentEngine : List.of("js", "JavaScript", "nashornjs", "nashorn")) {
                        try {
                            engine = registration.getProvider().getEngineByName(currentEngine);
                        } catch (Exception e) {
                            continue;
                        }
                        break;
                    }
                }
                if (engine == null) {
                    BigDoorsOpener.logger().severe("--------------------------------------------------------------------------");
                    BigDoorsOpener.logger().severe("-> No script engine found.                                              <-");
                    BigDoorsOpener.logger().severe("-> Custom evaluator and placeholder will not work.                      <-");
                    BigDoorsOpener.logger().severe("-> When using java 15 or above you have to use the nashorn js provider. <-");
                    BigDoorsOpener.logger().severe("-> Download here: https://www.spigotmc.org/resources/91204/.            <-");
                    BigDoorsOpener.logger().severe("--------------------------------------------------------------------------");
                    engine = null;
                }
            }
            engine.eval("print('[BigDoorsOpener] " + engine.getFactory().getEngineName() + " script engine started.')");
        } catch (ScriptException e) {
            BigDoorsOpener.logger().log(Level.WARNING, "No nashorn script engine found. Trying to use JavaScript fallback.", e);
            engine = new ScriptEngineManager(null).getEngineByName("JavaScript");
            try {
                engine.eval("print('[BigDoorsOpener] JavaScript script engine started.')");
            } catch (ScriptException ex) {
                BigDoorsOpener.logger().log(Level.WARNING, "Could not start script engine. Custom evaluator will not work.", e);
            }
        } catch (Exception e) {
            BigDoorsOpener.logger().log(Level.WARNING, "Could not start script engine. Custom evaluator will not work.", e);
        }

        cache = CacheBuilder.newBuilder().expireAfterAccess(24, TimeUnit.MINUTES).maximumSize(cacheSize).build();
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
        if (engine == null) return defaultValue;
        Object t = cache.get(string, () -> engine.eval(string));
        if (t == null) return defaultValue;
        return (T) t;
    }
}

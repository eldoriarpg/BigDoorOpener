/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config;

import de.eldoria.bigdoorsopener.config.elements.Doors;
import de.eldoria.bigdoorsopener.config.elements.Settings;
import de.eldoria.eldoutilities.config.JacksonConfig;
import dev.chojo.ocular.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;

import java.nio.file.Path;

/**
 * The files of the plugin: the settings in {@code config.yml} and the doors in {@code doors.yml}.
 * <p>
 * A condition of a type which is not registered is read as missing instead of failing the whole doors file.
 */
public class JacksonConfiguration extends JacksonConfig<Settings> {
    public static final Key<Doors> DOORS = Key.builder(Path.of("doors.yml"), Doors::new).name("Doors").build();

    public JacksonConfiguration(@NotNull Plugin plugin) {
        super(plugin, Key.builder(Path.of("config.yml"), Settings::new).build());
    }

    @Override
    public void configure(MapperBuilder<ObjectMapper, ?> builder) {
        super.configure(builder);
        builder.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    }

    public Doors doors() {
        return secondary(DOORS);
    }
}

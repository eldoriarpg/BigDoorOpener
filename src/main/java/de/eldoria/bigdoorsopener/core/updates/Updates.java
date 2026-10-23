/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.updates;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.config.elements.UpdateSettings;
import de.eldoria.bigdoorsopener.util.Permissions;
import de.eldoria.eldoutilities.updater.AutoUpdateDecision;
import de.eldoria.eldoutilities.updater.Updater;
import de.eldoria.eldoutilities.updater.lynaupdater.LynaUpdateData;
import de.eldoria.eldoutilities.updater.lynaupdater.LynaUpdateResponse;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Looks for new Big Doors Opener versions on Eldoria's release service Lyna. A new version is told to the console and,
 * on join, to players with {@link Permissions#UPDATES}. With auto update on, a new release within the current major
 * version is downloaded into the server's update folder and takes effect on the next start. Otherwise admins are
 * offered to download it and to switch auto update on from chat; their choice is written to {@code config.yml}.
 */
public final class Updates {

    private final Plugin plugin;
    private final Supplier<Config> config;
    private @Nullable Updater<LynaUpdateResponse, LynaUpdateData> updater;

    /**
     * @param plugin the plugin whose versions are looked up
     * @param config where the update settings are read and an admin's choice is written
     */
    public Updates(Plugin plugin, Supplier<Config> config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Applies the current update settings: starts looking for new versions, hands the running updater the new
     * settings, or stops it when the settings neither want to hear about new versions nor download them.
     */
    public void apply() {
        UpdateSettings settings = config.get().settings().updates();
        if (!settings.notice() && !settings.autoUpdate()) {
            stop();
            return;
        }
        LynaUpdateData data = data(settings);
        if (updater == null) {
            updater = Updater.lyna(data);
            updater.start();
        } else {
            updater.reconfigure(data);
        }
    }

    /**
     * Stops looking for new versions.
     */
    public void stop() {
        if (updater != null) updater.stop();
        updater = null;
    }

    private LynaUpdateData data(UpdateSettings settings) {
        return LynaUpdateData.builder(plugin)
                .notifyPermission(Permissions.UPDATES)
                .notifyUpdate(settings.notice())
                .autoUpdate(settings.autoUpdate())
                .askForAutoUpdate(settings.askForAutoUpdate())
                .autoUpdateHook(this::decided)
                .build();
    }

    private void decided(AutoUpdateDecision decision) {
        UpdateSettings settings = config.get().settings().updates();
        switch (decision) {
            case ENABLED -> settings.autoUpdate(true);
            case NEVER_ASK -> settings.askForAutoUpdate(false);
            default -> {
                return;
            }
        }
        config.get().save();
    }
}

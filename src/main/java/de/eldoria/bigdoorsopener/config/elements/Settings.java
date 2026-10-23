/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config.elements;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The settings in {@code config.yml}.
 * <p>
 * {@code version} is 1 for configurations written by 2.x, which still hold the doors, and 2 since the doors live in
 * {@code doors.yml}. A configuration without version was written by 1.x.
 */
@SuppressWarnings("FieldMayBeFinal")
public class Settings {
    public static final int CURRENT_VERSION = 2;

    private int version = CURRENT_VERSION;
    private int refreshRate = 20;
    private String language = "en_US";
    private boolean enableMetrics = true;
    private int jsCacheSize = 400;
    private int playerCheckRadius = 200;
    private UpdateSettings updates = new UpdateSettings();

    public int version() {
        return version;
    }

    public void version(int version) {
        this.version = version;
    }

    /**
     * @return ticks in which every door is checked once
     */
    public int refreshRate() {
        return refreshRate;
    }

    public String language() {
        return language;
    }

    public boolean enableMetrics() {
        return enableMetrics;
    }

    /**
     * @return amount of evaluated JavaScript expressions which are cached
     */
    public int jsCacheSize() {
        return jsCacheSize;
    }

    /**
     * @return distance in blocks in which players are checked by player conditions
     */
    public int playerCheckRadius() {
        return playerCheckRadius;
    }

    public UpdateSettings updates() {
        return updates;
    }

    /**
     * Takes the update check switch of configurations written before 3.0.0 as the start value of
     * {@link UpdateSettings#notice()}. The old key is only read and never written again.
     *
     * @param checkUpdates whether the old configuration looked for updates
     */
    @JsonSetter("checkUpdates")
    private void legacyCheckUpdates(boolean checkUpdates) {
        updates.notice(checkUpdates);
    }
}

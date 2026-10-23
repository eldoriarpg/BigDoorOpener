/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config.elements;

/**
 * Whether the server is told about new Big Doors Opener versions and whether they are downloaded.
 */
@SuppressWarnings("FieldMayBeFinal")
public class UpdateSettings {
    private boolean notice = true;
    private boolean autoUpdate = false;
    private boolean askForAutoUpdate = true;

    /**
     * @return whether the console and admins hear about a new version
     */
    public boolean notice() {
        return notice;
    }

    /**
     * @param notice whether the console and admins hear about a new version
     */
    public void notice(boolean notice) {
        this.notice = notice;
    }

    /**
     * @return whether a new release is downloaded, to take effect on the next start
     */
    public boolean autoUpdate() {
        return autoUpdate;
    }

    /**
     * @param autoUpdate whether a new release is downloaded, to take effect on the next start
     */
    public void autoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    /**
     * @return whether admins are asked in chat to switch auto update on
     */
    public boolean askForAutoUpdate() {
        return askForAutoUpdate;
    }

    /**
     * @param askForAutoUpdate whether admins are asked in chat to switch auto update on
     */
    public void askForAutoUpdate(boolean askForAutoUpdate) {
        this.askForAutoUpdate = askForAutoUpdate;
    }
}

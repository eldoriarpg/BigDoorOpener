/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.exceptions;

public class PluginInitFailed extends RuntimeException {
    public PluginInitFailed(String message) {
        super(message);
    }
}

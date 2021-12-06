/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.listener.registration;

import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Interface which can be passed to {@link RegisterInteraction} for object calls.
 */
public interface InteractionRegistrationObject {
    /**
     * This method is called every time a player interacts with anything.
     *
     * @param event interaction event of the player.
     * @return true if the registration is done and the object can be removed.
     */
    boolean invoke(PlayerInteractEvent event, MessageSender messageSender);
}

package de.eldoria.bigdoorsopener.listener.registration;

import org.bukkit.event.player.PlayerInteractEvent;

public interface InteractionRegistrationObject {
    /**
     * This method is called every time a player interacts with anything.
     *
     * @param event interaction event of the player.
     * @return true if the registration is done and the object can be removed.
     */
    boolean register(PlayerInteractEvent event);
}
package de.eldoria.bigdoorsopener.listener.registration;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This listener allows to execute a call on a object which tries further actions though player interactions.
 */
public class RegisterInteraction implements Listener {
    private final Map<UUID, InteractionRegistrationObject> registerObjects = new HashMap<>();

    public void onPlayerInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (registerObjects.containsKey(event.getPlayer().getUniqueId())) {
            InteractionRegistrationObject registrationObject = registerObjects.get(event.getPlayer().getUniqueId());
            boolean register = registrationObject.register(event);
            if (register) {
                registerObjects.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    /**
     * Register a new registration object for a user.
     *
     * @param player  player for which the registration process should be started.
     * @param wrapper wrapper which holds the action to check registration.
     */
    public void register(Player player, InteractionRegistrationObject wrapper) {
        registerObjects.put(player.getUniqueId(), wrapper);
    }

    /**
     * Cancel the registration for a player
     *
     * @param player player to cancel the registration for
     * @return true if the user had a registered registration process.
     */
    public boolean unregister(Player player) {
        return registerObjects.remove(player.getUniqueId()) != null;
    }
}

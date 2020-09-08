package de.eldoria.bigdoorsopener.listener.registration;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This listener allows to execute a call on a object which tries further actions though player interactions.
 */
public class RegisterInteraction implements Listener {
    private final Map<UUID, InteractionRegistrationObject> registerObjects = new HashMap<>();
    private final MessageSender messageSender;
    private final Config config;

    public RegisterInteraction(MessageSender messageSender, Config config) {
        this.messageSender = messageSender;
        this.config = config;
    }

    @EventHandler
    public void onPlayerInteraction(PlayerInteractEvent event) {
        if (registerObjects.containsKey(event.getPlayer().getUniqueId())) {
            InteractionRegistrationObject registrationObject = registerObjects.get(event.getPlayer().getUniqueId());
            if (registrationObject.invoke(event, messageSender)) {
                registerObjects.remove(event.getPlayer().getUniqueId());
                config.safeConfig();
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

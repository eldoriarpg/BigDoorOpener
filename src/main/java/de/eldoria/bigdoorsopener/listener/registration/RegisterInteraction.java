package de.eldoria.bigdoorsopener.listener.registration;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterInteraction implements Listener {
    private final Map<UUID, InteractionRegistrationObject> registerObjects = new HashMap<>();

    public void onPlayerInteraction(PlayerInteractEvent event) {

    }

    public void register(Player player, InteractionRegistrationObject wrapper) {
        registerObjects.put(player.getUniqueId(), wrapper);
    }
}

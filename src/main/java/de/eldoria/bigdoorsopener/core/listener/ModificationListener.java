package de.eldoria.bigdoorsopener.core.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorRegisteredEvent;
import de.eldoria.bigdoorsopener.core.events.DoorUnregisteredEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ModificationListener implements Listener {
    private final Config config;

    public ModificationListener(Config config) {
        this.config = config;
    }

    @EventHandler
    public void onConditionBagModification(ConditionBagModifiedEvent event) {
        config.safeConfig();
    }

    @EventHandler
    public void onDoorModifiedEvent(DoorModifiedEvent event) {
        config.safeConfig();
    }
    @EventHandler
    public void onDoorRegisteredEvent(DoorRegisteredEvent event) {
        config.safeConfig();
    }
    @EventHandler
    public void onDoorUnregisteredEvent(DoorUnregisteredEvent event) {
        config.safeConfig();
    }
}

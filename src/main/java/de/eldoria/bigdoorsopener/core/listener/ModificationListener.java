package de.eldoria.bigdoorsopener.core.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.events.ConditionBagModifiedEvent;
import de.eldoria.bigdoorsopener.core.events.DoorModifiedEvent;
import org.bukkit.event.Listener;

public class ModificationListener implements Listener {
    private final Config config;

    public ModificationListener(Config config) {
        this.config = config;
    }

    public void onConditionBagModification(ConditionBagModifiedEvent event) {
        config.safeConfig();
    }

    public void onDoorModifiedEvent(DoorModifiedEvent event) {
        config.safeConfig();
    }
}

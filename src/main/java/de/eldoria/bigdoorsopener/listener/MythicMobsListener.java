package de.eldoria.bigdoorsopener.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.scheduler.BigDoorsAdapter;
import de.eldoria.eldoutilities.localization.Localizer;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobsListener extends BigDoorsAdapter implements Listener {

    private final Config config;

    public MythicMobsListener(BigDoors bigDoors, Localizer localizer, Config config) {
        super(bigDoors, localizer);
        this.config = config;
    }

    @EventHandler
    public void onMobDeath(MythicMobDeathEvent event) {
        config.getDoors().values().forEach(d -> {
            if (d.getConditionChain().getMythicMob() == null) return;
            d.getConditionChain().getMythicMob().killed(event, isAvailableToOpen(d));
        });
    }
}

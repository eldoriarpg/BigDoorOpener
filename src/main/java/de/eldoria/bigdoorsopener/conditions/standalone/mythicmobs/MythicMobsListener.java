package de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class MythicMobsListener extends BigDoorsAdapter implements Listener {

    private final Config config;

    public MythicMobsListener(BigDoors bigDoors, Config config) {
        super(bigDoors);
        this.config = config;
    }

    @EventHandler
    public void onMobDeath(MythicMobDeathEvent event) {
        config.getDoors().forEach(d -> {
            List<DoorCondition> mythicMobs = d.getConditionBag().getConditions("mythicMob");
            if (mythicMobs.isEmpty()) return;
            mythicMobs.forEach(m -> ((MythicMob) m).killed(event, isAvailableToOpen(d)));
        });
    }
}

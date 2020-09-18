package de.eldoria.bigdoorsopener.listener;

import de.eldoria.bigdoorsopener.config.Config;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.conditions.standalone.MythicMob;
import de.eldoria.bigdoorsopener.core.adapter.BigDoorsAdapter;
import de.eldoria.eldoutilities.localization.Localizer;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

public class MythicMobsListener extends BigDoorsAdapter implements Listener {

    private final Config config;

    public MythicMobsListener(BigDoors bigDoors, Localizer localizer, Config config) {
        super(bigDoors);
        this.config = config;
    }

    @EventHandler
    public void onMobDeath(MythicMobDeathEvent event) {
        config.getDoors().forEach(d -> {
            Optional<DoorCondition> mythicMob = d.getConditionBag().getCondition("mythicMob");
            if (!mythicMob.isPresent()) return;
            ((MythicMob) mythicMob.get()).killed(event, isAvailableToOpen(d));
        });
    }
}

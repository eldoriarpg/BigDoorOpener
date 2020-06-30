package de.eldoria.bigdoorsopener.listener;

import de.eldoria.bigdoorsopener.scheduler.TimedDoorScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;


public class TimeSkipListener implements Listener {
    private TimedDoorScheduler scheduler;

    public TimeSkipListener(TimedDoorScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTimeSkip(TimeSkipEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getSkipAmount() > 1000 || event.getSkipAmount() < -1000) {
            scheduler.reload();
        }
    }

    public void reload(TimedDoorScheduler timedDoorScheduler) {
        scheduler = timedDoorScheduler;
    }
}

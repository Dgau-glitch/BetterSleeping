package be.betterplugins.bettersleeping.listeners;

import be.betterplugins.bettersleeping.api.BecomeDayEvent;
import be.betterplugins.bettersleeping.api.BecomeDayEvent.Cause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class TimeSetToDayCounter implements Listener {

    private final AtomicInteger counter = new AtomicInteger();

    @Inject
    public TimeSetToDayCounter() {}

    @EventHandler
    public void onTimeSetToDay(BecomeDayEvent event)
    {
        if (event.getCause() == Cause.SLEEPING)
            counter.incrementAndGet();
    }

    public int resetCounter()
    {
        return counter.getAndSet(0);
    }

}

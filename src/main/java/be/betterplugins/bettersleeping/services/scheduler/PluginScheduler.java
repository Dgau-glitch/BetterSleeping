package be.betterplugins.bettersleeping.services.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

/**
 * Central scheduler abstraction for all Folia task ownership contexts.
 */
public interface PluginScheduler
{
    TaskHandle runGlobal(Runnable task);

    TaskHandle repeatGlobal(Runnable task, long initialDelayTicks, long periodTicks);

    TaskHandle runAtLocation(Location location, Runnable task);

    TaskHandle repeatAtLocation(Location location, Runnable task, long initialDelayTicks, long periodTicks);

    TaskHandle runForEntity(Entity entity, Runnable task);

    TaskHandle runForEntityLater(Entity entity, Runnable task, long delayTicks);

    TaskHandle repeatForEntity(Entity entity, Runnable task, long initialDelayTicks, long periodTicks);

    TaskHandle runAsync(Runnable task);

    TaskHandle repeatAsync(Runnable task, long initialDelay, long period, TimeUnit unit);

    void cancelAll();
}

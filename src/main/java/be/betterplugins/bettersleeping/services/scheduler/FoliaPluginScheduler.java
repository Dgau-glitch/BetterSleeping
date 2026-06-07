package be.betterplugins.bettersleeping.services.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

@Singleton
public class FoliaPluginScheduler implements PluginScheduler
{
    private final JavaPlugin plugin;
    private final GlobalRegionScheduler globalScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    @Inject
    public FoliaPluginScheduler(JavaPlugin plugin)
    {
        this.plugin = plugin;
        Server server = plugin.getServer();
        this.globalScheduler = server.getGlobalRegionScheduler();
        this.regionScheduler = server.getRegionScheduler();
        this.asyncScheduler = server.getAsyncScheduler();
    }

    @Override
    public TaskHandle runGlobal(Runnable task)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(globalScheduler.run(plugin, scheduledTask -> task.run()));
    }

    @Override
    public TaskHandle repeatGlobal(Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(globalScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runAtLocation(Location location, Runnable task)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(regionScheduler.run(plugin, location, scheduledTask -> task.run()));
    }

    @Override
    public TaskHandle repeatAtLocation(Location location, Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(regionScheduler.runAtFixedRate(plugin, location, scheduledTask -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runForEntity(Entity entity, Runnable task)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrapNullable(entity.getScheduler().run(plugin, scheduledTask -> task.run(), null));
    }

    @Override
    public TaskHandle runForEntityLater(Entity entity, Runnable task, long delayTicks)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrapNullable(entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks));
    }

    @Override
    public TaskHandle repeatForEntity(Entity entity, Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrapNullable(entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), null, initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runAsync(Runnable task)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(asyncScheduler.runNow(plugin, scheduledTask -> task.run()));
    }

    @Override
    public TaskHandle repeatAsync(Runnable task, long initialDelay, long period, TimeUnit unit)
    {
        if (!isSchedulingAllowed())
            return new RetiredTaskHandle();

        return wrap(asyncScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), initialDelay, period, unit));
    }

    @Override
    public void cancelAll()
    {
        globalScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }

    private boolean isSchedulingAllowed()
    {
        return plugin.isEnabled();
    }

    private TaskHandle wrap(ScheduledTask scheduledTask)
    {
        return new FoliaTaskHandle(scheduledTask);
    }

    private TaskHandle wrapNullable(ScheduledTask scheduledTask)
    {
        return scheduledTask == null ? new RetiredTaskHandle() : wrap(scheduledTask);
    }
}

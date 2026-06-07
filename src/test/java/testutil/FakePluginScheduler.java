package testutil;

import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FakePluginScheduler implements PluginScheduler
{
    private final boolean runRepeatingImmediately;
    private final List<FakeTaskHandle> handles = new ArrayList<>();

    public FakePluginScheduler()
    {
        this(false);
    }

    public FakePluginScheduler(boolean runRepeatingImmediately)
    {
        this.runRepeatingImmediately = runRepeatingImmediately;
    }

    @Override
    public TaskHandle runGlobal(Runnable task)
    {
        task.run();
        return track();
    }

    @Override
    public TaskHandle repeatGlobal(Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (runRepeatingImmediately)
            task.run();
        return track();
    }

    @Override
    public TaskHandle runAtLocation(Location location, Runnable task)
    {
        task.run();
        return track();
    }

    @Override
    public TaskHandle repeatAtLocation(Location location, Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (runRepeatingImmediately)
            task.run();
        return track();
    }

    @Override
    public TaskHandle runForEntity(Entity entity, Runnable task)
    {
        task.run();
        return track();
    }

    @Override
    public TaskHandle runForEntityLater(Entity entity, Runnable task, long delayTicks)
    {
        task.run();
        return track();
    }

    @Override
    public TaskHandle repeatForEntity(Entity entity, Runnable task, long initialDelayTicks, long periodTicks)
    {
        if (runRepeatingImmediately)
            task.run();
        return track();
    }

    @Override
    public TaskHandle runAsync(Runnable task)
    {
        task.run();
        return track();
    }

    @Override
    public TaskHandle repeatAsync(Runnable task, long initialDelay, long period, TimeUnit unit)
    {
        if (runRepeatingImmediately)
            task.run();
        return track();
    }

    @Override
    public void cancelAll()
    {
        handles.forEach(FakeTaskHandle::cancel);
    }

    public List<FakeTaskHandle> getHandles()
    {
        return handles;
    }

    public FakeTaskHandle getLastHandle()
    {
        return handles.get(handles.size() - 1);
    }

    private FakeTaskHandle track()
    {
        FakeTaskHandle handle = new FakeTaskHandle();
        handles.add(handle);
        return handle;
    }

    public static class FakeTaskHandle implements TaskHandle
    {
        private boolean cancelled;

        @Override
        public void cancel()
        {
            this.cancelled = true;
        }

        @Override
        public boolean isCancelled()
        {
            return cancelled;
        }
    }
}

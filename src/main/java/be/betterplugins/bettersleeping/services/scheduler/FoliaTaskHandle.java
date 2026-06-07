package be.betterplugins.bettersleeping.services.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

final class FoliaTaskHandle implements TaskHandle
{
    private final ScheduledTask task;

    FoliaTaskHandle(ScheduledTask task)
    {
        this.task = task;
    }

    @Override
    public void cancel()
    {
        if (task != null && !task.isCancelled())
            task.cancel();
    }

    @Override
    public boolean isCancelled()
    {
        return task == null || task.isCancelled();
    }
}

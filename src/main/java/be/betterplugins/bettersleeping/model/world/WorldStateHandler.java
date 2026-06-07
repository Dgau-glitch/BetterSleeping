package be.betterplugins.bettersleeping.model.world;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldId;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import be.betterplugins.bettersleeping.services.world.WorldAccessService;
import be.betterplugins.core.messaging.logging.BPLogger;
import com.google.inject.Inject;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.List;

public class WorldStateHandler implements Listener
{

    private final Map<SleepWorldId, WorldState> worldWorldStateMap;
    private final List<TaskHandle> trackedTasks;
    private final PluginScheduler scheduler;
    private final WorldAccessService worldAccessService;
    private final BPLogger logger;
    private volatile WorldState currentTemporaryState;

    @Inject
    public WorldStateHandler(List<World> worlds, PluginScheduler scheduler, WorldAccessService worldAccessService, BPLogger logger)
    {
        this(scheduler, worldAccessService, logger);
        for (World world : worlds)
            captureOriginalState(world);
    }

    WorldStateHandler(Map<SleepWorldId, WorldState> originalStates, PluginScheduler scheduler, WorldAccessService worldAccessService, BPLogger logger)
    {
        this(scheduler, worldAccessService, logger);
        this.worldWorldStateMap.putAll(originalStates);
    }

    private WorldStateHandler(PluginScheduler scheduler, WorldAccessService worldAccessService, BPLogger logger)
    {
        this.scheduler = scheduler;
        this.worldAccessService = worldAccessService;
        this.logger = logger;
        this.worldWorldStateMap = new ConcurrentHashMap<>();
        this.trackedTasks = new CopyOnWriteArrayList<>();
    }

    /**
     * Make temporary world state changes to all worlds.
     */
    public void setWorldStates(WorldState state)
    {
        this.currentTemporaryState = state;
        for (SleepWorldId worldId : worldWorldStateMap.keySet())
            scheduleStateApply(worldId, state, "apply temporary state");
    }

    /**
     * Revert all temporary changes to their original value.
     *
     * <p>This method is called from plugin disable/reload. Folia does not allow
     * new scheduler tasks to be created once shutdown/disable has started, so
     * restoration is performed immediately and no scheduler API is used here.</p>
     */
    public void revertWorldStates()
    {
        this.currentTemporaryState = null;
        cancelTrackedTasks();
        logger.log(Level.FINE, "Restoring original world state immediately for " + worldWorldStateMap.size() + " world(s) during plugin disable/reload");
        for (Map.Entry<SleepWorldId, WorldState> entry : worldWorldStateMap.entrySet())
            applyStateImmediately(entry.getKey(), entry.getValue(), "restore original state");
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event)
    {
        SleepWorldId worldId = captureOriginalState(event.getWorld());
        if (currentTemporaryState != null)
            scheduleStateApply(worldId, currentTemporaryState, "apply temporary state to loaded world");
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event)
    {
        SleepWorldId worldId = worldAccessService.getId(event.getWorld());
        logger.log(Level.FINE, "World " + worldId + " unloaded while BetterSleeping was tracking its original state");
    }

    private SleepWorldId captureOriginalState(World world)
    {
        SleepWorldId worldId = worldAccessService.getId(world);
        this.worldWorldStateMap.put(worldId, new WorldState(world, logger));
        return worldId;
    }

    private void scheduleStateApply(SleepWorldId worldId, WorldState state, String action)
    {
        TaskHandle globalHandle = scheduler.runGlobal(() -> {
            World world = worldAccessService.getWorld(worldId);
            if (world == null)
            {
                logger.log(Level.WARNING, "Could not " + action + " for unloaded world " + worldId);
                return;
            }

            Location schedulingLocation = world.getSpawnLocation();
            TaskHandle regionHandle = scheduler.runAtLocation(schedulingLocation, () -> {
                World currentWorld = worldAccessService.getWorld(worldId);
                if (currentWorld == null)
                {
                    logger.log(Level.WARNING, "Could not " + action + " for unloaded world " + worldId + " after scheduling");
                    return;
                }
                state.applyState(currentWorld);
            });
            track(regionHandle);
        });
        track(globalHandle);
    }

    private void applyStateImmediately(SleepWorldId worldId, WorldState state, String action)
    {
        World world = worldAccessService.getWorld(worldId);
        if (world == null)
        {
            logger.log(Level.WARNING, "Could not " + action + " for unloaded world " + worldId);
            return;
        }

        state.applyState(world);
    }

    private void track(TaskHandle taskHandle)
    {
        trackedTasks.add(taskHandle);
    }

    private void cancelTrackedTasks()
    {
        for (TaskHandle taskHandle : trackedTasks)
            if (!taskHandle.isCancelled())
                taskHandle.cancel();
        trackedTasks.clear();
    }

}

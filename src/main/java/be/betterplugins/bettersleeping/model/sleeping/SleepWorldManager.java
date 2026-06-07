package be.betterplugins.bettersleeping.model.sleeping;

import be.betterplugins.bettersleeping.listeners.AnimationHandler;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.model.SleepStatus;
import be.betterplugins.bettersleeping.model.permissions.BypassChecker;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import be.betterplugins.bettersleeping.services.sleeping.SleepWorldTicker;
import be.betterplugins.bettersleeping.services.world.WorldAccessService;
import be.betterplugins.core.messaging.logging.BPLogger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

@Singleton
public class SleepWorldManager
{

    private final Map<String, ManagedSleepWorld> sleepWorlds;
    private final AnimationHandler animationHandler;
    private final PluginScheduler scheduler;

    @Inject
    public SleepWorldManager(List<World> allWorlds, ConfigContainer config, BypassChecker bypassChecker, MessageDeliveryService messageDeliveryService, AnimationHandler animationHandler, PluginScheduler scheduler, WorldAccessService worldAccess, BPLogger logger)
    {
        YamlConfiguration sleepingSettings = config.getSleeping_settings();
        this.sleepWorlds = new HashMap<>();

        this.animationHandler = animationHandler;
        this.scheduler = scheduler;

        for (World world : allWorlds)
        {
            // Only allow sleeping in the overworld
            if (world.getEnvironment() != World.Environment.NORMAL && world.getEnvironment() != World.Environment.CUSTOM)
            {
                logger.log(Level.FINE, "Sleeping in world " + world.getName() + " will not be handled because it is not an overworld / custom world (but: " + world.getEnvironment() + ")");
                continue;
            }

            String isEnabledPath = "world_settings." + world.getName() + ".enabled";
            boolean isEnabled = ( !sleepingSettings.contains(isEnabledPath) ) || sleepingSettings.getBoolean(isEnabledPath);

            Boolean doDayLightRule = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            boolean doDayLightCycle = doDayLightRule == null || doDayLightRule;

            // Only enable if this world is enabled in the config AND time has been paused
            if (isEnabled && !doDayLightCycle)
            {
                logger.log(Level.CONFIG, "Enabling BetterSleeping in world " + world.getName());

                SleepWorld sleepWorld = new SleepWorld(world, config, bypassChecker, logger, worldAccess);
                SleepWorldTicker ticker = new SleepWorldTicker(config, sleepWorld, messageDeliveryService, logger);
                TaskHandle taskHandle = scheduler.repeatAtLocation(sleepWorld.getSchedulingLocation(), ticker::tick, 1L, 1L);

                this.sleepWorlds.put(sleepWorld.getWorldName(), new ManagedSleepWorld(ticker, taskHandle));
            }
            else
            {
                logger.log(Level.CONFIG, "NOT enabling BetterSleeping in world " + world.getName() + ". Enabled in config? " + isEnabled + ". DoDayLightCycle? " + doDayLightCycle);
            }
        }
    }


    /**
     * Get the SleepStatus of a specific world, if sleeping is enabled in that world
     *
     * @param world the world for which the status should be retrieved
     * @return null if sleeping is not enabled in this world, the relevant SleepStatus otherwise
     */
    public @Nullable SleepStatus getSleepStatus(World world)
    {
        return getSleepStatus(world.getName());
    }

    public @Nullable SleepStatus getSleepStatus(String worldName)
    {
        ManagedSleepWorld managedWorld = this.sleepWorlds.get(worldName);
        return managedWorld != null ? managedWorld.ticker.getSleepStatus() : null;
    }


    /**
     * Check whether this world is a BetterSleeping world
     *
     * @param world the world to be checked
     * @return true if BetterSleeping handles sleeping
     */
    public boolean isWorldEnabled(World world)
    {
        return isWorldEnabled(world.getName());
    }

    public boolean isWorldEnabled(String worldName)
    {
        return sleepWorlds.containsKey( worldName );
    }

    public boolean isDayTime(String worldName)
    {
        ManagedSleepWorld managedWorld = this.sleepWorlds.get(worldName);
        return managedWorld == null || managedWorld.ticker.isDayTime();
    }

    /**
     * Add a sleeper to the right sleep runnable
     *
     * @param player the player that should be marked as sleeping
     */
    public void addSleeper(Player player)
    {
        addSleeper(player, sleepStatus -> { });
    }

    public void addSleeper(Player player, Consumer<SleepStatus> afterAdd)
    {
        scheduler.runForEntity(player, () -> addSleeperInEntityContext(player, afterAdd));
    }

    private void addSleeperInEntityContext(Player player, Consumer<SleepStatus> afterAdd)
    {
        ManagedSleepWorld managedWorld = sleepWorlds.get( player.getWorld().getName() );
        if (managedWorld != null)
        {
            managedWorld.ticker.addSleeper(player);
            this.animationHandler.startSleepingAnimation( player );
            afterAdd.accept(managedWorld.ticker.getSleepStatus());
        }
    }


    /**
     * Remove a sleeper from the right sleep runnable
     *
     * @param player the player that should no longer be marked as sleeping
     */
    public void removeSleeper(Player player)
    {
        scheduler.runForEntity(player, () -> removeSleeperInEntityContext(player));
    }

    private void removeSleeperInEntityContext(Player player)
    {
        removeSleeper(player.getWorld().getName(), player.getUniqueId());
    }

    public void removeSleeper(String worldName, UUID playerId)
    {
        ManagedSleepWorld managedWorld = sleepWorlds.get(worldName);
        if (managedWorld != null)
            managedWorld.ticker.removeSleeper(playerId);
    }


    /**
     * Stop all sleeping runnables
     */
    public void stopRunnables()
    {
        for (ManagedSleepWorld managedWorld : this.sleepWorlds.values())
            if (!managedWorld.taskHandle.isCancelled())
                managedWorld.taskHandle.cancel();
        this.sleepWorlds.clear();
    }

    private static class ManagedSleepWorld
    {
        private final SleepWorldTicker ticker;
        private final TaskHandle taskHandle;

        private ManagedSleepWorld(SleepWorldTicker ticker, TaskHandle taskHandle)
        {
            this.ticker = ticker;
            this.taskHandle = taskHandle;
        }
    }
}

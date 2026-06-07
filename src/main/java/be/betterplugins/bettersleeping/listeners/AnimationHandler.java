package be.betterplugins.bettersleeping.listeners;

import be.betterplugins.bettersleeping.animation.ZZZAnimation;
import be.betterplugins.bettersleeping.animation.location.PlayerSleepLocation;
import be.betterplugins.bettersleeping.api.BecomeDayEvent;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import be.betterplugins.core.interfaces.IReloadable;
import be.betterplugins.core.messaging.logging.BPLogger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Singleton
public class AnimationHandler implements Listener, IReloadable
{

    private final boolean isEnabled;

    private final Map<UUID, TaskHandle> sleepingAnimations;
    private final PluginScheduler scheduler;

    private final BPLogger logger;

    @Inject
    public AnimationHandler(ConfigContainer configContainer, PluginScheduler scheduler, BPLogger logger)
    {
        this.scheduler = scheduler;
        this.logger = logger;

        this.isEnabled = configContainer.getConfig().getBoolean("enable_animations");

        this.logger.log(Level.FINE, "Are animations enabled? " + this.isEnabled);

        this.sleepingAnimations = new ConcurrentHashMap<>();
    }

    public void startSleepingAnimation(Player player)
    {
        if (!isEnabled) {
            this.logger.log(Level.FINEST,"Attempted to start a sleeping animation but they were disabled");
            return;
        }

        UUID playerId = player.getUniqueId();
        TaskHandle[] scheduledHandle = new TaskHandle[1];
        scheduledHandle[0] = scheduler.runForEntity(player, () -> startSleepingAnimationInEntityContext(player, scheduledHandle[0]));
        TaskHandle previous = this.sleepingAnimations.put(playerId, scheduledHandle[0]);
        if (previous != null && !previous.isCancelled())
            previous.cancel();
    }

    private void startSleepingAnimationInEntityContext(Player player, TaskHandle scheduledHandle)
    {
        UUID playerId = player.getUniqueId();
        if (this.sleepingAnimations.get(playerId) != scheduledHandle || !player.isOnline())
            return;

        this.logger.log(Level.FINEST, "Starting animation for player " + player.getName());

        ZZZAnimation animation = new ZZZAnimation(Particle.COMPOSTER, 0.5, 0.1, 200, scheduler);
        TaskHandle handle = animation.startAnimation(new PlayerSleepLocation(player));
        this.sleepingAnimations.put(playerId, handle);
    }

    @EventHandler
    public void timeSetToDayEvent(BecomeDayEvent event)
    {
        this.logger.log(Level.FINEST, "Stopping animations for all players");
        stopAllAnimations();
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event)
    {
        this.logger.log(Level.FINEST, "Stopping animation for player " + event.getPlayer().getName() + " due to waking up");
        UUID uuid =  event.getPlayer().getUniqueId();
        stopAnimation( uuid );
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event)
    {
        this.logger.log(Level.FINEST, "Stopping animation for player " + event.getPlayer().getName() + " due to leaving");
        UUID uuid = event.getPlayer().getUniqueId();
        stopAnimation(uuid);
    }

    private void stopAnimation(UUID uuid)
    {
        TaskHandle handle = this.sleepingAnimations.remove(uuid);
        if (handle != null && !handle.isCancelled())
            handle.cancel();
    }

    private void stopAllAnimations()
    {
        this.sleepingAnimations.forEach((uuid, handle) -> {
            if (!handle.isCancelled())
                handle.cancel();
        });
        this.sleepingAnimations.clear();
    }

    @Override
    public void reload()
    {
        stopAllAnimations();
    }
}

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Singleton
public class AnimationHandler implements Listener, IReloadable
{

    private final boolean isEnabled;

    private final Map<UUID, AnimationSession> sleepingAnimations;
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
        AnimationSession session = new AnimationSession();
        AnimationSession previous = this.sleepingAnimations.put(playerId, session);
        if (previous != null)
            previous.cancel();

        TaskHandle scheduledHandle = scheduler.runForEntity(player, () -> startSleepingAnimationInEntityContext(player, session));
        session.setScheduledHandle(scheduledHandle);
    }

    private void startSleepingAnimationInEntityContext(Player player, AnimationSession session)
    {
        UUID playerId = player.getUniqueId();
        if (this.sleepingAnimations.get(playerId) != session || session.isCancelled() || !player.isOnline())
            return;

        this.logger.log(Level.FINEST, "Starting animation for player " + player.getName());

        ZZZAnimation animation = new ZZZAnimation(Particle.COMPOSTER, 0.5, 0.1, 200, scheduler);
        TaskHandle handle = animation.startAnimation(new PlayerSleepLocation(player));
        session.setAnimationHandle(handle);
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
        AnimationSession session = this.sleepingAnimations.remove(uuid);
        if (session != null)
            session.cancel();
    }

    private void stopAllAnimations()
    {
        this.sleepingAnimations.forEach((uuid, session) -> session.cancel());
        this.sleepingAnimations.clear();
    }

    @Override
    public void reload()
    {
        stopAllAnimations();
    }

    private static final class AnimationSession
    {
        private final AtomicReference<TaskHandle> scheduledHandle = new AtomicReference<>();
        private final AtomicReference<TaskHandle> animationHandle = new AtomicReference<>();
        private volatile boolean cancelled;

        void setScheduledHandle(TaskHandle handle)
        {
            this.scheduledHandle.set(handle);
            if (cancelled)
                cancelHandle(handle);
        }

        void setAnimationHandle(TaskHandle handle)
        {
            this.animationHandle.set(handle);
            if (cancelled)
                cancelHandle(handle);
        }

        boolean isCancelled()
        {
            return cancelled;
        }

        void cancel()
        {
            this.cancelled = true;
            cancelHandle(scheduledHandle.get());
            cancelHandle(animationHandle.get());
        }

        private static void cancelHandle(TaskHandle handle)
        {
            if (handle != null && !handle.isCancelled())
                handle.cancel();
        }
    }
}

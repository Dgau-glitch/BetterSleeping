package be.betterplugins.bettersleeping.hooks;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldManager;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.util.TimeUtil;
import com.google.inject.Inject;
import dev.geco.gsit.api.event.PlayerGetUpPoseEvent;
import dev.geco.gsit.api.event.PlayerPoseEvent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class GSitListener implements Listener {

    private final SleepWorldManager sleepWorldManager;
    private final PluginScheduler scheduler;

    @Inject
    public GSitListener(SleepWorldManager sleepWorldManager, PluginScheduler scheduler)
    {
        this.sleepWorldManager = sleepWorldManager;
        this.scheduler = scheduler;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerLay(PlayerPoseEvent layOrCrawlEvent)
    {
        Player player = layOrCrawlEvent.getPlayer();
        scheduler.runForEntity(player, () -> handlePlayerLay(player, layOrCrawlEvent.getPoseSeat().getPose()));
    }

    private void handlePlayerLay(Player player, Pose pose)
    {
        World world = player.getWorld();

        // If time for sleeping has not come yet, don't count the event
        if (!TimeUtil.isSleepPossible(world))
        {
            return;
        }

        // Don't handle disabled worlds
        if (!sleepWorldManager.isWorldEnabled(world))
        {
            return;
        }

        if (pose == Pose.SLEEPING)
        {
            sleepWorldManager.addSleeper(player);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerGetUp(PlayerGetUpPoseEvent playerGetUpFromCrawlOrLayEvent)
    {
        Player player = playerGetUpFromCrawlOrLayEvent.getPlayer();
        scheduler.runForEntity(player, () -> handlePlayerGetUp(player));
    }

    private void handlePlayerGetUp(Player player)
    {
        // Don't handle disabled worlds
        if (!sleepWorldManager.isWorldEnabled(player.getWorld()))
        {
            return;
        }

        this.sleepWorldManager.removeSleeper(player);
    }
}

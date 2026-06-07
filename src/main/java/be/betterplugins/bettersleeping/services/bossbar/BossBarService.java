package be.betterplugins.bettersleeping.services.bossbar;

import be.betterplugins.bettersleeping.model.SleepStatus;
import be.betterplugins.bettersleeping.model.sleeping.SleepWorldManager;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import be.betterplugins.core.messaging.messenger.Messenger;
import be.betterplugins.core.messaging.messenger.MsgEntry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class BossBarService implements Listener
{
    private final JavaPlugin plugin;
    private final SleepWorldManager sleepWorldManager;
    private final Messenger messenger;
    private final PluginScheduler scheduler;
    private final List<Player> initialOnlinePlayers;
    private final Map<String, BossBar> bossBars;
    private final Map<UUID, String> playerWorlds;

    private TaskHandle taskHandle;

    @Inject
    public BossBarService(JavaPlugin plugin, SleepWorldManager sleepWorldManager, Messenger messenger, PluginScheduler scheduler, @Named("online_players") List<Player> initialOnlinePlayers)
    {
        this.plugin = plugin;
        this.sleepWorldManager = sleepWorldManager;
        this.messenger = messenger;
        this.scheduler = scheduler;
        this.initialOnlinePlayers = initialOnlinePlayers;
        this.bossBars = new ConcurrentHashMap<>();
        this.playerWorlds = new ConcurrentHashMap<>();
    }

    public void start(long initialDelayTicks, long periodTicks)
    {
        for (Player player : initialOnlinePlayers)
            trackPlayer(player);

        this.taskHandle = scheduler.repeatGlobal(this::updateBossBars, initialDelayTicks, periodTicks);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        trackPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        untrackPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
        untrackFromWorld(event.getPlayer(), event.getFrom().getName());
        trackPlayer(event.getPlayer());
    }

    private void trackPlayer(Player player)
    {
        scheduler.runForEntity(player, () -> {
            String worldName = player.getWorld().getName();
            SleepStatus sleepStatus = sleepWorldManager.getSleepStatus(worldName);
            if (sleepStatus == null)
            {
                playerWorlds.remove(player.getUniqueId());
                return;
            }

            BossBar bossBar = bossBars.computeIfAbsent(worldName, key -> createBossBar());
            updateBossBar(bossBar, sleepStatus);
            bossBar.addPlayer(player);
            playerWorlds.put(player.getUniqueId(), worldName);
        });
    }

    private void untrackPlayer(Player player)
    {
        String worldName = playerWorlds.remove(player.getUniqueId());
        if (worldName != null)
            scheduler.runForEntity(player, () -> removeFromBossBar(player, worldName));
    }

    private void untrackFromWorld(Player player, String worldName)
    {
        playerWorlds.remove(player.getUniqueId(), worldName);
        scheduler.runForEntity(player, () -> removeFromBossBar(player, worldName));
    }

    private void removeFromBossBar(Player player, String worldName)
    {
        BossBar bossBar = bossBars.get(worldName);
        if (bossBar != null)
            bossBar.removePlayer(player);
    }

    private BossBar createBossBar()
    {
        BossBar bossBar = plugin.getServer().createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_12);
        bossBar.setVisible(false);
        return bossBar;
    }

    private void updateBossBars()
    {
        for (Map.Entry<String, BossBar> entry : bossBars.entrySet())
        {
            String worldName = entry.getKey();
            BossBar bossBar = entry.getValue();
            SleepStatus sleepStatus = sleepWorldManager.getSleepStatus(worldName);
            updateBossBar(bossBar, sleepStatus);
            bossBar.setVisible(isVisible(worldName, sleepStatus));
        }
    }

    private boolean isVisible(String worldName, @Nullable SleepStatus sleepStatus)
    {
        return sleepStatus != null && sleepStatus.getNumSleepers() > 0 && !sleepWorldManager.isDayTime(worldName);
    }

    private void updateBossBar(@NotNull BossBar bossBar, @Nullable SleepStatus sleepStatus)
    {
        if (sleepStatus == null)
            return;

        final double percentage = (double) sleepStatus.getNumSleepers() / (double) sleepStatus.getNumNeeded();
        double progress = Math.max(0, Math.min(1, percentage));
        bossBar.setProgress(progress);
        bossBar.setTitle(
                messenger.composeMessage(
                        "bossbar_title",
                        true,
                        new MsgEntry("<num_sleeping>", sleepStatus.getNumSleepers()),
                        new MsgEntry("<needed_sleeping>", sleepStatus.getNumNeeded()),
                        new MsgEntry("<remaining_sleeping>", sleepStatus.getNumMissing())
                )
        );
    }

    /**
     * Stop all BossBars from updating and remove all current visualisations.
     */
    public void stopBossBars()
    {
        if (this.taskHandle != null && !this.taskHandle.isCancelled())
            this.taskHandle.cancel();

        for (BossBar bossBar : bossBars.values())
            bossBar.removeAll();

        bossBars.clear();
        playerWorlds.clear();
    }
}

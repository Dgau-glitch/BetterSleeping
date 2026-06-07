package be.betterplugins.bettersleeping.hooks;

import be.betterplugins.bettersleeping.BetterSleeping;
import be.betterplugins.bettersleeping.listeners.BuffsHandler;
import be.betterplugins.bettersleeping.model.SleepStatus;
import be.betterplugins.bettersleeping.model.sleeping.SleepWorldManager;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.core.messaging.logging.BPLogger;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PapiExpansion extends PlaceholderExpansion implements Listener
{

    private final BetterSleeping plugin;
    private final SleepWorldManager sleepWorldManager;
    private final BuffsHandler buffsHandler;
    private final PluginScheduler scheduler;
    private final Map<UUID, String> playerWorlds;

    /**
     * @param plugin instance of BetterSleeping
     */
    @Inject
    public PapiExpansion(BetterSleeping plugin, SleepWorldManager sleepWorldManager, BuffsHandler buffsHandler, PluginScheduler scheduler, @Named("online_players") List<Player> onlinePlayers, BPLogger logger)
    {
        this.plugin = plugin;
        this.sleepWorldManager = sleepWorldManager;
        this.buffsHandler = buffsHandler;
        this.scheduler = scheduler;
        this.playerWorlds = new ConcurrentHashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : onlinePlayers)
            trackPlayer(player);

        logger.log(Level.CONFIG, "PlaceholderAPI hook enabled: placeholders use cached Folia-safe world/sleep snapshots and avoid live cross-region world scans.");
    }


    /**
     * Makes sure this expansion is supported by PAPI
     * @return true to persist through reloads
     */
    @Override
    public boolean persist()
    {
        return true;
    }


    /**
     * @return Always true since it's an internal class
     */
    @Override
    public boolean canRegister()
    {
        return true;
    }


    @Override
    public String getAuthor()
    {
        return plugin.getDescription().getAuthors().toString();
    }


    /**
     * The unique ID for BetterSleeping placeholders
     * @return always "bettersleeping"
     */
    @Override
    public String getIdentifier()
    {
        return "bettersleeping";
    }

    /**
     * @return the plugin version as a String
     */
    @Override
    public String getVersion()
    {
        return plugin.getDescription().getVersion();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        trackPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
        trackPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        playerWorlds.remove(event.getPlayer().getUniqueId());
    }

    private void trackPlayer(Player player)
    {
        scheduler.runForEntity(player, () -> playerWorlds.put(player.getUniqueId(), player.getWorld().getName()));
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * @param  player The involved player
     * @param  identifier The requested replacement
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier)
    {

        // Only allow calculating values if a Player object is provided
        if (player == null)
            return null;

        String worldName = playerWorlds.get(player.getUniqueId());
        if (worldName == null)
        {
            trackPlayer(player);
            return null;
        }

        // Get the latest cached status for this world without touching live World/Player region objects.
        SleepStatus sleepStatus = sleepWorldManager.getCachedSleepStatus(worldName);

        // BetterSleeping is disabled in this world so returning null to indicate a faulty identifier
        if (sleepStatus == null)
            return null;

        // Make sure the identifier matches, regardless of capitalization used
        identifier = identifier.toLowerCase();
        switch (identifier)
        {
            case "bs_num_sleeping":
                return "" + sleepStatus.getNumSleepers();
            case "bs_total_needed":
                return "" + sleepStatus.getNumNeeded();
            case "bs_extra_needed":
                return "" + Math.max(sleepStatus.getNumNeeded() - sleepStatus.getNumSleepers(),0);
            case "bs_num_in_world":
                return "" + sleepStatus.getNumPlayersInWorld();
            case "bs_buffs_amount":
                return "" + buffsHandler.getBuffs().size();
            case "bs_debuffs_amount":
                return "" + buffsHandler.getDebuffs().size();
            case "bs_dayspeed":
                return "" + sleepStatus.getDaySpeedup();
            case "bs_nightspeed":
                return "" + sleepStatus.getNightSpeedup();
            case "bs_sleepspeed":
                return "" + sleepStatus.getSleepSpeedup();
        }

        // PAPI documentation suggests returning null for faulty identifiers
        return null;
    }
}

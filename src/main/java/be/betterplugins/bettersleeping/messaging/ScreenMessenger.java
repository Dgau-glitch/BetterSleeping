package be.betterplugins.bettersleeping.messaging;

import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.core.messaging.logging.BPLogger;
import be.betterplugins.core.messaging.messenger.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ScreenMessenger extends Messenger
{

    private final PluginScheduler scheduler;
    private final Map<UUID, ScreenMessageSender> playerMessengerMap = new ConcurrentHashMap<>();

    public ScreenMessenger(JavaPlugin plugin, PluginScheduler scheduler, Map<String, String> messages, String prefix, BPLogger logger)
    {
        super(messages, logger, prefix);
        this.scheduler = scheduler;

        // Register creation and deletion of messengers for every player
        plugin.getServer().getPluginManager().registerEvents(new PlayerQueueEventListener(), plugin);
    }

    @Override
    protected void sendMessage(CommandSender receiver, String message)
    {
        if (receiver instanceof Player)
        {
            Player player = (Player) receiver;
            ScreenMessageSender sender = playerMessengerMap.computeIfAbsent(
                    player.getUniqueId(),
                    uuid -> new ScreenMessageSender(scheduler)
            );
            sender.sendMessage(player, message);
        }
        else
        {
            // Fallback
            receiver.sendMessage(message);
        }
    }

    /**
     * Event listener that manages queue of all players and their corresponding
     * messengers
     */
    private class PlayerQueueEventListener implements Listener
    {

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            // Create messenger for every player joining
            playerMessengerMap.put(event.getPlayer().getUniqueId(), new ScreenMessageSender(scheduler));
        }

        @EventHandler
        public void onPlayerLeave(PlayerQuitEvent event)
        {
            // Remove messenger for every player leaving
            playerMessengerMap.remove(event.getPlayer().getUniqueId());
        }
    }

}

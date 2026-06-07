package be.betterplugins.bettersleeping.messaging;

import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScreenMessageSender
{

    private static final long MESSAGE_DELAY = 3L * 20L;

    private final PluginScheduler scheduler;
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();

    public ScreenMessageSender(PluginScheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    /**
     * Send a message on-screen to a player. The actual player interaction is
     * scheduled on the player's entity scheduler.
     *
     * @param player the receiving player
     * @param message the raw message
     */
    public void sendMessage(Player player, String message)
    {
        scheduler.runForEntity(player, () -> enqueueAndSend(player, message));
    }

    private void enqueueAndSend(Player player, String message)
    {
        synchronized (messageQueue)
        {
            messageQueue.add(message);
            if (messageQueue.size() == 1)
            {
                sendQueuedMessage(player);
            }
        }
    }

    private void sendQueuedMessage(Player player)
    {
        String message = messageQueue.peek();
        if (message == null)
            return;

        Player.Spigot p = player.spigot();
        BaseComponent bc = new TextComponent();
        bc.addExtra(message);
        ChatMessageType type = ChatMessageType.ACTION_BAR;
        p.sendMessage(type, bc);

        scheduler.runForEntityLater(player, () -> {
            synchronized (messageQueue)
            {
                messageQueue.remove();
                if (!messageQueue.isEmpty())
                {
                    sendQueuedMessage(player);
                }
            }
        }, MESSAGE_DELAY);
    }

}

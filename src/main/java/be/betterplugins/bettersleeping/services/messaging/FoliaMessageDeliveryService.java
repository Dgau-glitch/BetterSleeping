package be.betterplugins.bettersleeping.services.messaging;

import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.core.messaging.messenger.Messenger;
import be.betterplugins.core.messaging.messenger.MsgEntry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Singleton
public class FoliaMessageDeliveryService implements MessageDeliveryService
{
    private final Messenger messenger;
    private final PluginScheduler scheduler;
    private final Server server;

    @Inject
    public FoliaMessageDeliveryService(Messenger messenger, PluginScheduler scheduler, JavaPlugin plugin)
    {
        this(messenger, scheduler, plugin.getServer());
    }

    FoliaMessageDeliveryService(Messenger messenger, PluginScheduler scheduler, Server server)
    {
        this.messenger = messenger;
        this.scheduler = scheduler;
        this.server = server;
    }

    @Override
    public void send(CommandSender receiver, String messageKey, MsgEntry... entries)
    {
        if (receiver instanceof Player)
        {
            Player player = (Player) receiver;
            scheduler.runForEntity(player, () -> messenger.sendMessage(player, messageKey, entries));
        }
        else
        {
            scheduler.runGlobal(() -> messenger.sendMessage(receiver, messageKey, entries));
        }
    }

    @Override
    public void send(UUID receiverId, String messageKey, MsgEntry... entries)
    {
        scheduler.runGlobal(() -> {
            Player player = server.getPlayer(receiverId);
            if (player != null && player.isOnline())
                send(player, messageKey, entries);
        });
    }

    @Override
    public void send(Collection<Player> receivers, String messageKey, MsgEntry... entries)
    {
        List<Player> snapshot = new ArrayList<>(receivers);
        for (Player receiver : snapshot)
            send(receiver, messageKey, entries);
    }
}

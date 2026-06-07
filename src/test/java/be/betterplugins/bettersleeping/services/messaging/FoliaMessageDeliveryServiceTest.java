package be.betterplugins.bettersleeping.services.messaging;

import be.betterplugins.core.messaging.messenger.Messenger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Test;
import testutil.FakePluginScheduler;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FoliaMessageDeliveryServiceTest
{
    @Test
    public void sendByUuidResolvesPlayerAndDeliversThroughEntityScheduler()
    {
        Messenger messenger = mock(Messenger.class);
        FakePluginScheduler scheduler = new FakePluginScheduler();
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getPlayer(playerId)).thenReturn(player);
        when(player.isOnline()).thenReturn(true);

        FoliaMessageDeliveryService service = new FoliaMessageDeliveryService(messenger, scheduler, plugin);

        service.send(playerId, "sleep_status");

        verify(messenger).sendMessage(player, "sleep_status");
    }
}

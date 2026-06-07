package be.betterplugins.bettersleeping.services.messaging;

import be.betterplugins.core.messaging.messenger.Messenger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
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
        Server server = mock(Server.class);
        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);

        when(server.getPlayer(playerId)).thenReturn(player);
        when(player.isOnline()).thenReturn(true);

        FoliaMessageDeliveryService service = new FoliaMessageDeliveryService(messenger, scheduler, server);

        service.send(playerId, "sleep_status");

        verify(messenger).sendMessage(player, "sleep_status");
    }
}

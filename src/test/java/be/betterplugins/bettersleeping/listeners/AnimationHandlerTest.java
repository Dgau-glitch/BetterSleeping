package be.betterplugins.bettersleeping.listeners;

import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.core.messaging.logging.BPLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;
import testutil.FakePluginScheduler;

import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnimationHandlerTest
{
    @Test
    public void reloadCancelsTrackedAnimationTask()
    {
        ConfigContainer configContainer = mock(ConfigContainer.class);
        YamlConfiguration config = mock(YamlConfiguration.class);
        when(configContainer.getConfig()).thenReturn(config);
        when(config.getBoolean("enable_animations")).thenReturn(true);

        FakePluginScheduler scheduler = new FakePluginScheduler(false);
        BPLogger logger = mock(BPLogger.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Sleeper");

        AnimationHandler handler = new AnimationHandler(configContainer, scheduler, logger);
        handler.startSleepingAnimation(player);
        FakePluginScheduler.FakeTaskHandle animationHandle = scheduler.getLastHandle();

        handler.reload();

        assertTrue(animationHandle.isCancelled());
    }
}

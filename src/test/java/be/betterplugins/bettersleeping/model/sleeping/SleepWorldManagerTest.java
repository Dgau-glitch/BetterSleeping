package be.betterplugins.bettersleeping.model.sleeping;

import be.betterplugins.bettersleeping.listeners.AnimationHandler;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.model.permissions.BypassChecker;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.services.world.WorldAccessService;
import be.betterplugins.core.messaging.logging.BPLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;
import testutil.FakePluginScheduler;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SleepWorldManagerTest
{
    @Test
    public void schedulesWorldTickerOnGlobalRegionBecauseWorldTimeIsGlobal()
    {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getTime()).thenReturn(0L);
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, 64, 0));

        YamlConfiguration sleepingSettings = mock(YamlConfiguration.class);
        when(sleepingSettings.getString("sleeper_calculator")).thenReturn("percentage");
        when(sleepingSettings.getInt("needed")).thenReturn(50);
        when(sleepingSettings.getDouble("day_length")).thenReturn(700D);
        when(sleepingSettings.getDouble("night_length")).thenReturn(500D);
        when(sleepingSettings.getDouble("night_skip_length")).thenReturn(10D);

        ConfigContainer config = mock(ConfigContainer.class);
        when(config.getSleeping_settings()).thenReturn(sleepingSettings);

        FakePluginScheduler scheduler = new FakePluginScheduler(false);

        new SleepWorldManager(
                List.of(world),
                config,
                mock(BypassChecker.class),
                mock(MessageDeliveryService.class),
                mock(AnimationHandler.class),
                scheduler,
                new TestWorldAccessService(world),
                mock(BPLogger.class)
        );

        assertEquals(1, scheduler.getRepeatGlobalCount());
        assertEquals(0, scheduler.getRepeatAtLocationCount());
    }

    private static final class TestWorldAccessService implements WorldAccessService
    {
        private final World world;
        private final SleepWorldId id;

        private TestWorldAccessService(World world)
        {
            this.world = world;
            this.id = SleepWorldId.fromWorld(world);
        }

        @Override
        public SleepWorldId getId(World world)
        {
            return id;
        }

        @Override
        public World getWorld(SleepWorldId worldId)
        {
            return id.equals(worldId) ? world : null;
        }

        @Override
        public Location getSchedulingLocation(SleepWorldId worldId)
        {
            return world.getSpawnLocation();
        }

        @Override
        public List<Player> getAllPlayersInWorld(SleepWorldId worldId)
        {
            return Collections.emptyList();
        }

        @Override
        public long getTime(SleepWorldId worldId)
        {
            return world.getTime();
        }

        @Override
        public void setTime(SleepWorldId worldId, long time)
        {
            world.setTime(time);
        }

        @Override
        public boolean isClearWeather(SleepWorldId worldId)
        {
            return world.isClearWeather();
        }

        @Override
        public void clearWeather(SleepWorldId worldId)
        {
            world.setStorm(false);
            world.setThundering(false);
        }

        @Override
        public boolean isInWorld(SleepWorldId worldId, Player player)
        {
            return id.equals(worldId) && player.getWorld().equals(world);
        }
    }
}

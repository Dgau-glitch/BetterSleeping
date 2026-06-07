package model.sleeping;

import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.model.sleeping.SleepWorld;
import be.betterplugins.bettersleeping.model.sleeping.SleepWorldId;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.services.sleeping.SleepWorldTicker;
import be.betterplugins.bettersleeping.services.world.WorldAccessService;
import be.betterplugins.bettersleeping.model.permissions.BypassChecker;
import be.betterplugins.core.messaging.logging.BPLogger;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class SleepWorldTest
{

    public ConfigContainer mockConfigContainer()
    {
        YamlConfiguration config = mock(YamlConfiguration.class);
        when(config.getString("sleeper_counter")).thenReturn("percentage");

        ConfigContainer container = mock(ConfigContainer.class);
        when(container.getSleeping_settings()).thenReturn(config);

        return container;
    }

    public void mockGetEnvironment(Player player)
    {
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(player.getWorld()).thenReturn(world);
    }

    public SleepWorld createSleepWorld(World world, ConfigContainer config, BypassChecker bypassChecker)
    {
        return new SleepWorld(world, config, bypassChecker, mock(BPLogger.class), new TestWorldAccessService(world));
    }


    private static class NoOpMessageDeliveryService implements MessageDeliveryService
    {
        @Override
        public void send(CommandSender receiver, String messageKey, be.betterplugins.core.messaging.messenger.MsgEntry... entries)
        {
        }

        @Override
        public void send(UUID receiverId, String messageKey, be.betterplugins.core.messaging.messenger.MsgEntry... entries)
        {
        }

        @Override
        public void send(Collection<Player> receivers, String messageKey, be.betterplugins.core.messaging.messenger.MsgEntry... entries)
        {
        }
    }

    private static class TestWorldAccessService implements WorldAccessService
    {
        private final World world;
        private final SleepWorldId worldId;

        private TestWorldAccessService(World world)
        {
            this.world = world;
            this.worldId = SleepWorldId.fromWorld(world);
        }

        @Override
        public SleepWorldId getId(World world)
        {
            return worldId;
        }

        @Override
        public World getWorld(SleepWorldId worldId)
        {
            return world;
        }

        @Override
        public Location getSchedulingLocation(SleepWorldId worldId)
        {
            return null;
        }

        @Override
        public List<Player> getAllPlayersInWorld(SleepWorldId worldId)
        {
            return world.getPlayers();
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
            return player.getWorld().equals(world);
        }
    }

    public World mockTimedWorld(long initialTime)
    {
        World world = mock(World.class);
        long[] time = {initialTime};
        when(world.getTime()).thenAnswer(invocation -> time[0]);
        doAnswer(invocation -> {
            time[0] = invocation.getArgument(0);
            return null;
        }).when(world).setTime(anyLong());
        return world;
    }

    @Test
    public void testGetAllPlayersInWorld()
    {
        List<Player> mockPlayerList = new ArrayList<>();
        Player p1 = mock( Player.class );
        Player p2 = mock( Player.class );
        Player p3 = mock( Player.class );

        mockGetEnvironment(p1);
        mockGetEnvironment(p2);
        mockGetEnvironment(p3);

        mockPlayerList.add(p1);
        mockPlayerList.add(p2);
        mockPlayerList.add(p3);

        World world = mock(World.class);
        when(world.getPlayers()).thenReturn( mockPlayerList );

        SleepWorld sleepWorld = createSleepWorld(world, mockConfigContainer(), mock(BypassChecker.class));
        assert sleepWorld.getAllPlayersInWorld().equals( mockPlayerList );
    }

    @Test
    public void testGetValidPlayersInWorld()
    {
        List<Player> mockPlayerList = new ArrayList<>();
        Player p1 = mock( Player.class );
        Player p2 = mock( Player.class );
        Player p3 = mock( Player.class );

        mockGetEnvironment(p1);
        mockGetEnvironment(p2);
        mockGetEnvironment(p3);

        mockPlayerList.add(p1);
        mockPlayerList.add(p2);
        mockPlayerList.add(p3);

        BypassChecker checker = mock(BypassChecker.class);
        when(checker.isPlayerBypassed(p1)).thenReturn(false);
        when(checker.isPlayerBypassed(p2)).thenReturn(true);
        when(checker.isPlayerBypassed(p3)).thenReturn(false);

        World world = mock(World.class);
        when(world.getPlayers()).thenReturn( mockPlayerList );

        SleepWorld sleepWorld = createSleepWorld(world, mockConfigContainer(), checker);

        List<Player> expectedList = new ArrayList<>();
        expectedList.add(p1);
        expectedList.add(p3);

        assert sleepWorld.getValidPlayersInWorld().equals( expectedList );
    }

    @Test
    public void testRemoveSleeperClearsSleeperCounter()
    {
        UUID worldId = UUID.randomUUID();
        World world = mockTimedWorld(13000);
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Sleeper");
        when(player.getWorld()).thenReturn(world);
        when(player.isSleeping()).thenReturn(false);
        when(player.isOnline()).thenReturn(true);

        when(world.getPlayers()).thenReturn(new ArrayList<Player>() {{ add(player); }});

        SleepWorld sleepWorld = createSleepWorld(world, mockConfigContainer(), mock(BypassChecker.class));
        SleepWorldTicker ticker = new SleepWorldTicker(mockConfigContainer(), sleepWorld, new NoOpMessageDeliveryService(), mock(BPLogger.class));

        ticker.addSleeper(player);
        assertEquals(1, ticker.getSleepStatus().getNumSleepers());

        ticker.removeSleeper(player);
        assertEquals(0, ticker.getSleepStatus().getNumSleepers());
    }

    @Test
    public void testTimeChanging()
    {
        World world = mockTimedWorld(100);

        SleepWorld sleepWorld = createSleepWorld(world, mockConfigContainer(), mock(BypassChecker.class));

        sleepWorld.addTime(56.3);
        assert world.getTime() == 156;
        assert sleepWorld.getInternalTime() == 156.3;

        sleepWorld.addTime(0.6);
        assert world.getTime() == 156;
        assert sleepWorld.getInternalTime() == 156.9;

        sleepWorld.addTime(0.1);
        assert world.getTime() == 157;
        assert sleepWorld.getInternalTime() == 157;

        sleepWorld.setTime(23999);
        assert world.getTime() == 23999;
        assert sleepWorld.getInternalTime() == 23999;

        sleepWorld.addTime(1);
        assert world.getTime() == 0;
        assert sleepWorld.getInternalTime() == 0;
    }

    @Test
    public void testTimePassedDetection()
    {
        World world = mockTimedWorld(23500);

        SleepWorld sleepWorld = createSleepWorld(world, mockConfigContainer(), mock(BypassChecker.class));

        sleepWorld.addTime(500);
        assert sleepWorld.didTimeBecomeDay( 23500 );
        assert sleepWorld.calcPassedTime( 23500 ) == 500;

        sleepWorld.setTime(23999);
        assert !sleepWorld.didTimeBecomeDay(23998);
        assert sleepWorld.calcPassedTime(23500) == 499;

        sleepWorld.addTime(1);
        assert sleepWorld.didTimeBecomeDay(23999);
        assert sleepWorld.calcPassedTime(23999) == 1;
    }
}

package be.betterplugins.bettersleeping.model.world;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldId;
import be.betterplugins.bettersleeping.services.world.WorldAccessService;
import be.betterplugins.core.messaging.logging.BPLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Test;
import testutil.FakePluginScheduler;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldStateHandlerTest
{
    @Test
    public void revertWorldStatesDoesNotScheduleTasksDuringDisable()
    {
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, 64, 0));

        FakePluginScheduler scheduler = new FakePluginScheduler();
        TestWorldAccessService worldAccessService = new TestWorldAccessService(world);
        WorldState originalState = mock(WorldState.class);
        WorldState temporaryState = mock(WorldState.class);
        WorldStateHandler handler = new WorldStateHandler(
                Map.of(worldAccessService.id, originalState),
                scheduler,
                worldAccessService,
                mock(BPLogger.class)
        );
        handler.setWorldStates(temporaryState);
        int scheduledTasksBeforeDisable = scheduler.getHandles().size();

        handler.revertWorldStates();

        assertEquals(scheduledTasksBeforeDisable, scheduler.getHandles().size());
        verify(originalState).applyState(world);
        verify(temporaryState).applyState(world);
        verify(temporaryState, never()).applyState(null);
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
            return false;
        }
    }
}

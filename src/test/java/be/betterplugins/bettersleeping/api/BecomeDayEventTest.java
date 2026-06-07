package be.betterplugins.bettersleeping.api;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BecomeDayEventTest
{
    @Test
    public void snapshotExposesWorldCauseAndPlayerIdsWithoutLivePlayerLists()
    {
        World world = mock(World.class);
        World playerWorld = mock(World.class);
        Player sleeper = mock(Player.class);
        Player nonSleeper = mock(Player.class);
        UUID sleeperId = UUID.randomUUID();
        UUID nonSleeperId = UUID.randomUUID();

        when(world.getName()).thenReturn("world");
        when(playerWorld.getName()).thenReturn("world");
        when(sleeper.hasMetadata("NPC")).thenReturn(false);
        when(sleeper.isOnline()).thenReturn(true);
        when(sleeper.getUniqueId()).thenReturn(sleeperId);
        when(sleeper.getName()).thenReturn("Sleeper");
        when(sleeper.getWorld()).thenReturn(playerWorld);
        when(nonSleeper.hasMetadata("NPC")).thenReturn(false);
        when(nonSleeper.isOnline()).thenReturn(true);
        when(nonSleeper.getUniqueId()).thenReturn(nonSleeperId);
        when(nonSleeper.getName()).thenReturn("Awake");
        when(nonSleeper.getWorld()).thenReturn(playerWorld);

        BecomeDayEvent event = new BecomeDayEvent(
                world,
                BecomeDayEvent.Cause.SLEEPING,
                Collections.singletonList(sleeper),
                Collections.singletonList(nonSleeper)
        );

        BecomeDayEvent.EventSnapshot snapshot = event.getSnapshot();

        assertEquals("world", snapshot.getWorldName());
        assertEquals(BecomeDayEvent.Cause.SLEEPING, snapshot.getCause());
        assertEquals(Collections.singletonList(sleeperId), snapshot.getSleptPlayerIds());
        assertEquals(Collections.singletonList("Sleeper"), snapshot.getSleptPlayerNames());
        assertEquals(Collections.singletonList(nonSleeperId), snapshot.getNonSleptPlayerIds());
        assertEquals(Collections.singletonList("Awake"), snapshot.getNonSleptPlayerNames());
    }
}

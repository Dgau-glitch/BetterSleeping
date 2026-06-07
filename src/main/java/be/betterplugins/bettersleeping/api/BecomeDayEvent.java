package be.betterplugins.bettersleeping.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BecomeDayEvent extends Event
{

    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;              // The world in which the time was set to day
    private final String worldName;
    private final Cause cause;              // The cause of time being set to day
    private final List<PlayerSnapshot> sleepers;
    private final List<PlayerSnapshot> nonSleepers;
    private final EventSnapshot snapshot;


    public enum Cause
    {
        NATURAL,
        SLEEPING,
        OTHER
    }


    public BecomeDayEvent(World world, Cause cause, List<Player> sleepers, List<Player> nonSleepers)
    {
        super();

        this.world = world;
        this.worldName = world.getName();
        this.cause = cause;
        this.sleepers = toSnapshots(sleepers, true);
        this.nonSleepers = toSnapshots(nonSleepers, false);
        this.snapshot = new EventSnapshot(worldName, cause, this.sleepers, this.nonSleepers);
    }

    private List<PlayerSnapshot> toSnapshots(List<Player> players, boolean slept)
    {
        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (Player player : players)
        {
            if (!player.hasMetadata("NPC") && player.isOnline())
                snapshots.add(PlayerSnapshot.fromPlayer(player, slept));
        }
        return Collections.unmodifiableList(snapshots);
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }


    /**
     * Get the world in which the time was set to day.
     *
     * @return the world
     * @deprecated Prefer {@link #getWorldName()} or {@link #getSnapshot()} in Folia handlers; world mutations must be scheduled by region.
     */
    @Deprecated
    public World getWorld()
    {
        return world;
    }

    public String getWorldName()
    {
        return worldName;
    }


    /**
     * Get the cause of time being set to day
     * @return the cause
     */
    public Cause getCause()
    {
        return cause;
    }

    public List<PlayerSnapshot> getPlayersWhoSleptSnapshots()
    {
        return sleepers;
    }

    public List<PlayerSnapshot> getPlayersWhoDidNotSleepSnapshots()
    {
        return nonSleepers;
    }

    public List<PlayerSnapshot> getPlayerSnapshots()
    {
        List<PlayerSnapshot> snapshots = new ArrayList<>(sleepers);
        snapshots.addAll(nonSleepers);
        return Collections.unmodifiableList(snapshots);
    }

    /**
     * Get a region-safe immutable event DTO for external Folia consumers.
     * Mutations based on this data must be scheduled through Folia entity/world schedulers by the consumer.
     *
     * @return immutable snapshot of this event
     */
    public EventSnapshot getSnapshot()
    {
        return snapshot;
    }


    /**
     * Get a List of players that slept this night
     * @return the list of Players
     * @deprecated Prefer {@link #getPlayersWhoSleptSnapshots()} so handlers do not retain live Player objects.
     */
    @Deprecated
    public List<Player> getPlayersWhoSlept()
    {
        return toOnlinePlayers(sleepers);
    }


    /**
     * Get the List of players that did not sleep last night
     * @return the list of players
     * @deprecated Prefer {@link #getPlayersWhoDidNotSleepSnapshots()} so handlers do not retain live Player objects.
     */
    @Deprecated
    public List<Player> getPlayersWhoDidNotSleep()
    {
        return toOnlinePlayers(nonSleepers);
    }

    private List<Player> toOnlinePlayers(List<PlayerSnapshot> snapshots)
    {
        return snapshots.stream()
                .map(snapshot -> Bukkit.getPlayer(snapshot.getPlayerId()))
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toList());
    }


    public static final class EventSnapshot
    {
        private final String worldName;
        private final Cause cause;
        private final List<PlayerSnapshot> sleepers;
        private final List<PlayerSnapshot> nonSleepers;

        private EventSnapshot(String worldName, Cause cause, List<PlayerSnapshot> sleepers, List<PlayerSnapshot> nonSleepers)
        {
            this.worldName = worldName;
            this.cause = cause;
            this.sleepers = sleepers;
            this.nonSleepers = nonSleepers;
        }

        public String getWorldName()
        {
            return worldName;
        }

        public Cause getCause()
        {
            return cause;
        }

        public List<PlayerSnapshot> getSleepers()
        {
            return sleepers;
        }

        public List<PlayerSnapshot> getNonSleepers()
        {
            return nonSleepers;
        }

        public List<UUID> getSleptPlayerIds()
        {
            return sleepers.stream().map(PlayerSnapshot::getPlayerId).collect(Collectors.toList());
        }

        public List<String> getSleptPlayerNames()
        {
            return sleepers.stream().map(PlayerSnapshot::getPlayerName).collect(Collectors.toList());
        }

        public List<UUID> getNonSleptPlayerIds()
        {
            return nonSleepers.stream().map(PlayerSnapshot::getPlayerId).collect(Collectors.toList());
        }

        public List<String> getNonSleptPlayerNames()
        {
            return nonSleepers.stream().map(PlayerSnapshot::getPlayerName).collect(Collectors.toList());
        }
    }

    public static final class PlayerSnapshot
    {
        private final UUID playerId;
        private final String playerName;
        private final String worldName;
        private final boolean slept;

        private PlayerSnapshot(UUID playerId, String playerName, String worldName, boolean slept)
        {
            this.playerId = playerId;
            this.playerName = playerName;
            this.worldName = worldName;
            this.slept = slept;
        }

        public static PlayerSnapshot fromPlayer(Player player, boolean slept)
        {
            return new PlayerSnapshot(player.getUniqueId(), player.getName(), player.getWorld().getName(), slept);
        }

        public UUID getPlayerId()
        {
            return playerId;
        }

        public String getPlayerName()
        {
            return playerName;
        }

        public String getWorldName()
        {
            return worldName;
        }

        public boolean hasSlept()
        {
            return slept;
        }
    }

}

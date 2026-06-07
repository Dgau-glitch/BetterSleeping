package be.betterplugins.bettersleeping.services.sleeping;

import be.betterplugins.bettersleeping.api.BecomeDayEvent;
import be.betterplugins.bettersleeping.api.BecomeDayEvent.Cause;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.model.SleepStatus;
import be.betterplugins.bettersleeping.model.sleeping.SleepWorld;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.util.TimeUtil;
import be.betterplugins.core.messaging.logging.BPLogger;
import be.betterplugins.core.messaging.messenger.MsgEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SleepWorldTicker
{
    private final MessageDeliveryService messageDeliveryService;

    private final SleepWorld sleepWorld;
    private final Set<UUID> sleepers;

    private final double daySpeedup;
    private final double nightSpeedup;
    private final double sleepSpeedup;

    private TimeState timeState;
    private boolean isSkipping;

    public SleepWorldTicker(ConfigContainer config, SleepWorld sleepWorld, MessageDeliveryService messageDeliveryService, BPLogger logger)
    {
        this.messageDeliveryService = messageDeliveryService;

        this.sleepWorld = sleepWorld;
        this.sleepers = new HashSet<>();

        YamlConfiguration sleepConfig = config.getSleeping_settings();
        double dayDuration = getGeneralOrPerWorld("day_length", sleepConfig) * 20;
        double nightDuration = getGeneralOrPerWorld("night_length", sleepConfig) * 20;
        double skippedNightDuration = getGeneralOrPerWorld("night_skip_length", sleepConfig) * 20;

        this.daySpeedup   = Math.min(TimeUtil.DAY_DURATION / dayDuration, 14000);
        this.nightSpeedup = Math.min(TimeUtil.NIGHT_DURATION / nightDuration, 10000);
        this.sleepSpeedup = Math.min(TimeUtil.NIGHT_DURATION / skippedNightDuration, 10000);

        this.isSkipping = false;
        this.timeState = TimeState.fromWorld(sleepWorld);

        logger.log(Level.FINEST, "Day speedup: " + daySpeedup);
        logger.log(Level.FINEST, "Night speedup: " + nightSpeedup);
        logger.log(Level.FINEST, "Sleep speedup: " + sleepSpeedup);
    }

    private Double getGeneralOrPerWorld(String subPath, YamlConfiguration config)
    {
        String perWorldPath = "world_settings." + sleepWorld.getWorldName() + "." + subPath;
        if (config.contains( perWorldPath ))
        {
            return config.getDouble( perWorldPath );
        }
        else
        {
            return config.getDouble( subPath );
        }
    }

    /**
     * Mark a player as sleeping, he does not actually have to sleep
     * Will fail silently when the player is already sleeping
     *
     * @param sleeper the relevant player
     */
    public void addSleeper(Player sleeper)
    {
        List<Player> players = sleepWorld.getAllPlayersInWorld();
        players.removeIf(player -> player.getUniqueId().equals(sleeper.getUniqueId()));

        if (!sleeper.isSleeping())
        {
            sleepers.add(sleeper.getUniqueId());

            SleepStatus sleepStatus = getSleepStatus();
            this.messageDeliveryService.send(players, "bed_enter_broadcast",
                    new MsgEntry("<num_sleeping>", sleepStatus.getNumSleepers()),
                    new MsgEntry("<needed_sleeping>", sleepStatus.getNumNeeded()),
                    new MsgEntry("<remaining_sleeping>", sleepStatus.getNumMissing()),
                    new MsgEntry("<player>", sleeper.getName()));
        }
    }


    /**
     * Mark a player as no longer sleeping
     *
     * @param player the relevant player
     */
    public void removeSleeper(Player player)
    {
        removeSleeper(player.getUniqueId());
    }

    /**
     * Mark a player as no longer sleeping by stable player id.
     *
     * @param playerId the relevant player id
     */
    public void removeSleeper(UUID playerId)
    {
        sleepers.remove(playerId);
    }


    /**
     * Reset the internal players sleeping state: makes BetterSleeping think all players woke up
     */
    public void resetSleepers()
    {
        this.sleepers.clear();
    }

    /**
     * Get the amount of players that count as sleeping
     *
     * @return the amount of sleeping players
     */
    public SleepStatus getSleepStatus()
    {
        return new SleepStatus
        (
            this.sleepers.size(),
            this.sleepWorld.getNumNeeded(),
            this.sleepWorld.getAllPlayersInWorld().size(),
            this.daySpeedup,
            this.nightSpeedup,
            this.sleepSpeedup
        );
    }

    private List<Player> getOnlinePlayersInManagedWorld()
    {
        return this.sleepWorld.getAllPlayersInWorld().stream()
                .filter(Player::isOnline)
                .collect(Collectors.toList());
    }

    private void retainValidSleepers(List<Player> playersInWorld)
    {
        Set<UUID> validPlayerIds = playersInWorld.stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());
        this.sleepers.retainAll(validPlayerIds);
    }


    public boolean isDayTime()
    {
        return TimeUtil.isDayTime(this.sleepWorld.getWorldTime());
    }

    private double calcSpeedup()
    {
        final double speedup;

        if ( this.sleepWorld.getWorldTime() >= TimeUtil.TIME_NIGHT_START )
        {
            speedup = isSkipping ? sleepSpeedup : nightSpeedup;
        }
        else
        {
            speedup = this.daySpeedup;
        }

        return speedup;
    }

    public void tick()
    {
        List<Player> playersInWorld = getOnlinePlayersInManagedWorld();
        retainValidSleepers(playersInWorld);

        // Calculate the amounts
        int numSleepers = sleepers.size();
        int numNeeded = sleepWorld.getNumNeeded();


        // Only start skipping when enough players sleep & at least someone sleeps
        if (!isSkipping)
        {
            if (numSleepers >= numNeeded && numSleepers > 0)
            {
                this.isSkipping = true;
                messageDeliveryService.send(playersInWorld, "enough_sleeping");
            }

            // Handle proceeding to a next time state
            TimeState nextState = TimeState.fromWorld( sleepWorld );
            if (this.timeState.isNextState( nextState ))
            {
                this.timeState = nextState;
                switch (nextState)
                {
                    case CAN_SLEEP_SOON:
                        messageDeliveryService.send(playersInWorld, "sleep_possible_soon");
                        break;
                    case CAN_SLEEP:
                        messageDeliveryService.send(playersInWorld, "sleep_possible_now");
                        break;
                    case CANNOT_SLEEP:
                    default:
                        break;
                }
            }
        }
        else
        {
            this.sleepWorld.clearWeather();
        }

        // Calculate the acceleration
        final double acceleration = calcSpeedup();

        // Check whether another cause skipped the night and allow this if no one is sleeping
        boolean wasNightExternallyChanged = this.sleepWorld.getWorldTime() != Math.floor(this.sleepWorld.getInternalTime()) && numSleepers == 0;
        if (wasNightExternallyChanged)
        {
            this.sleepWorld.setTime( this.sleepWorld.getWorldTime() );
        }

        // Set the correct time
        boolean isNightSkipped = this.sleepWorld.addTime( acceleration );

        if (isNightSkipped)
        {
            messageDeliveryService.send(
                    new ArrayList<>(playersInWorld),
                    "morning_message",
                    new MsgEntry("<num>", numSleepers)
            );


            // Find the cause for night skipping
            final Cause cause = isSkipping ? Cause.SLEEPING : Cause.NATURAL;

            // Find all players that slept
            final List<Player> restedPlayers = playersInWorld.stream()
                    .filter(player -> this.sleepers.contains(player.getUniqueId()))
                    .collect(Collectors.toList());

            // Get all players that did not sleep
            final List<Player> tiredPlayers = new ArrayList<>(playersInWorld);
            tiredPlayers.removeIf(player -> this.sleepers.contains( player.getUniqueId() ));

            BecomeDayEvent event = new BecomeDayEvent(sleepWorld.getWorld(), cause, restedPlayers, tiredPlayers);
            Bukkit.getServer().getPluginManager().callEvent( event );

            this.isSkipping = false;
            this.sleepers.clear();
        }
    }

    enum TimeState
    {
        CANNOT_SLEEP,
        CAN_SLEEP_SOON,
        CAN_SLEEP;

        public boolean isNextState(TimeState timeState)
        {
            return values()[(this.ordinal() + 1) % values().length] == timeState;
        }

        public static TimeState fromWorld(SleepWorld world)
        {
            long time = world.getWorldTime();

            // During daytime
            long almostSkipTime = TimeUtil.BED_TIME_NIGHT - 1200;
            if (time >= 0 && time < almostSkipTime)
            {
                return CANNOT_SLEEP;
            }
            // One minute before nighttime
            else if (time >= almostSkipTime && time < TimeUtil.BED_TIME_NIGHT)
            {
                return CAN_SLEEP_SOON;
            }
            // Nighttime
            else
            {
                return CAN_SLEEP;
            }
        }
    }
}

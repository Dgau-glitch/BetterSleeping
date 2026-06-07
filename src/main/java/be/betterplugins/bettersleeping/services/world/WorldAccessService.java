package be.betterplugins.bettersleeping.services.world;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldId;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Boundary for Bukkit/Folia world access. Domain services should depend on this
 * interface instead of directly mutating World objects.
 */
public interface WorldAccessService
{
    SleepWorldId getId(World world);

    World getWorld(SleepWorldId worldId);

    Location getSchedulingLocation(SleepWorldId worldId);

    List<Player> getAllPlayersInWorld(SleepWorldId worldId);

    long getTime(SleepWorldId worldId);

    void setTime(SleepWorldId worldId, long time);

    boolean isClearWeather(SleepWorldId worldId);

    void clearWeather(SleepWorldId worldId);

    boolean isInWorld(SleepWorldId worldId, Player player);
}

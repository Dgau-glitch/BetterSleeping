package be.betterplugins.bettersleeping.services.world;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldId;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class FoliaWorldAccessService implements WorldAccessService
{
    @Override
    public SleepWorldId getId(World world)
    {
        return SleepWorldId.fromWorld(world);
    }

    @Override
    public World getWorld(SleepWorldId worldId)
    {
        World world = Bukkit.getWorld(worldId.getUid());
        return world != null ? world : Bukkit.getWorld(worldId.getName());
    }

    @Override
    public Location getSchedulingLocation(SleepWorldId worldId)
    {
        World world = requireWorld(worldId);
        return world.getSpawnLocation();
    }

    @Override
    public List<Player> getAllPlayersInWorld(SleepWorldId worldId)
    {
        World world = getWorld(worldId);
        return world == null ? Collections.emptyList() : new ArrayList<>(world.getPlayers());
    }

    @Override
    public long getTime(SleepWorldId worldId)
    {
        return requireWorld(worldId).getTime();
    }

    @Override
    public void setTime(SleepWorldId worldId, long time)
    {
        requireWorld(worldId).setTime(time);
    }

    @Override
    public boolean isClearWeather(SleepWorldId worldId)
    {
        return requireWorld(worldId).isClearWeather();
    }

    @Override
    public void clearWeather(SleepWorldId worldId)
    {
        World world = requireWorld(worldId);
        if (!world.isClearWeather())
        {
            world.setStorm(false);
            world.setThundering(false);
        }
    }

    @Override
    public boolean isInWorld(SleepWorldId worldId, Player player)
    {
        return player.getWorld().getUID().equals(worldId.getUid());
    }

    private World requireWorld(SleepWorldId worldId)
    {
        World world = getWorld(worldId);
        if (world == null)
            throw new IllegalStateException("World is not loaded: " + worldId);
        return world;
    }
}

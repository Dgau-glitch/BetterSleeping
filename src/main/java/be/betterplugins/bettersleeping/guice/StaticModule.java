package be.betterplugins.bettersleeping.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StaticModule extends AbstractModule
{
    @Provides
    public List<World> provideAllWorlds()
    {
        return Bukkit.getWorlds();
    }

    @Provides
    @Named("online_players")
    public List<Player> provideOnlinePlayers()
    {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    @Provides
    @Named("normal_world_count")
    public Long provideNormalWorldCount()
    {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL || world.getEnvironment() == World.Environment.CUSTOM)
                .count();
    }

}

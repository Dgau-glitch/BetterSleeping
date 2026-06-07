package be.betterplugins.bettersleeping.model.sleeping;

import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for a managed world. Long-lived plugin state should use
 * this instead of retaining mutable World instances as map keys.
 */
public final class SleepWorldId
{
    private final UUID uid;
    private final String name;

    public SleepWorldId(UUID uid, String name)
    {
        this.uid = uid;
        this.name = name;
    }

    public static SleepWorldId fromWorld(World world)
    {
        return new SleepWorldId(world.getUID(), world.getName());
    }

    public UUID getUid()
    {
        return uid;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof SleepWorldId)) return false;
        SleepWorldId that = (SleepWorldId) o;
        return Objects.equals(uid, that.uid) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uid, name);
    }

    @Override
    public String toString()
    {
        return name + "(" + uid + ")";
    }
}

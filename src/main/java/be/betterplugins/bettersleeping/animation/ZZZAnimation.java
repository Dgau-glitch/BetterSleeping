package be.betterplugins.bettersleeping.animation;

import be.betterplugins.bettersleeping.animation.location.IVariableLocation;
import be.betterplugins.bettersleeping.animation.location.PlayerSleepLocation;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ZZZAnimation extends Animation implements PreComputeable<Double>
{

    private final List<Double> rotations;
    private static final Map<Double, List<Vector>> animationComputations = new ConcurrentHashMap<>();

    private final Particle particle;
    private final double size;
    private final double spacing;

    private final long delayTicks;

    private final PluginScheduler scheduler;


    public ZZZAnimation(Particle particle, double size, double spacing, long delayMilliseconds, PluginScheduler scheduler)
    {
        this.scheduler = scheduler;

        this.particle = particle;
        this.size = size;
        this.spacing = spacing;

        this.delayTicks = Math.max(1L, Math.round(delayMilliseconds / 50.0));

        this.rotations = new ArrayList<>();
        for (double rotation = 0; rotation < 2 * Math.PI; rotation += Math.PI / 16)
        {
            this.rotations.add(rotation);
            preCompute(rotation);
        }

    }


    @Override
    public void preCompute(Double rotation)
    {
        if (animationComputations.containsKey(rotation))
            return;

        // Do only math/vector calculations async.
        scheduler.runAsync(() -> {

            // Load the raw animation by following the path

            List<Vector> rawLocations = new ArrayList<>();
            for (int i = -1; i < 2; i++)
            {
                double correctedSize = size / spacing;
                double spaceSize = 3;
                Vector currentPosition = new Vector(i * (correctedSize + spaceSize), 0, 0);

                for (double t = 0; t < size; t += spacing)
                {
                    Vector movement = new Vector(1, 0, 0);
                    rawLocations.add(currentPosition.add(movement).clone());
                }


                for (double t = 0; t < size; t += spacing)
                {
                    Vector movement = new Vector(-1, -1, 0);
                    rawLocations.add(currentPosition.add(movement).clone());
                }


                for (double t = 0; t < size; t += spacing)
                {
                    Vector movement = new Vector(1, 0, 0);
                    rawLocations.add(currentPosition.add(movement).clone());
                }
            }

            // Perform calculations

            double halfSize = (3 * size) / 2;
            Vector offset = new Vector(-halfSize, halfSize, 0).add(new Vector(0, 1 / spacing, 0));
            List<Vector> locations = new ArrayList<>();
            for (Vector position : rawLocations)
            {
                position.add(offset);
                position.rotateAroundY(rotation);
                position.multiply(spacing);
                locations.add(position);
            }

            animationComputations.put(rotation, Collections.unmodifiableList(locations));
        });
    }


    @Override
    public boolean isComputed(Double rotation)
    {
        return animationComputations.containsKey(rotation);
    }


    @Override
    public TaskHandle startAnimation(IVariableLocation variableLocation)
    {
        if (!(variableLocation instanceof PlayerSleepLocation))
            return super.startAnimation(variableLocation);

        super.startAnimation(variableLocation);
        PlayerSleepLocation playerSleepLocation = (PlayerSleepLocation) variableLocation;
        Player player = playerSleepLocation.getPlayer();
        AtomicInteger iteration = new AtomicInteger();

        return scheduler.repeatForEntity(player, () -> {
            if (!super.isPlaying)
                return;

            // Pick a rotation in the entity-owning context.
            double rotation = this.rotations.get(iteration.getAndUpdate(value -> (value + 1) % this.rotations.size()));
            Location origin = playerSleepLocation.getLocation();
            if (origin.getWorld() == null)
                return;

            // Draw particles in the entity-owning region instead of from an async task.
            List<Vector> locations = animationComputations.get(rotation);
            if (locations != null)
            {
                for (Vector location : locations)
                    origin.getWorld().spawnParticle(particle, origin.clone().add(location), 1);
            }
        }, 1L, delayTicks);
    }
}

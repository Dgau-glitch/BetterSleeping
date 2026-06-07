package be.betterplugins.bettersleeping.model.world;

import be.betterplugins.core.messaging.logging.BPLogger;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.logging.Level;

public class WorldState
{

    private final boolean doDayLightCycle;
    private final boolean supportsSleepingPercentage;
    private final int percentageSetting;
    private final Long time;
    private final Boolean storm;
    private final Boolean thundering;

    /**
     * Capture the world state of a given world.
     * This state can be copied to any other world, or be used to make temporary state changes to the given world.
     *
     * @param world the world for which we want to capture the state.
     */
    public WorldState(final World world, BPLogger logger)
    {
        Boolean doTime = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
        this.doDayLightCycle = doTime == null || doTime;

        this.supportsSleepingPercentage = this.hasGameRule( "playersSleepingPercentage" );
        if (supportsSleepingPercentage)
        {
            Integer percentage = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE);
            this.percentageSetting = percentage == null ? 100 : percentage;
        }
        else
        {
            this.percentageSetting = 100;
        }

        this.time = world.getTime();
        this.storm = world.hasStorm();
        this.thundering = world.isThundering();

        logger.log(Level.FINER, "Worldstate for world '" + world.getName() + "': Cycle time: " + doDayLightCycle + ", 1.17+? " + supportsSleepingPercentage + ", percentage setting? " + percentageSetting + ", time: " + time + ", storm: " + storm + ", thunder: " + thundering);
    }

    private boolean hasGameRule(String gameRule)
    {
        for (GameRule rule : GameRule.values())
        {
            if (rule.getName().equalsIgnoreCase(gameRule))
                return true;
        }
        return false;
    }

    /**
     * Manually create a world state that can be applied to any world.
     *
     * @param doDayLightCycle the value to which DoDayLightCycle should be set in this state.
     */
    public WorldState(final boolean doDayLightCycle, final int percentageSetting)
    {
        this.doDayLightCycle = doDayLightCycle;
        this.supportsSleepingPercentage = this.hasGameRule( "playersSleepingPercentage" );
        this.percentageSetting = percentageSetting;
        this.time = null;
        this.storm = null;
        this.thundering = null;
    }

    /**
     * Update the given world to this state. Must be called from the Folia global region because time, weather and game rules are global world state.
     *
     * @param world the world whose state is to be modified.
     */
    public void applyState(final World world)
    {
        world.setGameRule( GameRule.DO_DAYLIGHT_CYCLE, this.doDayLightCycle );
        if (this.supportsSleepingPercentage)
        {
            world.setGameRule( GameRule.PLAYERS_SLEEPING_PERCENTAGE, this.percentageSetting );
        }
        if (this.time != null)
        {
            world.setTime(this.time);
        }
        if (this.storm != null)
        {
            world.setStorm(this.storm);
        }
        if (this.thundering != null)
        {
            world.setThundering(this.thundering);
        }
    }
}

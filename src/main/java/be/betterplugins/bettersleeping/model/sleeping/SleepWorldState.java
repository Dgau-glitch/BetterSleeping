package be.betterplugins.bettersleeping.model.sleeping;

/**
 * Pure domain state for a managed sleeping world.
 */
public class SleepWorldState
{
    private double time;

    public SleepWorldState(double initialTime)
    {
        this.time = normalize(initialTime);
    }

    public double getTime()
    {
        return time;
    }

    public void setTime(double newTime)
    {
        this.time = normalize(newTime);
    }

    public boolean addTime(double deltaTicks)
    {
        if (deltaTicks < 0)
            throw new IllegalArgumentException("deltaTicks must be non-negative");

        this.time += deltaTicks;
        boolean nextDay = this.time >= 24000;
        this.time = normalize(this.time);
        return nextDay;
    }

    private double normalize(double time)
    {
        double normalized = time % 24000;
        return normalized < 0 ? normalized + 24000 : normalized;
    }
}

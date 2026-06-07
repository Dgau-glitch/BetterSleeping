package be.betterplugins.bettersleeping.animation;

import be.betterplugins.bettersleeping.animation.location.IVariableLocation;
import be.betterplugins.bettersleeping.services.scheduler.RetiredTaskHandle;
import be.betterplugins.bettersleeping.services.scheduler.TaskHandle;

public abstract class Animation
{

    protected boolean isPlaying = false;

    public Animation() {}

    public TaskHandle startAnimation(IVariableLocation variableLocation)
    {
        this.isPlaying = true;
        return new RetiredTaskHandle();
    }

    public void stopAnimation()
    {
        this.isPlaying = false;
    }

}

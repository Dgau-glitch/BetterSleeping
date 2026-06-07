package be.betterplugins.bettersleeping.services.scheduler;

public final class RetiredTaskHandle implements TaskHandle
{
    @Override
    public void cancel()
    {
        // Nothing to cancel: the entity scheduler was already retired.
    }

    @Override
    public boolean isCancelled()
    {
        return true;
    }
}

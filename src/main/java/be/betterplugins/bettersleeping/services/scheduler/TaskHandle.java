package be.betterplugins.bettersleeping.services.scheduler;

/**
 * Small plugin-owned task handle that keeps Folia scheduler details out of the
 * rest of the codebase.
 */
public interface TaskHandle
{
    void cancel();

    boolean isCancelled();
}

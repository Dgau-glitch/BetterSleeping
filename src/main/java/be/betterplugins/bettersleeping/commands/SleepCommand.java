package be.betterplugins.bettersleeping.commands;

import be.betterplugins.bettersleeping.model.sleeping.SleepWorldManager;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.bettersleeping.util.TimeUtil;
import be.betterplugins.core.commands.shortcuts.PlayerBPCommand;
import be.betterplugins.core.messaging.messenger.Messenger;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SleepCommand extends PlayerBPCommand
{

    private final SleepWorldManager sleepWorldManager;
    private final PluginScheduler scheduler;
    private final MessageDeliveryService messageDeliveryService;

    public SleepCommand(Messenger messenger, SleepWorldManager sleepWorldManager, PluginScheduler scheduler, MessageDeliveryService messageDeliveryService)
    {
        super(messenger);
        this.sleepWorldManager = sleepWorldManager;
        this.scheduler = scheduler;
        this.messageDeliveryService = messageDeliveryService;
    }

    @Override
    public @NotNull String getCommandName()
    {
        return "sleep";
    }

    @Override
    public @NotNull List<String> getAliases()
    {
        return Collections.singletonList("s");
    }

    @Override
    public @NotNull String getPermission()
    {
        return "bettersleeping.sleepcommand";
    }

    @Override
    public boolean execute(@NotNull Player player, @NotNull Command command, @NotNull String[] strings)
    {
        scheduler.runForEntity(player, () -> executeInEntityContext(player));
        return true;
    }

    private void executeInEntityContext(Player player)
    {
        World world = player.getWorld();

        // Make sure this world is enabled
        if (!sleepWorldManager.isWorldEnabled(world))
        {
            messageDeliveryService.send(player, "world_disabled");
            return;
        }

        // Make sure the time is right in the player's world
        if (!TimeUtil.isSleepPossible(world))
        {
            messageDeliveryService.send(player, "command_sleep_notnight");
            return;
        }

        sleepWorldManager.addSleeper(player);
    }
}

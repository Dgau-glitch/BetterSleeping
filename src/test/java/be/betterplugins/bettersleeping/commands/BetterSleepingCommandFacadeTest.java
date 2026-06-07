package be.betterplugins.bettersleeping.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BetterSleepingCommandFacadeTest
{
    private final Command command = mock(Command.class);
    private final CommandExecutor delegate = mock(CommandExecutor.class);

    @Test
    public void userSeesOnlyUserSubcommands()
    {
        BetterSleepingCommandFacade facade = new BetterSleepingCommandFacade(delegate);
        CommandSender sender = senderWithPermissions("bettersleeping.help", "bettersleeping.status", "bettersleeping.buffs", "bettersleeping.shout");

        List<String> completions = facade.onTabComplete(sender, command, "bs", new String[]{""});

        assertEquals(Arrays.asList("help", "status", "buffs", "shout"), completions);
    }

    @Test
    public void adminSeesAdminSubcommands()
    {
        BetterSleepingCommandFacade facade = new BetterSleepingCommandFacade(delegate);
        CommandSender sender = senderWithPermissions(
                "bettersleeping.help",
                "bettersleeping.status",
                "bettersleeping.buffs",
                "bettersleeping.shout",
                "bettersleeping.reload",
                "bettersleeping.sleepcommand",
                "bettersleeping.version"
        );

        List<String> completions = facade.onTabComplete(sender, command, "bs", new String[]{""});

        assertEquals(Arrays.asList("help", "status", "buffs", "shout", "reload", "sleep", "version"), completions);
    }

    @Test
    public void noPermissionSeesNoSubcommandsOrHiddenArguments()
    {
        BetterSleepingCommandFacade facade = new BetterSleepingCommandFacade(delegate);
        CommandSender sender = senderWithPermissions();

        assertTrue(facade.onTabComplete(sender, command, "bs", new String[]{""}).isEmpty());
        assertTrue(facade.onTabComplete(sender, command, "bs", new String[]{"reload", ""}).isEmpty());
    }

    @Test
    public void prefixFilteringStillAppliesPermissionFirst()
    {
        BetterSleepingCommandFacade facade = new BetterSleepingCommandFacade(delegate);
        CommandSender sender = senderWithPermissions("bettersleeping.status", "bettersleeping.shout");

        assertEquals(Arrays.asList("status", "shout"), facade.onTabComplete(sender, command, "bs", new String[]{"s"}));
    }

    private CommandSender senderWithPermissions(String... permissions)
    {
        Set<String> permissionSet = new HashSet<>(Arrays.asList(permissions));
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(anyString())).thenAnswer(invocation -> permissionSet.contains(invocation.getArgument(0)));
        return sender;
    }
}

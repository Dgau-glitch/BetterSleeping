package be.betterplugins.bettersleeping.commands;

import be.betterplugins.core.commands.BPCommandHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class BetterSleepingCommandFacade implements CommandExecutor, TabCompleter
{
    private static final List<SubcommandDescriptor> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            new SubcommandDescriptor("help", "bettersleeping.help", "h"),
            new SubcommandDescriptor("status", "bettersleeping.status", "s"),
            new SubcommandDescriptor("buffs", "bettersleeping.buffs", "b"),
            new SubcommandDescriptor("shout", "bettersleeping.shout"),
            new SubcommandDescriptor("reload", "bettersleeping.reload", "r"),
            new SubcommandDescriptor("sleep", "bettersleeping.sleepcommand"),
            new SubcommandDescriptor("version", "bettersleeping.version", "v")
    ));

    private final CommandExecutor delegate;

    @Inject
    public BetterSleepingCommandFacade(BPCommandHandler delegate)
    {
        this((CommandExecutor) delegate);
    }

    BetterSleepingCommandFacade(CommandExecutor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        return delegate.onCommand(sender, command, label, args);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args)
    {
        if (args.length == 0)
            return visibleSubcommandNames(sender);

        if (args.length == 1)
        {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return visibleSubcommandNames(sender).stream()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        Optional<SubcommandDescriptor> subcommand = findSubcommand(args[0]);
        if (!subcommand.isPresent() || !subcommand.get().hasPermission(sender))
            return Collections.emptyList();

        return subcommand.get().complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private List<String> visibleSubcommandNames(CommandSender sender)
    {
        return SUBCOMMANDS.stream()
                .filter(subcommand -> subcommand.hasPermission(sender))
                .map(SubcommandDescriptor::getName)
                .collect(Collectors.toList());
    }

    private Optional<SubcommandDescriptor> findSubcommand(String input)
    {
        String normalized = input.toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream()
                .filter(subcommand -> subcommand.matches(normalized))
                .findFirst();
    }

    static List<SubcommandDescriptor> getSubcommandsForTests()
    {
        return SUBCOMMANDS;
    }

    static final class SubcommandDescriptor
    {
        private final String name;
        private final String permission;
        private final List<String> aliases;
        private final ArgumentCompleter argumentCompleter;

        private SubcommandDescriptor(String name, String permission, String... aliases)
        {
            this(name, permission, (sender, args) -> Collections.emptyList(), aliases);
        }

        private SubcommandDescriptor(String name, String permission, ArgumentCompleter argumentCompleter, String... aliases)
        {
            this.name = name;
            this.permission = permission;
            this.argumentCompleter = argumentCompleter;
            this.aliases = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(aliases)));
        }

        private String getName()
        {
            return name;
        }

        private boolean matches(String input)
        {
            return name.equals(input) || aliases.contains(input);
        }

        private boolean hasPermission(CommandSender sender)
        {
            return sender.hasPermission(permission);
        }

        private List<String> complete(CommandSender sender, String[] args)
        {
            return argumentCompleter.complete(sender, args);
        }
    }

    @FunctionalInterface
    private interface ArgumentCompleter
    {
        List<String> complete(CommandSender sender, String[] args);
    }
}

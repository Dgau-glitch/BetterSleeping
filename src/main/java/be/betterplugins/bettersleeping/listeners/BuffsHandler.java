package be.betterplugins.bettersleeping.listeners;

import be.betterplugins.bettersleeping.api.BecomeDayEvent;
import be.betterplugins.bettersleeping.api.BecomeDayEvent.PlayerSnapshot;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.bettersleeping.model.permissions.BypassChecker;
import be.betterplugins.bettersleeping.services.messaging.MessageDeliveryService;
import be.betterplugins.bettersleeping.services.scheduler.PluginScheduler;
import be.betterplugins.core.messaging.logging.BPLogger;
import be.betterplugins.core.messaging.messenger.MsgEntry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Singleton
public class BuffsHandler implements Listener {


    private final BPLogger logger;
    private final MessageDeliveryService messageDeliveryService;
    private final PluginScheduler scheduler;
    private final JavaPlugin plugin;
    private final BypassChecker bypassChecker;

    private final Set<PotionEffect> sleepingBuffs;
    private final Set<PotionEffect> sleepingDebuffs;

    private final List<String> sleepingCommands;
    private final List<String> nonSleepingCommands;


    /**
     * Event handler for {@link be.betterplugins.bettersleeping.api.BecomeDayEvent}
     */
    @Inject
    public BuffsHandler(BPLogger logger, MessageDeliveryService messageDeliveryService, PluginScheduler scheduler, JavaPlugin plugin, BypassChecker bypassChecker, ConfigContainer config)
    {
        this.logger = logger;
        this.messageDeliveryService = messageDeliveryService;
        this.scheduler = scheduler;
        this.plugin = plugin;
        this.bypassChecker = bypassChecker;

        YamlConfiguration buffsConfig = config.getBuffs();
        this.sleepingBuffs   = readPotions(buffsConfig, "sleeper_buffs");
        this.sleepingDebuffs = readPotions(buffsConfig, "non_sleeper_debuffs");

        this.sleepingCommands    = buffsConfig.getStringList( "sleeper_commands" );
        this.nonSleepingCommands = buffsConfig.getStringList( "non_sleeper_commands" );
    }


    public Set<PotionEffect> getBuffs()
    {
        return sleepingBuffs;
    }

    public Set<PotionEffect> getDebuffs()
    {
        return sleepingDebuffs;
    }


    @EventHandler
    public void onSetToDay(BecomeDayEvent event)
    {
        // Only handle buffs if players (possibly) slept
        if (event.getCause() == BecomeDayEvent.Cause.OTHER)
            return;

        if (sleepingBuffs.size() > 0)
        {
            for (PlayerSnapshot snapshot : event.getPlayersWhoSleptSnapshots())
                giveEffects(snapshot, sleepingBuffs, sleepingCommands, "buff_received", false);
        }

        if (sleepingDebuffs.size() > 0)
        {
            for (PlayerSnapshot snapshot : event.getPlayersWhoDidNotSleepSnapshots())
                giveEffects(snapshot, sleepingDebuffs, nonSleepingCommands, "debuff_received", true);
        }
    }


    private void giveEffects(PlayerSnapshot snapshot, Set<PotionEffect> effects, List<String> commands, String messageKey, boolean skipBypassed)
    {
        scheduler.runGlobal(() -> {
            Player player = plugin.getServer().getPlayer(snapshot.getPlayerId());
            if (player == null || !player.isOnline())
                return;

            scheduler.runForEntity(player, () -> giveEffectsInEntityContext(player, snapshot, effects, commands, messageKey, skipBypassed));
        });
    }

    private void giveEffectsInEntityContext(Player player, PlayerSnapshot snapshot, Set<PotionEffect> effects, List<String> commands, String messageKey, boolean skipBypassed)
    {
        if (skipBypassed && bypassChecker.isPlayerBypassed(player))
            return;

        messageDeliveryService.send(
                player,
                messageKey,
                new MsgEntry("<var>", "" + effects.size())
        );

        // Add (de)buffs in the player entity context.
        player.addPotionEffects(effects);

        // Execute each command on the global region scheduler.
        for (String command : commands)
        {
            String commandToRun = command.replace("<user>", snapshot.getPlayerName());
            scheduler.runGlobal(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandToRun));
        }
    }


    /**
     * Read all {@link PotionEffect}s in a configuration section
     * @param config the config file to be read
     * @param section the section to be searched for the BetterSleeping potion effect format
     * @return A set of all valid potions in this section
     */
    private Set<PotionEffect> readPotions(FileConfiguration config, String section)
    {
        Set<PotionEffect> potions = new HashSet<>();

        // Prevent reading faulty config
        if ( ! config.isConfigurationSection(section) )
            return potions;

        // Only read effects when the config section exists
        ConfigurationSection configSection = config.getConfigurationSection(section);
        if (configSection != null)
        {
            for (String path : configSection.getKeys(false)) {
                int time = config.getInt(section + "." + path + ".time");
                int level = config.getInt(section + "." + path + ".level");

                PotionEffectType type = PotionEffectType.getByName(path.toUpperCase());


                // Only add if all fields are valid
                if (type != null && time > 0 && level > 0)
                    potions.add(new PotionEffect(type, 20 * time, level - 1));
                else
                    logger.log(Level.CONFIG, "Faulty (de)buff: '" + path + "' of duration '" + time + "' and level '" + level + "'");
            }
        }

        return potions;
    }
}

package be.betterplugins.bettersleeping.guice;

import be.betterplugins.bettersleeping.hooks.EssentialsHook;
import be.betterplugins.bettersleeping.hooks.NoEssentialsHook;
import be.betterplugins.bettersleeping.model.ConfigContainer;
import be.betterplugins.core.messaging.logging.BPLogger;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class HooksModule extends AbstractModule
{

    @Provides
    @Singleton
    public EssentialsHook provideEssentialsHook(ConfigContainer config, BPLogger logger)
    {
        Plugin essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (essentials == null)
            return new NoEssentialsHook(config, logger);

        if (!(essentials instanceof Essentials))
        {
            logger.log(Level.WARNING, "Essentials hook disabled: found incompatible Essentials plugin class " + essentials.getClass().getName());
            return new NoEssentialsHook(config, logger);
        }

        return new EssentialsHook(config, logger);
    }

}

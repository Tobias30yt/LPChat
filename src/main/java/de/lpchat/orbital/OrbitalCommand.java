package de.lpchat.orbital;

import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Ordinary fishing rods; no commands, item tags or configuration. */
public final class OrbitalCommand implements Listener, AutoCloseable {
    private record Cast(EquipmentSlot hand, FishHook hook, boolean stab) {}
    private final JavaPlugin plugin;
    private final OrbitalEngine engine;
    private final NamespacedKey rodKey;
    private final Map<UUID, Cast> casts = new HashMap<>();
    private final Set<BukkitTask> pending = new HashSet<>();
    public OrbitalCommand(JavaPlugin plugin, OrbitalEngine engine) { this.plugin = plugin; this.engine = engine; this.rodKey = new NamespacedKey(plugin, "orbital_rod"); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.isCancelled() || !OrbitalAccess.allowed(event.getPlayer())) return;
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            EquipmentSlot hand = event.getHand();
            if (hand != null && isRod(player.getInventory().getItem(hand)))
                casts.put(player.getUniqueId(), new Cast(hand, event.getHook(), player.isSneaking()));
            return;
        }
        Cast cast = casts.get(player.getUniqueId());
        if (cast == null || !cast.hook.getUniqueId().equals(event.getHook().getUniqueId())) return;
        switch (event.getState()) {
            case REEL_IN, IN_GROUND, CAUGHT_ENTITY, CAUGHT_FISH -> { }
            default -> { return; }
        }
        casts.remove(player.getUniqueId());
        event.setCancelled(true);
        event.setExpToDrop(0);
        event.getHook().remove();
        ItemStack held = player.getInventory().getItem(cast.hand);
        if (!isRod(held)) return;
        try {
            Location target = aim(player);
            if (cast.stab) {
                if (pending.size() >= 2) throw new IllegalArgumentException("Es sind bereits zwei Schuesse vorgemerkt.");
                BukkitTask[] task = new BukkitTask[1];
                task[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    pending.remove(task[0]);
                    if (!player.isOnline()) return;
                    try { engine.fire(player, target, StrikePattern.STAB, 1, StrikePlan.Order.SIMULTANEOUS); }
                    catch (IllegalArgumentException e) { player.sendMessage(Component.text(e.getMessage())); }
                }, 60L);
                pending.add(task[0]);
            } else engine.fire(player, target, StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS);
            player.getInventory().setItem(cast.hand, null);
            player.playSound(player.getLocation(), "entity.item.break", 1, 1);
        } catch (IllegalArgumentException e) { player.sendMessage(Component.text(e.getMessage())); }
    }
    static Location aim(Player player) {
        Location eye = player.getEyeLocation();
        var direction = eye.getDirection();
        for (double distance = 0; distance <= 128; distance += .1) {
            Location point = eye.clone().add(direction.clone().multiply(distance));
            World world = point.getWorld();
            if (!world.isChunkLoaded(point.getBlockX() >> 4, point.getBlockZ() >> 4)) break;
            if (point.getY() < world.getMinHeight() || point.getY() >= world.getMaxHeight()) break;
            if (!point.getBlock().isPassable()) {
                return new Location(world, point.getBlockX() + .5, point.getBlockY() + 1,
                        point.getBlockZ() + .5, player.getLocation().getYaw(), 0);
            }
        }
        throw new IllegalArgumentException("Kein Block anvisiert (maximal 128 Bloecke). Kein Schuss ausgeloest.");
    }
    private boolean isRod(ItemStack item) {
        return item != null && item.getType() == Material.FISHING_ROD && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().get(rodKey, org.bukkit.persistence.PersistentDataType.STRING) != null;
    }
    public void completions(Player player) {
        if (OrbitalAccess.allowed(player)) player.addCustomChatCompletions(List.of(".orbital", ".orbital give"));
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { completions(event.getPlayer()); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(io.papermc.paper.event.player.ChatEvent event) {
        String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (!isCommand(message)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!OrbitalAccess.allowed(player)) {
            player.sendMessage(Component.text("Orbital ist ausschliesslich fuer Tobias30 freigeschaltet."));
            return;
        }
        if (!message.equalsIgnoreCase(".orbital give")) {
            player.sendMessage(Component.text("Verwendung: .orbital give")); return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(Component.text("Dein Inventar ist voll.")); return;
        }
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        var meta = rod.getItemMeta();
        meta.getPersistentDataContainer().set(rodKey, org.bukkit.persistence.PersistentDataType.STRING, "enabled");
        rod.setItemMeta(meta);
        player.getInventory().addItem(rod);
        player.sendMessage(Component.text("Orbital-Angel erhalten. Normal auswerfen: Nuke. Schleichend auswerfen: Stab. Danach Block anvisieren und einholen."));
    }
    static boolean isCommand(String message) {
        return message.equalsIgnoreCase(".orbital") || message.toLowerCase(Locale.ROOT).startsWith(".orbital ");
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Cast cast = casts.remove(event.getPlayer().getUniqueId());
        if (cast != null) cast.hook.remove();
    }
    @Override public void close() {
        for (Player player : plugin.getServer().getOnlinePlayers())
            if (OrbitalAccess.allowed(player)) player.removeCustomChatCompletions(List.of(".orbital", ".orbital give"));
        pending.forEach(BukkitTask::cancel); pending.clear();
        casts.values().forEach(c -> c.hook.remove()); casts.clear();
    }
}

package de.lpchat.orbital;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrbitalCommand implements TabExecutor, Listener, AutoCloseable {
    record Rod(StrikePattern pattern, int size, StrikePlan.Order order, boolean infinite, String id) {}
    private record Cast(Rod rod, EquipmentSlot hand, FishHook hook) {}
    private final OrbitalEngine engine;
    private final NamespacedKey rodKey;
    private final Map<UUID, Cast> casts = new HashMap<>();

    public OrbitalCommand(JavaPlugin plugin, OrbitalEngine engine) {
        this.engine = engine;
        rodKey = new NamespacedKey(plugin, "orbital_rod");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            OrbitalAccess.require(sender);
            Player player = (Player) sender;
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                player.sendMessage(Component.text("Orbital: /ob give <Muster> [Groesse] [true|false] [simultaneous|inward|outward]\n"
                        + "/ob <Muster> [x y z] [Groesse] [Reihenfolge] | /ob preview <Muster> [Groesse] | /ob cancel\n"
                        + "Ohne Koordinaten: Blickziel. Angel auswerfen, dann am Haken-Ziel einholen.\nMuster: "
                        + String.join(", ", Arrays.stream(StrikePattern.values()).map(StrikePattern::id).toList()), NamedTextColor.AQUA));
            } else if (args[0].equalsIgnoreCase("give")) {
                give(player, args);
            } else if (args[0].equalsIgnoreCase("cancel")) {
                if (args.length != 1) throw new IllegalArgumentException("/ob cancel");
                tell(player, engine.cancelAll() + " aktive TNT-Entities entfernt.");
            } else if (args[0].equalsIgnoreCase("preview")) {
                if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("/ob preview <Muster> [Groesse]");
                StrikePattern pattern = StrikePattern.parse(args[1]);
                engine.preview(player, aim(player), pattern, size(pattern, args, 2));
                tell(player, "Vorschau ohne Explosion. /ob " + pattern.id() + " loest einen echten Schuss aus.");
            } else {
                StrikePattern pattern = StrikePattern.parse(args[0]);
                if (args.length != 1 && (args.length < 4 || args.length > 6))
                    throw new IllegalArgumentException("/ob <Muster> [x y z] [Groesse] [simultaneous|inward|outward]");
                Location target;
                if (args.length == 1) target = aim(player);
                else {
                    Location base = player.getLocation();
                    target = new Location(player.getWorld(), coordinate(args[1], base.getX()),
                            coordinate(args[2], base.getY()), coordinate(args[3], base.getZ()), base.getYaw(), 0);
                }
                int count = engine.fire(player, target, pattern, size(pattern, args, 4),
                        args.length > 5 ? StrikePlan.Order.parse(args[5]) : StrikePlan.Order.SIMULTANEOUS);
                tell(player, pattern.id() + " gestartet: " + count + " TNT.");
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void give(Player player, String[] args) {
        if (args.length < 2 || args.length > 5)
            throw new IllegalArgumentException("/ob give <Muster> [Groesse] [true|false] [Reihenfolge]");
        StrikePattern pattern = StrikePattern.parse(args[1]);
        int size = size(pattern, args, 2);
        boolean infinite = args.length <= 3 || parseBoolean(args[3]);
        StrikePlan.Order order = args.length > 4 ? StrikePlan.Order.parse(args[4]) : StrikePlan.Order.SIMULTANEOUS;
        if (player.getInventory().firstEmpty() == -1) throw new IllegalArgumentException("Dein Inventar ist voll.");
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Orbital / " + pattern.id(), NamedTextColor.RED));
        meta.lore(List.of(Component.text("Nur Tobias30 | Groesse " + size, NamedTextColor.GRAY),
                Component.text("Auswerfen und einholen | " + (infinite ? "Unendlich" : "Einmalig"), NamedTextColor.GRAY)));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(rodKey, PersistentDataType.STRING,
                pattern.id() + ":" + size + ":" + order.name() + ":" + infinite + ":" + UUID.randomUUID());
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        tell(player, "Orbital-Angel erhalten. Ziel ist beim Einholen die Position des Hakens.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            EquipmentSlot hand = event.getHand();
            if (hand == null) return;
            Rod rod = readRod(player.getInventory().getItem(hand));
            if (rod == null) return;
            if (!OrbitalAccess.allowed(player)) {
                event.setCancelled(true);
                tell(player, "Diese Orbital-Angel ist nur fuer Tobias30.");
                return;
            }
            casts.put(player.getUniqueId(), new Cast(rod, hand, event.getHook()));
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
        Location target = event.getHook().getLocation().clone();
        target.setYaw(player.getLocation().getYaw());
        event.getHook().remove();
        try {
            OrbitalAccess.require(player);
            ItemStack held = player.getInventory().getItem(cast.hand);
            Rod current = readRod(held);
            if (current == null || !current.equals(cast.rod)) throw new IllegalArgumentException("Zum Einholen dieselbe Orbital-Angel halten.");
            engine.fire(player, target, current.pattern, current.size, current.order);
            if (!current.infinite) player.getInventory().setItem(cast.hand, new ItemStack(Material.AIR));
            tell(player, "Orbital " + current.pattern.id() + " gestartet.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Cast cast = casts.remove(event.getPlayer().getUniqueId());
        if (cast != null) cast.hook.remove();
    }

    private Rod readRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return null;
        String value = item.getItemMeta().getPersistentDataContainer().get(rodKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 5) return null;
            StrikePattern pattern = StrikePattern.parse(parts[0]);
            int size = Integer.parseInt(parts[1]);
            pattern.points(size, 0);
            return new Rod(pattern, size, StrikePlan.Order.parse(parts[2]), parseBoolean(parts[3]), UUID.fromString(parts[4]).toString());
        } catch (IllegalArgumentException e) { return null; }
    }

    static double coordinate(String input, double base) {
        try {
            double result = input.startsWith("~") ? base + (input.length() == 1 ? 0 : Double.parseDouble(input.substring(1))) : Double.parseDouble(input);
            if (!Double.isFinite(result) || Math.abs(result) > 30_000_000) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Ungueltige Koordinate: " + input); }
    }
    private static boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Unendlich: true oder false.");
    }
    private static int size(StrikePattern pattern, String[] args, int index) {
        int value;
        try { value = args.length > index ? Integer.parseInt(args[index]) : pattern.defaultSize(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Groesse muss eine ganze Zahl sein."); }
        pattern.points(value, 0);
        return value;
    }
    private static Location aim(Player player) {
        Location eye = player.getEyeLocation();
        var direction = eye.getDirection();
        for (double distance = 1; distance <= 128; distance += .5) {
            Location point = eye.clone().add(direction.clone().multiply(distance));
            if (!point.getWorld().isChunkLoaded(point.getBlockX() >> 4, point.getBlockZ() >> 4)) break;
            if (point.getY() < point.getWorld().getMinHeight() || point.getY() >= point.getWorld().getMaxHeight()) break;
            if (!point.getBlock().isPassable()) {
                point.setY(point.getBlockY() + 1);
                point.setYaw(player.getLocation().getYaw());
                return point;
            }
        }
        throw new IllegalArgumentException("Kein Block im Blick (maximal 128 Bloecke). Alternativ Zielkoordinaten angeben.");
    }
    private static void tell(Player player, String text) { player.sendMessage(Component.text(text, NamedTextColor.AQUA)); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!OrbitalAccess.allowed(sender) || args.length == 0) return List.of();
        Stream<String> options;
        if (args.length == 1) options = Stream.concat(Stream.of("give", "preview", "cancel", "help"), Arrays.stream(StrikePattern.values()).map(StrikePattern::id));
        else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("preview"))) options = Arrays.stream(StrikePattern.values()).map(StrikePattern::id);
        else if (args[0].equalsIgnoreCase("give") && args.length == 4) options = Stream.of("true", "false");
        else if ((args[0].equalsIgnoreCase("give") && args.length == 5) || args.length == 6) options = Stream.of("simultaneous", "inward", "outward");
        else options = Stream.empty();
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.filter(v -> v.startsWith(prefix)).toList();
    }
    @Override public void close() { casts.values().forEach(c -> c.hook.remove()); casts.clear(); }
}

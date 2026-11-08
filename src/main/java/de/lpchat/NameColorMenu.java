package de.lpchat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class NameColorMenu implements TabExecutor, Listener {
    private static final List<String> COLORS = List.copyOf(NamedTextColor.NAMES.keys());
    private final LPChatPlugin plugin;
    private enum Mode { SOLID, START, END }

    NameColorMenu(LPChatPlugin plugin) { this.plugin = plugin; }

    private static final class Holder implements InventoryHolder {
        private final Inventory inventory;
        private final Mode mode;
        private final TextColor start;
        Holder(Mode mode, TextColor start) {
            this.mode = mode;
            this.start = start;
            String title = switch (mode) {
                case SOLID -> "Namensfarbe";
                case START -> "Verlauf: Startfarbe";
                case END -> "Verlauf: Endfarbe";
            };
            inventory = Bukkit.createInventory(this, 27, Component.text(title));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
            return true;
        }
        if (!allowed(player, false)) return true;
        if (args.length == 0) {
            open(player, Mode.SOLID, null);
        } else if (args[0].equalsIgnoreCase("gradient")) {
            if (!allowed(player, true)) return true;
            if (args.length == 1) open(player, Mode.START, null);
            else if (args.length == 3) {
                TextColor start = NameStyle.color(args[1]);
                TextColor end = NameStyle.color(args[2]);
                if (start == null || end == null) invalidColor(player);
                else save(player, new NameStyle(start, end));
            } else usage(player);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            plugin.selectNameStyle(player, null);
            player.sendMessage(Component.text("Deine Namensfarbe wurde zurueckgesetzt.", NamedTextColor.GREEN));
        } else if (args.length == 1) {
            TextColor color = NameStyle.color(args[0]);
            if (color == null) invalidColor(player);
            else save(player, new NameStyle(color, color));
        } else usage(player);
        return true;
    }

    private boolean allowed(Player player, boolean gradient) {
        if (player.hasPermission("namecolor.use") && (!gradient || player.hasPermission("namecolor.gradient"))) return true;
        player.sendMessage(Component.text("Dafuer hast du keine Berechtigung.", NamedTextColor.RED));
        return false;
    }

    private void save(Player player, NameStyle style) {
        if (!allowed(player, style.gradient())) return;
        plugin.selectNameStyle(player, style);
        player.sendMessage(Component.text("Dein Name im Chat: ", NamedTextColor.GRAY)
                .append(style.nameAndSeparator(player.getName())));
    }

    private void invalidColor(Player player) {
        player.sendMessage(Component.text("Unbekannte Farbe. Nutze z.B. red, aqua oder #ff5500.", NamedTextColor.RED));
    }

    private void usage(Player player) {
        player.sendMessage(Component.text("/namecolor [Farbe|gradient [Startfarbe Endfarbe]|reset]", NamedTextColor.GRAY));
    }

    private static ItemStack item(Material material, Component title) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private void open(Player player, Mode mode, TextColor start) {
        if (!allowed(player, mode != Mode.SOLID)) return;
        Holder holder = new Holder(mode, start);
        for (int index = 0; index < COLORS.size(); index++) {
            String name = COLORS.get(index);
            TextColor color = NameStyle.color(name);
            Component title = mode == Mode.END
                    ? new NameStyle(start, color).nameAndSeparator(player.getName())
                    : Component.text(name, color);
            holder.inventory.setItem(index, item(Material.PAPER, title));
        }
        holder.inventory.setItem(22, item(Material.MILK_BUCKET, Component.text("Zuruecksetzen", NamedTextColor.WHITE)));
        if (mode == Mode.SOLID && player.hasPermission("namecolor.gradient")) {
            holder.inventory.setItem(24, item(Material.PRISMARINE_CRYSTALS, Component.text("Farbverlauf waehlen", NamedTextColor.AQUA)));
        }
        player.openInventory(holder.inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot != 22 && slot != 24 && (slot < 0 || slot >= COLORS.size())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != top) return;
            if (!allowed(player, holder.mode != Mode.SOLID)) return;
            if (slot == 22) {
                plugin.selectNameStyle(player, null);
                player.sendMessage(Component.text("Deine Namensfarbe wurde zurueckgesetzt.", NamedTextColor.GREEN));
                player.closeInventory();
            } else if (slot == 24) {
                if (holder.mode == Mode.SOLID) open(player, Mode.START, null);
            } else {
                TextColor selected = NameStyle.color(COLORS.get(slot));
                if (holder.mode == Mode.START) open(player, Mode.END, selected);
                else {
                    save(player, new NameStyle(holder.mode == Mode.END ? holder.start : selected, selected));
                    player.closeInventory();
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("namecolor.use") || args.length == 0) return List.of();
        List<String> candidates = new ArrayList<>(COLORS);
        if (args.length == 1) {
            candidates.add("reset");
            if (sender.hasPermission("namecolor.gradient")) candidates.add("gradient");
        } else if (args.length > 3 || !args[0].equalsIgnoreCase("gradient") || !sender.hasPermission("namecolor.gradient")) {
            return List.of();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}

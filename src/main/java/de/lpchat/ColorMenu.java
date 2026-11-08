package de.lpchat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

final class ColorMenu implements TabExecutor, Listener {
    private final LPChatPlugin plugin;

    ColorMenu(LPChatPlugin plugin) { this.plugin = plugin; }

    private static final class Holder implements InventoryHolder {
        private final Inventory inventory;
        Holder(int count, int total) {
            inventory = Bukkit.createInventory(this, 27, Component.text("Chatfarbe (" + count + "/" + total + ")"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
            return true;
        }
        if (!player.hasPermission("chatcolor.use")) {
            player.sendMessage(Component.text("Dafuer hast du keine Berechtigung.", NamedTextColor.RED));
        } else if (args.length == 0) {
            open(player);
        } else if (args.length == 1) {
            choose(player, args[0].toLowerCase(Locale.ROOT));
        } else {
            player.sendMessage(Component.text("/chatcolor [Farbe|reset]", NamedTextColor.GRAY));
        }
        return true;
    }

    private void open(Player player) {
        int count = plugin.unlocked(player);
        Holder holder = new Holder(count, plugin.colors().size());
        for (int index = 0; index < plugin.colors().size(); index++) {
            String name = plugin.colors().get(index);
            boolean allowed = index < count;
            boolean selected = name.equals(plugin.selected(player));
            ItemStack item = new ItemStack(allowed ? Material.PAPER : Material.BARRIER);
            var meta = item.getItemMeta();
            meta.displayName(Component.text(name + (selected ? " (ausgewaehlt)" : ""), NamedTextColor.NAMES.value(name))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(allowed ? "Klicken zum Auswaehlen" : "Benoetigt chatcolor." + (index + 1),
                    allowed ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
            holder.inventory.setItem(index, item);
        }
        ItemStack reset = new ItemStack(Material.MILK_BUCKET);
        var meta = reset.getItemMeta();
        meta.displayName(Component.text("Standardfarbe verwenden", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        reset.setItemMeta(meta);
        holder.inventory.setItem(22, reset);
        player.openInventory(holder.inventory);
    }

    private void choose(Player player, String name) {
        if (!player.hasPermission("chatcolor.use")) {
            player.sendMessage(Component.text("Dafuer hast du keine Berechtigung.", NamedTextColor.RED));
            return;
        }
        if (name.equals("reset")) {
            plugin.select(player, null);
            player.sendMessage(Component.text("Deine Chatfarbe wurde zurueckgesetzt.", NamedTextColor.GREEN));
            return;
        }
        if (!plugin.colors().contains(name)) {
            player.sendMessage(Component.text("Unbekannte Farbe. Oeffne /chatcolor.", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/chatcolor")));
            return;
        }
        if (!ColorPolicy.allowed(name, plugin.colors(), plugin.unlocked(player))) {
            player.sendMessage(Component.text("Diese Farbe ist fuer dich noch gesperrt.", NamedTextColor.RED));
            return;
        }
        plugin.select(player, name);
        player.sendMessage(Component.text("Deine Chatfarbe: " + name, NamedTextColor.NAMES.value(name)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true); // Also block shift-click, hotbar swaps and double-click collection.
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        String color = slot == 22 ? "reset" : slot >= 0 && slot < plugin.colors().size() ? plugin.colors().get(slot) : null;
        if (color == null) return;
        // Opening/closing inventories inside InventoryClickEvent is unsafe; defer by one tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != event.getView().getTopInventory()) return;
            choose(player, color); // Re-check rights at selection time.
            open(player);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1 || !player.hasPermission("chatcolor.use")) return List.of();
        List<String> available = new ArrayList<>(plugin.colors().subList(0, plugin.unlocked(player)));
        available.add("reset");
        String start = args[0].toLowerCase(Locale.ROOT);
        return available.stream().filter(value -> value.startsWith(start)).toList();
    }
}

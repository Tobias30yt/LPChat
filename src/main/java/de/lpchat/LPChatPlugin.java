package de.lpchat;

import java.util.List;
import java.util.Objects;
import net.luckperms.api.LuckPerms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.ChatEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class LPChatPlugin extends JavaPlugin implements Listener {
    private LuckPerms luckPerms;
    private TeamHook teams;
    private NamespacedKey selectionKey;
    private NamespacedKey nameStyleKey;
    private List<String> colors;
    private NamedTextColor defaultColor;
    private PaperAttributeSwapping attributeSwapping;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        colors = List.copyOf(getConfig().getStringList("color-order"));
        defaultColor = NamedTextColor.NAMES.value(getConfig().getString("default-chat-color", "white"));
        if (colors.isEmpty() || colors.size() > 16 || colors.stream().distinct().count() != colors.size()
                || colors.stream().anyMatch(c -> NamedTextColor.NAMES.value(c) == null) || defaultColor == null) {
            getLogger().severe("Ungueltige Farbliste oder Standardfarbe in config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            getLogger().severe("LuckPerms-API ist nicht verfuegbar.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        selectionKey = new NamespacedKey(this, "chatcolor");
        nameStyleKey = new NamespacedKey(this, "namecolor");
        teams = new TeamHook(this);
        ColorMenu menu = new ColorMenu(this);
        Objects.requireNonNull(getCommand("chatcolor")).setExecutor(menu);
        Objects.requireNonNull(getCommand("chatcolor")).setTabCompleter(menu);
        getServer().getPluginManager().registerEvents(menu, this);
        NameColorMenu nameMenu = new NameColorMenu(this);
        Objects.requireNonNull(getCommand("namecolor")).setExecutor(nameMenu);
        Objects.requireNonNull(getCommand("namecolor")).setTabCompleter(nameMenu);
        getServer().getPluginManager().registerEvents(nameMenu, this);
        getServer().getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("attribute-swapping.enabled", true)) {
            try {
                attributeSwapping = PaperAttributeSwapping.enable();
                getLogger().info("Attribute-Swapping aktiviert (Paper equipment updates = false).");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                getLogger().warning("Attribute-Swapping konnte nicht aktiviert werden: " + exception
                        + ". Alternativ in config/paper-global.yml unter unsupported-settings "
                        + "update-equipment-on-player-actions: false setzen und den Server neu starten.");
            }
        }
        if (getConfig().getBoolean("teams.enabled")
                && !getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("Team-Anzeige benoetigt PlaceholderAPI und UltimateTeams.");
        }
    }

    @Override
    public void onDisable() {
        if (attributeSwapping == null) return;
        try {
            attributeSwapping.close();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            getLogger().warning("Paper-Einstellung konnte nicht wiederhergestellt werden: " + exception);
        } finally {
            attributeSwapping = null;
        }
    }

    // ChatEvent runs on the main thread: permissions, PDC and PAPI are read safely.
    // Only install a renderer; preserve cancellation, recipients and team-chat routing.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(ChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String prefix = luckPerms.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();
        Component header = ChatFormat.header(teams.team(player), ChatFormat.prefix(prefix,
                "minimessage".equalsIgnoreCase(getConfig().getString("prefix-format"))), player.getName(), nameStyle(player));
        NamedTextColor color = ColorPolicy.effective(selected(player), colors, unlocked(player), defaultColor);
        event.renderer(ChatRenderer.viewerUnaware((source, displayName, message) ->
                ChatFormat.render(header, message, color)));
    }

    List<String> colors() { return colors; }

    NameStyle nameStyle(Player player) {
        if (!player.hasPermission("namecolor.use")) return NameStyle.DEFAULT;
        NameStyle style = NameStyle.fromStored(player.getPersistentDataContainer().get(nameStyleKey, PersistentDataType.STRING));
        return style.gradient() && !player.hasPermission("namecolor.gradient") ? NameStyle.DEFAULT : style;
    }

    void selectNameStyle(Player player, NameStyle style) {
        if (style == null) player.getPersistentDataContainer().remove(nameStyleKey);
        else player.getPersistentDataContainer().set(nameStyleKey, PersistentDataType.STRING, style.stored());
    }

    int unlocked(Player player) { return ColorPolicy.unlocked(player::hasPermission, colors.size()); }

    String selected(Player player) {
        return player.getPersistentDataContainer().get(selectionKey, PersistentDataType.STRING);
    }

    void select(Player player, String color) {
        if (color == null) player.getPersistentDataContainer().remove(selectionKey);
        else player.getPersistentDataContainer().set(selectionKey, PersistentDataType.STRING, color);
    }
}

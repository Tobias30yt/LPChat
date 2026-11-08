package de.lpchat;

import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

final class TeamHook {
    private final LPChatPlugin plugin;
    private boolean warned;

    TeamHook(LPChatPlugin plugin) { this.plugin = plugin; }

    Component team(Player player) {
        if (!plugin.getConfig().getBoolean("teams.enabled", true)
                || !plugin.getServer().getPluginManager().isPluginEnabled("UltimateTeams")
                || !plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return Component.empty();
        }
        try {
            String memberPlaceholder = plugin.getConfig().getString("teams.membership-placeholder",
                    "%ultimateteams_is_in_team%");
            String membership = PlaceholderAPI.setPlaceholders(player, memberPlaceholder);
            if ("false".equalsIgnoreCase(membership.strip())) return Component.empty();
            String placeholder = plugin.getConfig().getString("teams.name-placeholder", "%ultimateteams_team_name%");
            String name = PlaceholderAPI.setPlaceholders(player, placeholder).strip();
            if (name.equals(placeholder)) return Component.empty();
            Component component = ChatFormat.legacy(name);
            String plain = ChatFormat.plain(component).strip();
            List<String> empty = plugin.getConfig().getStringList("teams.empty-values");
            if (plain.isBlank() || empty.stream().anyMatch(plain::equalsIgnoreCase)) return Component.empty();
            return component;
        } catch (RuntimeException exception) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("UltimateTeams-Platzhalter nicht lesbar: " + exception.getMessage());
            }
            return Component.empty();
        }
    }
}

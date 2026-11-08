package de.lpchat.orbital;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OrbitalAccess {
    private OrbitalAccess() {}
    public static boolean allowed(CommandSender sender) {
        return sender instanceof Player player && "Tobias30".equalsIgnoreCase(player.getName());
    }
    public static void require(CommandSender sender) {
        if (!allowed(sender)) throw new IllegalArgumentException("Orbital ist ausschliesslich fuer Tobias30 freigeschaltet.");
    }
}

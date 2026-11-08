package de.lpchat.orbital;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrbitalAccessTest {
    @Test void onlyTobias30IsAllowedRegardlessOfOpOrPermissions() {
        Player tobias = mock(Player.class), other = mock(Player.class);
        when(tobias.getName()).thenReturn("tobias30");
        when(other.getName()).thenReturn("SomeoneElse");
        when(other.isOp()).thenReturn(true);
        when(other.hasPermission(anyString())).thenReturn(true);
        assertTrue(OrbitalAccess.allowed(tobias));
        assertFalse(OrbitalAccess.allowed(other));
        assertFalse(OrbitalAccess.allowed(mock(ConsoleCommandSender.class)));
        assertFalse(OrbitalAccess.allowed(null));
    }
    @Test void commandAndEngineBothBlockOtherPlayersBeforeAnyWorldAccess() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("LPChat");
        when(plugin.namespace()).thenReturn("lpchat");
        Player other = mock(Player.class);
        when(other.getName()).thenReturn("OtherAdmin");
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenReturn(mock(BukkitTask.class));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        try (OrbitalEngine actual = new OrbitalEngine(plugin)) {
            assertThrows(IllegalArgumentException.class, () -> actual.fire(other, null, StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            assertThrows(IllegalArgumentException.class, () -> actual.preview(other, null, StrikePattern.NUKE, 5));
        }
        verify(other, never()).getWorld();
    }
}

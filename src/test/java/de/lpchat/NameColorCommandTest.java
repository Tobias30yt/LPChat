package de.lpchat;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NameColorCommandTest {
    private LPChatPlugin plugin;
    private Player player;
    private NameColorMenu command;

    @BeforeEach void setup() {
        plugin = mock(LPChatPlugin.class);
        player = mock(Player.class);
        when(player.hasPermission("namecolor.use")).thenReturn(true);
        when(player.hasPermission("namecolor.gradient")).thenReturn(true);
        when(player.getName()).thenReturn("Tobias30");
        command = new NameColorMenu(plugin);
    }

    private void run(String... args) { command.onCommand(player, null, "namecolor", args); }

    @Test void savesSolidAndGradientSelections() {
        run("red");
        verify(plugin).selectNameStyle(player, new NameStyle(NamedTextColor.RED, NamedTextColor.RED));
        run("gradient", "#123456", "#abcdef");
        verify(plugin).selectNameStyle(player, new NameStyle(NameStyle.color("#123456"), NameStyle.color("#abcdef")));
    }

    @Test void resetRemovesSavedSelection() {
        run("reset");
        verify(plugin).selectNameStyle(player, null);
    }

    @Test void invalidInputNeverChangesSelection() {
        run("bogus");
        run("gradient", "red");
        run("gradient", "red", "#xyzxyz");
        verifyNoInteractions(plugin);
    }

    @Test void permissionsAreEnforcedAndHiddenFromCompletion() {
        when(player.hasPermission("namecolor.gradient")).thenReturn(false);
        run("gradient", "red", "blue");
        verifyNoInteractions(plugin);
        assertFalse(command.onTabComplete(player, null, "namecolor", new String[]{""}).contains("gradient"));
        when(player.hasPermission("namecolor.use")).thenReturn(false);
        run("red");
        run("reset");
        verifyNoInteractions(plugin);
        assertTrue(command.onTabComplete(player, null, "namecolor", new String[]{""}).isEmpty());
    }
}

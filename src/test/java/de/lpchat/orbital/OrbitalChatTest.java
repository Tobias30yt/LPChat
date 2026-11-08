package de.lpchat.orbital;
import io.papermc.paper.event.player.ChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class OrbitalChatTest {
    @Test void foreignOperatorsCannotUseChatCommandAndItIsNotBroadcast() {
        JavaPlugin plugin=mock(JavaPlugin.class); when(plugin.namespace()).thenReturn("lpchat");
        var command=new OrbitalCommand(plugin,mock(OrbitalEngine.class));
        Player player=mock(Player.class); when(player.getName()).thenReturn("OtherAdmin"); when(player.isOp()).thenReturn(true);
        ChatEvent event=mock(ChatEvent.class); when(event.getPlayer()).thenReturn(player); when(event.message()).thenReturn(Component.text(".orbital give"));
        command.onChat(event);
        verify(event).setCancelled(true); verify(player,never()).getInventory();
        command.completions(player); verify(player,never()).addCustomChatCompletions(anyCollection());
    }
    @Test void tobiasReceivesCompletionAndOtherChatIsUntouched() {
        JavaPlugin plugin=mock(JavaPlugin.class); when(plugin.namespace()).thenReturn("lpchat");
        var command=new OrbitalCommand(plugin,mock(OrbitalEngine.class));
        Player player=mock(Player.class); when(player.getName()).thenReturn("Tobias30");
        command.completions(player); verify(player).addCustomChatCompletions(anyCollection());
        ChatEvent event=mock(ChatEvent.class); when(event.message()).thenReturn(Component.text("hello .orbital give"));
        command.onChat(event); verify(event,never()).setCancelled(anyBoolean());
        assertTrue(OrbitalCommand.isCommand(".orbital give"));
        assertFalse(OrbitalCommand.isCommand(".orbitals"));
    }
}

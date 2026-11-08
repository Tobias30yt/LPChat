package de.lpchat;

import dev.xf3d3.ultimateteams.UltimateTeams;
import dev.xf3d3.ultimateteams.config.Settings;
import dev.xf3d3.ultimateteams.hooks.PapiExpansion;
import dev.xf3d3.ultimateteams.listeners.PlayerChatEvent;
import dev.xf3d3.ultimateteams.models.Team;
import dev.xf3d3.ultimateteams.utils.TeamsStorage;
import dev.xf3d3.ultimateteams.utils.UsersStorage;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.ChatEvent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.manager.LocalExpansionManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Executes the released UltimateTeams expansion and chat listener with simulated server/storage. */
class UltimateTeamsIntegrationTest {
    private LPChatPlugin plugin;
    private UltimateTeams ultimateTeams;
    private TeamsStorage storage;
    private Player player;
    private TeamHook hook;
    private LocalExpansionManager expansions;
    private MockedStatic<PlaceholderAPIPlugin> papiInstance;
    private MockedStatic<Bukkit> bukkit;
    private Settings settings;
    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @BeforeEach void setup() {
        plugin = mock(LPChatPlugin.class, RETURNS_DEEP_STUBS);
        var config = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LPChat-test"));
        when(plugin.getServer().getPluginManager().isPluginEnabled("UltimateTeams")).thenReturn(true);
        when(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")).thenReturn(true);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Tobias30");

        ultimateTeams = mock(UltimateTeams.class);
        storage = mock(TeamsStorage.class);
        when(ultimateTeams.getTeamStorageUtil()).thenReturn(storage);
        when(storage.findTeamByMember(playerId)).thenReturn(Optional.empty());
        settings = mock(Settings.class, RETURNS_DEEP_STUBS);
        when(ultimateTeams.getSettings()).thenReturn(settings);
        when(settings.getPlaceholder().getNotInATeam()).thenReturn("&cNicht in einem Team!");
        expansions = mock(LocalExpansionManager.class);
        when(expansions.getExpansion("ultimateteams")).thenReturn(new PapiExpansion(ultimateTeams));
        Server server = mock(Server.class);
        when(server.getBukkitVersion()).thenReturn("26.2");
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        PlaceholderAPIPlugin papi = mock(PlaceholderAPIPlugin.class);
        when(papi.getLocalExpansionManager()).thenReturn(expansions);
        papiInstance = mockStatic(PlaceholderAPIPlugin.class);
        papiInstance.when(PlaceholderAPIPlugin::getInstance).thenReturn(papi);
        hook = new TeamHook(plugin);
    }

    @AfterEach void cleanup() {
        if (papiInstance != null) papiInstance.close();
        if (bukkit != null) bukkit.close();
    }

    private Team join(String name) {
        Team team = Team.builder().name(name).members(Map.of(playerId, 3)).build();
        when(storage.findTeamByMember(playerId)).thenReturn(Optional.of(team));
        return team;
    }

    @Test void actualExpansionResolvesTeamColorsAndCompleteChatFormat() {
        join("&aBuilders");
        Component team = hook.team(player);
        assertEquals("Builders", ChatFormat.plain(team));
        assertEquals(NamedTextColor.GREEN, team.color());
        Component header = ChatFormat.header(team, ChatFormat.prefix("&4Supporter", false), "Tobias30",
                new NameStyle(NamedTextColor.AQUA, NamedTextColor.BLUE));
        assertEquals("(Builders) Supporter Tobias30 >>  Hallo", ChatFormat.plain(
                ChatFormat.render(header, Component.text("Hallo"), NamedTextColor.WHITE)));
    }

    @Test void realTeamsNamedLikeEmptySentinelsRemainVisible() {
        for (String name : new String[]{"None", "Kein Team", "N/A"}) {
            join(name);
            assertEquals(name, ChatFormat.plain(hook.team(player)));
        }
    }

    @Test void noTeamHidesEvenCustomNoTeamMessagesAndLeaveIsImmediate() {
        assertEquals("", ChatFormat.plain(hook.team(player)));
        join("Builders");
        assertEquals("Builders", ChatFormat.plain(hook.team(player)));
        when(storage.findTeamByMember(playerId)).thenReturn(Optional.empty());
        assertEquals("", ChatFormat.plain(hook.team(player)));
    }

    @Test void unavailableDependenciesAndExpansionDoNotLeakPlaceholders() {
        join("Builders");
        when(expansions.getExpansion("ultimateteams")).thenReturn(null);
        assertEquals("", ChatFormat.plain(hook.team(player)));
        when(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")).thenReturn(false);
        assertEquals("", ChatFormat.plain(hook.team(player)));
        when(plugin.getServer().getPluginManager().isPluginEnabled("UltimateTeams")).thenReturn(false);
        assertEquals("", ChatFormat.plain(hook.team(player)));
    }

    @Test void teamAndAllyChannelsCancelPublicDelivery() {
        Team team = mock(Team.class);
        when(team.getName()).thenReturn("Builders");
        when(team.getRelations(ultimateTeams)).thenReturn(Map.of());
        when(storage.findTeamByMember(playerId)).thenReturn(Optional.of(team));
        UsersStorage users = mock(UsersStorage.class);
        when(ultimateTeams.getUsersStorageUtil()).thenReturn(users);
        var channels = new ConcurrentHashMap<UUID, UsersStorage.ChatType>();
        when(users.getChatPlayers()).thenReturn(channels);
        when(settings.getTeam().getChat().getPrefix()).thenReturn("[Team] %PLAYER%");
        when(settings.getTeam().getAllies().getChat().getPrefix()).thenReturn("[Ally] %PLAYER%");
        when(ultimateTeams.replacePlaceholders(eq(player), anyString())).thenAnswer(call -> call.getArgument(1));
        when(ultimateTeams.getMessageBroker()).thenReturn(Optional.empty());
        var listener = new PlayerChatEvent(ultimateTeams);
        for (var channel : new UsersStorage.ChatType[]{UsersStorage.ChatType.TEAM_CHAT, UsersStorage.ChatType.ALLY_CHAT}) {
            channels.put(playerId, channel);
            Set<Audience> recipients = new HashSet<>(Set.of(mock(Audience.class)));
            Component message = Component.text("Nur intern");
            var event = new AsyncChatEvent(true, player, recipients, ChatRenderer.defaultRenderer(), message,
                    message, SignedMessage.system("Nur intern", message));
            listener.onPlayerChat(event);
            assertTrue(event.isCancelled());
            assertTrue(event.viewers().isEmpty());
            // Paper forwards the cancellation to its synchronous event; LPChat must leave it alone.
            ChatRenderer original = event.renderer();
            var sync = new ChatEvent(player, event.viewers(), original, message, message, event.signedMessage());
            sync.setCancelled(event.isCancelled());
            doCallRealMethod().when(plugin).onChat(sync);
            plugin.onChat(sync);
            assertTrue(sync.isCancelled());
            assertSame(original, sync.renderer());
            assertTrue(sync.viewers().isEmpty());
        }
        verify(team, times(2)).sendTeamMessage(any(Component.class));
    }
}

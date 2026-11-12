package de.lpchat.orbital;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrbitalEngineTest {
    JavaPlugin plugin;
    Player player;
    World world;
    YamlConfiguration spigot;
    List<TNTPrimed> spawned;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        Server.Spigot serverSpigot = mock(Server.Spigot.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.spigot()).thenReturn(serverSpigot);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenReturn(mock(BukkitTask.class));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        spigot = new YamlConfiguration();
        spigot.set("world-settings.default.max-tnt-per-tick", -1);
        when(serverSpigot.getSpigotConfig()).thenReturn(spigot);
        world = mock(World.class);
        WorldBorder border = mock(WorldBorder.class);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any())).thenReturn(true);
        when(world.getName()).thenReturn("test");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        player = mock(Player.class);
        when(player.getName()).thenReturn("Tobias30");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        spawned = new ArrayList<>();
        when(world.spawn(any(Location.class), eq(TNTPrimed.class), any(Consumer.class))).thenAnswer(call -> {
            TNTPrimed tnt = mock(TNTPrimed.class);
            when(tnt.getWorld()).thenReturn(world);
            when(tnt.getUniqueId()).thenReturn(UUID.randomUUID());
            ((Consumer<TNTPrimed>) call.getArgument(2)).accept(tnt);
            spawned.add(tnt);
            return tnt;
        });
    }
    @Test void realTntReceivesSourceFuseAndVelocityAndCooldownBlocksSecondShot() {
        try (OrbitalEngine engine = new OrbitalEngine(plugin)) {
            assertEquals(181, engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            for (TNTPrimed tnt : spawned) {
                verify(tnt).setSource(player);
                verify(tnt).setFuseTicks(50);
                verify(tnt).setGravity(true);
                verify(tnt).setPersistent(false);
                verify(tnt).setYield(4);
                verify(tnt).setVelocity(any());
            }
            assertEquals(181, engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            assertEquals(362, engine.cancelAll());
            for (TNTPrimed tnt : spawned) verify(tnt).remove();
            assertEquals(0, engine.cancelAll());
        }
    }
    @Test void unloadedChunksTickLimitAndWorldBorderRejectBeforeSpawning() {
        try (OrbitalEngine engine = new OrbitalEngine(plugin)) {
            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
            spigot.set("world-settings.default.max-tnt-per-tick", 100);
            assertThrows(IllegalArgumentException.class, () -> engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            spigot.set("world-settings.default.max-tnt-per-tick", -1);
            when(world.getWorldBorder().isInside(any())).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS));
            assertTrue(spawned.isEmpty());
        }
    }
    @Test void drillAddsSolidColumnButPreservesBedrockAndCancellation() {
        var block = mock(org.bukkit.block.Block.class);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        try (OrbitalEngine engine = new OrbitalEngine(plugin)) {
            engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.STAB, 1, StrikePlan.Order.SIMULTANEOUS);
            for (int i = 0; i < 3; i++) {
                var event = mock(org.bukkit.event.entity.EntityExplodeEvent.class);
                when(event.getEntity()).thenReturn(spawned.get(i));
                var blocks = new ArrayList<org.bukkit.block.Block>();
                when(event.blockList()).thenReturn(blocks);
                when(block.getType()).thenReturn(i == 0 ? org.bukkit.Material.BEDROCK : org.bukkit.Material.DEEPSLATE);
                when(event.isCancelled()).thenReturn(i == 2);
                engine.onExplosion(event);
                assertEquals(i == 1 ? 1 : 0, blocks.size());
            }
        }
    }
    @Test void disableRemovesOutstandingPayloadAndPreviewDoesNotSpawnTnt() {
        OrbitalEngine engine = new OrbitalEngine(plugin);
        engine.preview(player, new Location(world, 0, 64, 0), StrikePattern.STAB, 1);
        assertTrue(spawned.isEmpty());
        engine.fire(player, new Location(world, 0, 64, 0), StrikePattern.STAB, 1, StrikePlan.Order.SIMULTANEOUS);
        engine.close();
        for (TNTPrimed tnt : spawned) verify(tnt).remove();
    }
}

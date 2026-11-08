package de.lpchat.orbital;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrbitalRodTest {
    JavaPlugin plugin;
    OrbitalCommand command;
    OrbitalEngine engine;
    Player player;
    PlayerInventory inventory;
    FishHook hook;
    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("lpchat");
        engine = mock(OrbitalEngine.class);
        command = new OrbitalCommand(plugin, engine);
        player = mock(Player.class);
        when(player.getName()).thenReturn("Tobias30");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack rod = mock(ItemStack.class);
        when(rod.getType()).thenReturn(Material.FISHING_ROD);
        when(rod.hasItemMeta()).thenReturn(true);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(rod.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenReturn("nuke:5:SIMULTANEOUS:true:" + UUID.randomUUID());
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(rod);
        hook = mock(FishHook.class);
        when(hook.getUniqueId()).thenReturn(UUID.randomUUID());
        World world = mock(World.class);
        when(hook.getLocation()).thenReturn(new Location(world, 10, 64, 10));
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 65, 0));
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getBlockAt(any(Location.class))).thenReturn(mock(org.bukkit.block.Block.class));
    }
    private PlayerFishEvent event(PlayerFishEvent.State state) {
        PlayerFishEvent event = mock(PlayerFishEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getHook()).thenReturn(hook);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getState()).thenReturn(state);
        return event;
    }
    @Test void otherOperatorCannotUseBorrowedRod() {
        when(player.getName()).thenReturn("OtherAdmin");
        when(player.isOp()).thenReturn(true);
        var cast = event(PlayerFishEvent.State.FISHING);
        command.onFish(cast);
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        verify(cast, never()).setCancelled(true);
        verifyNoInteractions(engine);
    }
    @Test void tobiasCanFireOnceAndNormalRodBreaks() {
        command.onFish(event(PlayerFishEvent.State.FISHING));
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        verify(engine, times(1)).fire(eq(player), any(Location.class), eq(StrikePattern.NUKE), eq(5), eq(StrikePlan.Order.SIMULTANEOUS));
        verify(inventory).setItem(eq(EquipmentSlot.HAND), isNull());
        verify(hook).remove();
    }
    @Test void switchingRodsCannotTriggerSavedShot() {
        command.onFish(event(PlayerFishEvent.State.FISHING));
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(null);
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        verifyNoInteractions(engine);
    }
    @Test void stabWaitsSixtyTicksBeforeSpawning() {
        var server = mock(org.bukkit.Server.class);
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(60L))).thenReturn(mock(org.bukkit.scheduler.BukkitTask.class));
        when(player.isSneaking()).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        command.onFish(event(PlayerFishEvent.State.FISHING));
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        verifyNoInteractions(engine);
        var callback = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), callback.capture(), eq(60L));
        callback.getValue().run();
        verify(engine).fire(eq(player), any(Location.class), eq(StrikePattern.STAB), eq(1), eq(StrikePlan.Order.SIMULTANEOUS));
    }
    @Test void cancelledFishingIsPreserved() {
        var cast = event(PlayerFishEvent.State.FISHING);
        when(cast.isCancelled()).thenReturn(true);
        command.onFish(cast);
        command.onFish(event(PlayerFishEvent.State.REEL_IN));
        verifyNoInteractions(engine);
    }
}

package de.lpchat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.*;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class OwnerIdentityTest {
    @Test void operatorAccessUsesUuidEvenAfterRename() {
        var owner = mock(org.bukkit.entity.Player.class);
        when(owner.getUniqueId()).thenReturn(OwnerIdentity.OWNER);
        when(owner.getName()).thenReturn("NebelFuchs");
        OwnerIdentity.ensureOperator(owner);
        verify(owner).setOp(true);
        when(owner.isOp()).thenReturn(true);
        OwnerIdentity.ensureOperator(owner);
        verify(owner, times(1)).setOp(true);
    }
    @Test void matchingNameWithOtherUuidNeverGetsOperatorAccess() {
        var other = mock(org.bukkit.entity.Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        when(other.getName()).thenReturn("Tobias30");
        OwnerIdentity.ensureOperator(other);
        when(other.getName()).thenReturn("NebelFuchs");
        OwnerIdentity.ensureOperator(other);
        verify(other, never()).setOp(anyBoolean());
    }
    @Test @SuppressWarnings({"unchecked","rawtypes"}) void onlyOwnerProfileBanIsRemoved() {
        JavaPlugin plugin=mock(JavaPlugin.class); Server server=mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        ProfileBanList bans=mock(ProfileBanList.class);
        when(server.getBanList(BanList.Type.PROFILE)).thenReturn(bans);
        BanEntry owner=mock(BanEntry.class), other=mock(BanEntry.class);
        PlayerProfile ownerProfile=mock(PlayerProfile.class), otherProfile=mock(PlayerProfile.class);
        when(ownerProfile.getUniqueId()).thenReturn(OwnerIdentity.OWNER);
        when(otherProfile.getUniqueId()).thenReturn(UUID.randomUUID());
        when(owner.getBanTarget()).thenReturn(ownerProfile); when(other.getBanTarget()).thenReturn(otherProfile);
        when(bans.getEntries()).thenReturn(Set.of(owner,other));
        new OwnerIdentity(plugin).pardon();
        verify(owner).remove(); verify(other,never()).remove();
        verify(server,never()).getBanList(BanList.Type.IP);
    }
    @Test void preLoginChangesOnlyProfileNameAndPreservesOtherDenials() {
        JavaPlugin plugin=mock(JavaPlugin.class); Server server=mock(Server.class);
        BukkitScheduler scheduler=mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.callSyncMethod(eq(plugin),any())).thenReturn(CompletableFuture.completedFuture(null));
        AsyncPlayerPreLoginEvent event=mock(AsyncPlayerPreLoginEvent.class);
        when(event.getUniqueId()).thenReturn(OwnerIdentity.OWNER);
        PlayerProfile original=mock(PlayerProfile.class), copy=mock(PlayerProfile.class);
        when(event.getPlayerProfile()).thenReturn(original); when(original.clone()).thenReturn(copy);
        new OwnerIdentity(plugin).onPreLogin(event);
        verify(copy).setName("NebelFuchs"); verify(copy,never()).setId(any());
        verify(copy,never()).clearProperties(); verify(event).setPlayerProfile(copy);
        verify(event,never()).allow(); verify(event,never()).setLoginResult(any());
    }
    @Test void otherUuidCannotAcquireOwnerAlias() {
        JavaPlugin plugin=mock(JavaPlugin.class); AsyncPlayerPreLoginEvent event=mock(AsyncPlayerPreLoginEvent.class);
        when(event.getUniqueId()).thenReturn(UUID.randomUUID()); when(event.getName()).thenReturn("Tobias30");
        new OwnerIdentity(plugin).onPreLogin(event);
        verify(event,never()).setPlayerProfile(any()); verifyNoInteractions(plugin);
        assertFalse(OwnerIdentity.isOwner(null));
    }
}

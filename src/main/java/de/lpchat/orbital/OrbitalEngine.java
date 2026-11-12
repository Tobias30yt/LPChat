package de.lpchat.orbital;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class OrbitalEngine implements AutoCloseable, org.bukkit.event.Listener {
    private record Active(List<TNTPrimed> entities, long expires) {}
    private final JavaPlugin plugin;
    private final Map<UUID, Location> drillBlocks = new HashMap<>();
    private final List<Active> active = new ArrayList<>();
    private final BukkitTask cleanup;

    public OrbitalEngine(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cleanup = plugin.getServer().getScheduler().runTaskTimer(plugin, this::prune, 1, 1);
    }

    public int fire(Player source, Location target, StrikePattern pattern, int size, StrikePlan.Order order) {
        OrbitalAccess.require(source);
        prune();
        long now = System.nanoTime();
        int maxActive = 2;
        if (active.size() >= maxActive) throw new IllegalArgumentException("Es laufen bereits zu viele Orbital-Schuesse.");
        int maxTnt = 1024;
        World world = target.getWorld();
        List<StrikePlan.Payload> plan = StrikePlan.create(pattern, size, order, target.getYaw(),
                target.getY(), world.getMinHeight(), world.getMaxHeight(), maxTnt);
        validateArea(target, pattern.points(size, target.getYaw()));
        checkTickLimit(world, plan.size());
        List<TNTPrimed> spawned = new ArrayList<>();
        final float power = 4;
        try {
            for (StrikePlan.Payload p : plan) {
                Location start = new Location(world, target.getX() + p.x(), p.y(), target.getZ() + p.z());
                TNTPrimed tnt = world.spawn(start, TNTPrimed.class, entity -> {
                    entity.setSource(source);
                    entity.setFuseTicks(p.fuse());
                    entity.setYield(power);
                    entity.setIsIncendiary(false);
                    entity.setGravity(p.gravity());
                    if (pattern.stab()) entity.setNoPhysics(true);
                    entity.setVelocity(new Vector(p.vx(), p.vy(), p.vz()));
                    entity.setPersistent(false);
                    entity.addScoreboardTag("lpchat_orbital");
                });
                spawned.add(tnt);
                if (pattern.stab()) drillBlocks.put(tnt.getUniqueId(), start);
            }
        } catch (RuntimeException exception) {
            spawned.forEach(t -> { drillBlocks.remove(t.getUniqueId()); t.remove(); });
            throw exception;
        }
        // A wall-clock expiry also removes suspended TNT if chunks stop ticking.
        active.add(new Active(spawned, now + 30_000_000_000L));
        plugin.getLogger().info("Orbital " + pattern.id() + ": " + spawned.size() + " TNT, "
                + (source == null ? "Konsole" : source.getName()) + ", " + world.getName()
                + " " + target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ());
        return spawned.size();
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onExplosion(org.bukkit.event.entity.EntityExplodeEvent event) {
        Location centre = drillBlocks.remove(event.getEntity().getUniqueId());
        if (centre == null || event.isCancelled()) return;
        org.bukkit.block.Block block = centre.getBlock();
        if (block.getType() != org.bukkit.Material.BEDROCK && !block.isEmpty()
                && !event.blockList().contains(block)) event.blockList().add(block);
    }

    private void checkTickLimit(World world, int count) {
        var config = plugin.getServer().spigot().getSpigotConfig();
        int limit = config.getInt("world-settings." + world.getName() + ".max-tnt-per-tick",
                config.getInt("world-settings.default.max-tnt-per-tick", 100));
        long existing = active.stream().flatMap(a -> a.entities.stream()).filter(t -> t.getWorld().equals(world)).count();
        if (limit > 0 && count + existing > limit)
            throw new IllegalArgumentException("spigot.yml: max-tnt-per-tick auf -1 setzen und neu starten (sonst zerfaellt das TNT-Muster).");
    }

    public void preview(Player player, Location target, StrikePattern pattern, int size) {
        OrbitalAccess.require(player);
        List<StrikePattern.Point> points = pattern.points(size, target.getYaw());
        validateArea(target, points);
        // Private particles only; previews never prime TNT or modify blocks.
        for (StrikePattern.Point p : points) {
            Location point = target.clone().add(p.x(), 1.2, p.z());
            player.spawnParticle(Particle.END_ROD, point, 3, .1, .1, .1, 0);
            if (pattern.stab())
                for (int y = 4; y <= 24; y += 4) player.spawnParticle(Particle.END_ROD, point.clone().add(0, y, 0), 1, 0, 0, 0, 0);
        }
    }

    private void validateArea(Location target, List<StrikePattern.Point> points) {
        World world = target.getWorld();
        if (!Double.isFinite(target.getX()) || !Double.isFinite(target.getZ())
                || Math.abs(target.getX()) > 29_999_000 || Math.abs(target.getZ()) > 29_999_000
                || !Double.isFinite(target.getY()) || target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight())
            throw new IllegalArgumentException("Ungueltige Zielkoordinaten.");
        double radius = points.stream().mapToDouble(p -> Math.max(Math.abs(p.x()), Math.abs(p.z()))).max().orElse(0) + 10;
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1})
            if (!world.getWorldBorder().isInside(target.clone().add(dx * radius, 0, dz * radius)))
                throw new IllegalArgumentException("Schuss liegt zu nah an der Weltgrenze.");
        int minX = (int) Math.floor(target.getX() - radius) >> 4, maxX = (int) Math.floor(target.getX() + radius) >> 4;
        int minZ = (int) Math.floor(target.getZ() - radius) >> 4, maxZ = (int) Math.floor(target.getZ() + radius) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (!world.isChunkLoaded(x, z)) throw new IllegalArgumentException("Zielgebiet ist nicht geladen. Gehe naeher heran.");
    }

    private void prune() {
        long now = System.nanoTime();
        active.removeIf(a -> {
            if (now >= a.expires) { a.entities.forEach(t -> { drillBlocks.remove(t.getUniqueId()); t.remove(); }); return true; }
            a.entities.removeIf(t -> { if (!t.isDead()) return false; drillBlocks.remove(t.getUniqueId()); return true; });
            return a.entities.isEmpty();
        });
    }
    public int cancelAll() {
        int count = active.stream().mapToInt(a -> a.entities.size()).sum();
        active.forEach(a -> a.entities.forEach(TNTPrimed::remove));
        active.clear();
        drillBlocks.clear();
        return count;
    }
    @Override public void close() { cleanup.cancel(); cancelAll(); }
}


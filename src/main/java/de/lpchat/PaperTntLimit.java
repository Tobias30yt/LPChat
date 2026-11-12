package de.lpchat;

import java.lang.reflect.Modifier;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Applies to loaded worlds and worlds loaded later; no LPChat config entry needed. */
final class PaperTntLimit implements Listener {
    private final JavaPlugin plugin;
    PaperTntLimit(JavaPlugin plugin) { this.plugin = plugin; }
    void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getWorlds().forEach(this::apply);
    }
    @EventHandler public void onWorldLoad(WorldLoadEvent event) { apply(event.getWorld()); }
    private void apply(World world) {
        try {
            Object handle = world.getClass().getMethod("getHandle").invoke(world);
            Object settings = handle.getClass().getField("spigotConfig").get(handle);
            setUnlimited(settings);
            var config = plugin.getServer().spigot().getSpigotConfig();
            config.set("world-settings." + world.getName() + ".max-tnt-per-tick", -1);
            config.set("world-settings." + world.getKey() + ".max-tnt-per-tick", -1);
            plugin.getLogger().info("max-tnt-per-tick = -1 fuer " + world.getKey());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            plugin.getLogger().warning("TNT-Limit konnte fuer " + world.getName() + " nicht gesetzt werden: " + e);
        }
    }
    static void setUnlimited(Object settings) throws ReflectiveOperationException {
        var field = settings.getClass().getField("maxTntTicksPerTick");
        if (field.getType() != int.class || Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
            throw new NoSuchFieldException("Expected mutable int maxTntTicksPerTick");
        field.setInt(settings, -1);
    }
}

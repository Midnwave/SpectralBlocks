package com.horizonsmp.spectralblocks.particles;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import com.horizonsmp.spectralblocks.ghost.GhostBlock;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class ParticleManager {

    private static final double PARTICLE_RANGE_SQUARED = 48.0 * 48.0; // 48 block radius

    private final SpectralBlocksPlugin plugin;
    private BukkitTask task;
    private final Random random = new Random();

    public ParticleManager(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfigManager().isParticlesEnabled()) return;
        int interval = plugin.getConfigManager().getParticleIntervalTicks();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnParticles();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void spawnParticles() {
        Particle particle = plugin.getConfigManager().getParticleType();
        double spread = plugin.getConfigManager().getParticleSpread();

        for (GhostBlock ghost : plugin.getGhostBlockManager().getAllGhostBlocks()) {
            World world = plugin.getServer().getWorld(ghost.getWorld());
            if (world == null) continue;

            double cx = ghost.getX() + 0.5;
            double cy = ghost.getY() + 0.5;
            double cz = ghost.getZ() + 0.5;

            for (Player player : world.getPlayers()) {
                if (!shouldSeeParticles(player, ghost)) continue;

                double dx = player.getLocation().getX() - cx;
                double dy = player.getLocation().getY() - cy;
                double dz = player.getLocation().getZ() - cz;
                if (dx * dx + dy * dy + dz * dz > PARTICLE_RANGE_SQUARED) continue;

                Location loc = new Location(world,
                        cx + (random.nextDouble() - 0.5) * spread,
                        cy + (random.nextDouble() - 0.5) * spread,
                        cz + (random.nextDouble() - 0.5) * spread);

                player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
            }
        }
    }

    private boolean shouldSeeParticles(Player player, GhostBlock ghost) {
        var manager = plugin.getGhostBlockManager();

        if (player.hasPermission("spectralblocks.admin.seeghost") && manager.isAdminReveal(player.getUniqueId())) {
            return true;
        }

        return ghost.getOwnerUuid().equals(player.getUniqueId())
                && manager.areParticlesEnabled(player.getUniqueId());
    }
}

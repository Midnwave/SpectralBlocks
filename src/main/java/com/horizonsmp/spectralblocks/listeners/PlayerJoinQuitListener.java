package com.horizonsmp.spectralblocks.listeners;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinQuitListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public PlayerJoinQuitListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // delay 2 ticks so the player is fully loaded before sending packets
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                plugin.getGhostBlockManager().getPacketUtil()
                        .sendAllGhostBlocksInWorld(player, plugin.getGhostBlockManager().getAllGhostBlocks());
            }
        }.runTaskLater(plugin, 2L);
    }
}

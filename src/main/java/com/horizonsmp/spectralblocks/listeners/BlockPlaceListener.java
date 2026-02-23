package com.horizonsmp.spectralblocks.listeners;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public BlockPlaceListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.getGhostBlockManager().handleRealBlockPlaced(event.getBlock().getLocation());
    }
}

package com.blockforge.spectralblocks.listeners;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChunkLoadListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public ChunkLoadListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        String worldName = event.getChunk().getWorld().getName();

        plugin.getGhostBlockManager().getPacketUtil().sendGhostBlocksInChunk(
                event.getPlayer(),
                worldName,
                chunkX,
                chunkZ,
                plugin.getGhostBlockManager().getAllGhostBlocks()
        );
    }
}

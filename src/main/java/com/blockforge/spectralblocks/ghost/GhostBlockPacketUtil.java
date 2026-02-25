package com.blockforge.spectralblocks.ghost;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.integrations.ItemsAdderIntegration;
import com.blockforge.spectralblocks.integrations.NexoIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages ghost block visuals using BlockDisplay entities.
 * Display entities are purely visual with no collision — players can walk through them.
 * Entities are non-persistent and re-spawned via ChunkLoadListener as chunks load.
 */
public class GhostBlockPacketUtil {

    private final SpectralBlocksPlugin plugin;

    /** Tracks spawned display entity UUIDs by ghost block location key */
    private final Map<String, UUID> displayEntities = new ConcurrentHashMap<>();

    public GhostBlockPacketUtil(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    /** Spawn a BlockDisplay entity for the given ghost block. */
    public void spawnDisplayEntity(GhostBlock ghost) {
        BlockData blockData = resolveBlockData(ghost);
        if (blockData == null) return;

        World world = Bukkit.getWorld(ghost.getWorld());
        if (world == null) return;

        // remove stale entity at this location if present
        removeDisplayEntity(ghost.locationKey());

        Location loc = new Location(world, ghost.getX(), ghost.getY(), ghost.getZ());
        try {
            BlockDisplay display = world.spawn(loc, BlockDisplay.class, entity -> {
                entity.setBlock(blockData);
                entity.setPersistent(false);
                entity.setGravity(false);
                entity.addScoreboardTag("spectralblocks_ghost");
            });
            displayEntities.put(ghost.locationKey(), display.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to spawn ghost block display at " + ghost.locationKey(), e);
        }
    }

    /** Remove the display entity for the given ghost block. */
    public void removeDisplayEntity(GhostBlock ghost) {
        removeDisplayEntity(ghost.locationKey());
    }

    // ---- backward-compatible methods called by GhostBlockManager ----

    public void broadcastGhostBlock(GhostBlock ghost) {
        spawnDisplayEntity(ghost);
    }

    public void broadcastRestoreBlock(GhostBlock ghost) {
        removeDisplayEntity(ghost);
    }

    // ---- chunk / join helpers ----

    /**
     * Ensure display entities exist for all ghost blocks in the given chunk.
     * Called by ChunkLoadListener when a chunk is sent to a player.
     */
    public void sendGhostBlocksInChunk(Player player, String worldName, int chunkX, int chunkZ,
                                        Collection<GhostBlock> ghostBlocks) {
        for (GhostBlock ghost : ghostBlocks) {
            if (!ghost.getWorld().equals(worldName)) continue;
            int blockChunkX = Math.floorDiv(ghost.getX(), 16);
            int blockChunkZ = Math.floorDiv(ghost.getZ(), 16);
            if (blockChunkX == chunkX && blockChunkZ == chunkZ) {
                ensureEntityExists(ghost);
            }
        }
    }

    /**
     * Ensure display entities exist for all ghost blocks visible to the player.
     * Called on player join as a fallback for non-Paper servers.
     */
    public void sendAllGhostBlocksInWorld(Player player, Collection<GhostBlock> ghostBlocks) {
        String worldName = player.getWorld().getName();
        for (GhostBlock ghost : ghostBlocks) {
            if (!ghost.getWorld().equals(worldName)) continue;
            ensureEntityExists(ghost);
        }
    }

    /** Remove all tracked display entities (used on plugin disable). */
    public void removeAllDisplayEntities() {
        for (UUID entityUuid : displayEntities.values()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) entity.remove();
        }
        displayEntities.clear();
    }

    // ---- internals ----

    private void ensureEntityExists(GhostBlock ghost) {
        UUID existingUuid = displayEntities.get(ghost.locationKey());
        if (existingUuid != null) {
            Entity existing = Bukkit.getEntity(existingUuid);
            if (existing != null && !existing.isDead()) return;
        }
        spawnDisplayEntity(ghost);
    }

    private void removeDisplayEntity(String locationKey) {
        UUID entityUuid = displayEntities.remove(locationKey);
        if (entityUuid == null) return;
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) entity.remove();
    }

    private BlockData resolveBlockData(GhostBlock ghost) {
        String blockDataString = resolveBlockDataString(ghost);
        if (blockDataString == null) return null;
        try {
            return Bukkit.createBlockData(blockDataString);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to parse block data: " + blockDataString, e);
            return null;
        }
    }

    private String resolveBlockDataString(GhostBlock ghost) {
        if (ghost.isItemsAdder()) {
            ItemsAdderIntegration ia = plugin.getItemsAdderIntegration();
            if (!ia.isEnabled() || !ia.isDataLoaded()) return null;
            return ia.getBlockDataString(ghost.getBlockType());
        }
        if (ghost.isNexo()) {
            NexoIntegration nexo = plugin.getNexoIntegration();
            if (!nexo.isEnabled() || !nexo.isDataLoaded()) return null;
            return nexo.getBlockDataString(ghost.getBlockType());
        }
        return ghost.getBlockType();
    }
}

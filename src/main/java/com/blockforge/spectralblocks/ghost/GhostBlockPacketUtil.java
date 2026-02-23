package com.blockforge.spectralblocks.ghost;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.integrations.ItemsAdderIntegration;
import com.blockforge.spectralblocks.integrations.NexoIntegration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.logging.Level;

public class GhostBlockPacketUtil {

    private final SpectralBlocksPlugin plugin;

    public GhostBlockPacketUtil(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendGhostBlock(Player player, GhostBlock ghost) {
        String blockDataString = resolveBlockDataString(ghost);
        if (blockDataString == null) return;
        sendBlockPacket(player, ghost.getWorld(), ghost.getX(), ghost.getY(), ghost.getZ(), blockDataString);
    }

    public void restoreBlock(Player player, GhostBlock ghost) {
        World world = player.getServer().getWorld(ghost.getWorld());
        if (world == null) return;
        Location loc = new Location(world, ghost.getX(), ghost.getY(), ghost.getZ());
        String realBlockData = loc.getBlock().getBlockData().getAsString();
        sendBlockPacket(player, ghost.getWorld(), ghost.getX(), ghost.getY(), ghost.getZ(), realBlockData);
    }

    public void broadcastGhostBlock(GhostBlock ghost) {
        String blockDataString = resolveBlockDataString(ghost);
        if (blockDataString == null) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(ghost.getWorld())) continue;
            sendBlockPacket(player, ghost.getWorld(), ghost.getX(), ghost.getY(), ghost.getZ(), blockDataString);
        }
    }

    // sends the real block back to everyone, undoing the ghost visual
    public void broadcastRestoreBlock(GhostBlock ghost) {
        World world = plugin.getServer().getWorld(ghost.getWorld());
        String realBlockData = world != null
                ? new Location(world, ghost.getX(), ghost.getY(), ghost.getZ()).getBlock().getBlockData().getAsString()
                : "minecraft:air";

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(ghost.getWorld())) continue;
            sendBlockPacket(player, ghost.getWorld(), ghost.getX(), ghost.getY(), ghost.getZ(), realBlockData);
        }
    }

    public void sendAllGhostBlocksInWorld(Player player, Collection<GhostBlock> ghostBlocks) {
        String worldName = player.getWorld().getName();
        for (GhostBlock ghost : ghostBlocks) {
            if (!ghost.getWorld().equals(worldName)) continue;
            if (!isInRange(player, ghost)) continue;
            sendGhostBlock(player, ghost);
        }
    }

    public void sendGhostBlocksInChunk(Player player, String worldName, int chunkX, int chunkZ,
                                        Collection<GhostBlock> ghostBlocks) {
        for (GhostBlock ghost : ghostBlocks) {
            if (!ghost.getWorld().equals(worldName)) continue;
            int blockChunkX = Math.floorDiv(ghost.getX(), 16);
            int blockChunkZ = Math.floorDiv(ghost.getZ(), 16);
            if (blockChunkX == chunkX && blockChunkZ == chunkZ) {
                sendGhostBlock(player, ghost);
            }
        }
    }

    private void sendBlockPacket(Player player, String worldName, int x, int y, int z, String blockDataString) {
        try {
            var clientVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
            WrappedBlockState state = WrappedBlockState.getByString(clientVersion, blockDataString);
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                    new Vector3i(x, y, z), state.getGlobalId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "failed to send ghost block packet to " + player.getName() + " at " + x + "," + y + "," + z, e);
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

    private boolean isInRange(Player player, GhostBlock ghost) {
        if (!player.getWorld().getName().equals(ghost.getWorld())) return false;
        int viewDist = player.getServer().getViewDistance() * 16;
        int dx = player.getLocation().getBlockX() - ghost.getX();
        int dz = player.getLocation().getBlockZ() - ghost.getZ();
        return (dx * dx + dz * dz) <= (viewDist * viewDist);
    }
}

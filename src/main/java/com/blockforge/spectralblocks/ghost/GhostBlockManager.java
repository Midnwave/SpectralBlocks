package com.blockforge.spectralblocks.ghost;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.storage.SQLiteStorage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class GhostBlockManager {

    private final SpectralBlocksPlugin plugin;
    private final SQLiteStorage storage;
    private final GhostBlockPacketUtil packetUtil;

    private final Map<String, GhostBlock> byLocation = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> byOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> particlesDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> adminReveal = ConcurrentHashMap.newKeySet();

    public GhostBlockManager(SpectralBlocksPlugin plugin, SQLiteStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.packetUtil = new GhostBlockPacketUtil(plugin);
    }

    public void loadAll() {
        byLocation.clear();
        byOwner.clear();
        List<GhostBlock> blocks = storage.loadAll();
        for (GhostBlock ghost : blocks) {
            byLocation.put(ghost.locationKey(), ghost);
            byOwner.computeIfAbsent(ghost.getOwnerUuid(), k -> new ArrayList<>())
                   .add(ghost.locationKey());
        }
        plugin.getLogger().info("Loaded " + blocks.size() + " ghost blocks from storage.");
    }

    public PlaceResult place(Player player, Location location, String blockType) {
        if (!plugin.getConfigManager().isWorldAllowed(location.getWorld().getName())
                && !player.hasPermission("spectralblocks.bypass.world")) {
            return PlaceResult.WORLD_DENIED;
        }

        if (!plugin.getWorldGuardIntegration().canBuild(player, location)) {
            return PlaceResult.REGION_DENIED;
        }

        if (!player.hasPermission("spectralblocks.bypass.cooldown")) {
            int cooldownSec = plugin.getConfigManager().getCooldownSeconds();
            if (cooldownSec > 0) {
                long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                long elapsed = System.currentTimeMillis() - last;
                if (elapsed < cooldownSec * 1000L) {
                    return PlaceResult.COOLDOWN;
                }
            }
        }

        if (!player.hasPermission("spectralblocks.bypass.limit")) {
            int limit = resolveLimit(player);
            int current = byOwner.getOrDefault(player.getUniqueId(), List.of()).size();
            if (limit >= 0 && current >= limit) {
                return PlaceResult.LIMIT_REACHED;
            }
        }

        String locKey = GhostBlock.locationKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (byLocation.containsKey(locKey)) {
            return PlaceResult.OCCUPIED;
        }

        GhostBlock ghost = new GhostBlock(
                UUID.randomUUID(),
                player.getUniqueId(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                blockType,
                System.currentTimeMillis()
        );

        byLocation.put(locKey, ghost);
        byOwner.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(locKey);
        storage.save(ghost);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // delay 1 tick so the packet arrives after Paper's block correction from the cancelled event
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> packetUtil.broadcastGhostBlock(ghost), 1L);

        plugin.getAuditLogger().logPlace(player.getUniqueId(), player.getName(), ghost);
        return PlaceResult.SUCCESS;
    }

    // actor is null when removed by a real block being placed on top
    public RemoveResult remove(Player actor, Location location, boolean isAdmin) {
        String locKey = GhostBlock.locationKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        GhostBlock ghost = byLocation.get(locKey);
        if (ghost == null) return RemoveResult.NOT_FOUND;

        if (actor != null && !isAdmin && !ghost.getOwnerUuid().equals(actor.getUniqueId())) {
            return RemoveResult.NOT_YOURS;
        }

        removeGhost(ghost);

        if (actor != null) {
            if (isAdmin) {
                plugin.getAuditLogger().logAdminRemove(actor.getUniqueId(), actor.getName(), ghost);
            } else {
                plugin.getAuditLogger().logRemove(actor.getUniqueId(), actor.getName(), ghost);
            }
        }
        return RemoveResult.SUCCESS;
    }

    public int removeAllByOwner(UUID ownerUuid, Player admin) {
        List<String> keys = byOwner.remove(ownerUuid);
        if (keys == null || keys.isEmpty()) return 0;
        int count = 0;
        for (String key : new ArrayList<>(keys)) {
            GhostBlock ghost = byLocation.remove(key);
            if (ghost == null) continue;
            storage.delete(ghost.getId());
            packetUtil.broadcastRestoreBlock(ghost);
            if (admin != null) {
                plugin.getAuditLogger().logAdminRemove(admin.getUniqueId(), admin.getName(), ghost);
            }
            count++;
        }
        storage.deleteAllByOwner(ownerUuid);
        return count;
    }

    public void handleRealBlockPlaced(Location location) {
        String locKey = GhostBlock.locationKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        GhostBlock ghost = byLocation.get(locKey);
        if (ghost == null) return;
        removeGhost(ghost);
    }

    public GhostBlock getAtLocation(Location location) {
        return byLocation.get(GhostBlock.locationKey(
                location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public GhostBlock getAtLocation(String world, int x, int y, int z) {
        return byLocation.get(GhostBlock.locationKey(world, x, y, z));
    }

    public List<GhostBlock> getByOwner(UUID ownerUuid) {
        List<String> keys = byOwner.getOrDefault(ownerUuid, List.of());
        List<GhostBlock> result = new ArrayList<>();
        for (String key : keys) {
            GhostBlock g = byLocation.get(key);
            if (g != null) result.add(g);
        }
        return result;
    }

    public Collection<GhostBlock> getAllGhostBlocks() {
        return Collections.unmodifiableCollection(byLocation.values());
    }

    public GhostBlockPacketUtil getPacketUtil() {
        return packetUtil;
    }

    public boolean toggleParticles(UUID uuid) {
        if (particlesDisabled.contains(uuid)) {
            particlesDisabled.remove(uuid);
            return true;
        } else {
            particlesDisabled.add(uuid);
            return false;
        }
    }

    public boolean areParticlesEnabled(UUID uuid) {
        return !particlesDisabled.contains(uuid);
    }

    public boolean toggleAdminReveal(UUID uuid) {
        if (adminReveal.contains(uuid)) {
            adminReveal.remove(uuid);
            return false;
        } else {
            adminReveal.add(uuid);
            return true;
        }
    }

    public boolean isAdminReveal(UUID uuid) {
        return adminReveal.contains(uuid);
    }

    private void removeGhost(GhostBlock ghost) {
        byLocation.remove(ghost.locationKey());
        List<String> ownerList = byOwner.get(ghost.getOwnerUuid());
        if (ownerList != null) ownerList.remove(ghost.locationKey());
        storage.delete(ghost.getId());
        packetUtil.broadcastRestoreBlock(ghost);
    }

    // picks the highest spectralblocks.limit.<n> perm the player has
    public int resolveLimit(Player player) {
        int highest = -1;
        for (var pi : player.getEffectivePermissions()) {
            String perm = pi.getPermission();
            if (!pi.getValue() || !perm.startsWith("spectralblocks.limit.")) continue;
            try {
                int val = Integer.parseInt(perm.substring("spectralblocks.limit.".length()));
                if (val > highest) highest = val;
            } catch (NumberFormatException ignored) {}
        }
        return highest >= 0 ? highest : plugin.getConfigManager().getDefaultLimit();
    }

    public enum PlaceResult {
        SUCCESS, WORLD_DENIED, REGION_DENIED, COOLDOWN, LIMIT_REACHED, OCCUPIED
    }

    public enum RemoveResult {
        SUCCESS, NOT_FOUND, NOT_YOURS
    }
}

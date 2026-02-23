package com.horizonsmp.spectralblocks.ghost;

import java.util.Objects;
import java.util.UUID;

public final class GhostBlock {

    private final UUID id;
    private final UUID ownerUuid;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String blockType;
    private final long placedAt;

    public GhostBlock(UUID id, UUID ownerUuid, String world, int x, int y, int z,
                      String blockType, long placedAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.placedAt = placedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    // full bukkit blockdata string for vanilla, or "itemsadder:ns:id" for ia blocks
    public String getBlockType() { return blockType; }
    public long getPlacedAt() { return placedAt; }

    public boolean isItemsAdder() {
        return blockType.startsWith("itemsadder:");
    }

    public String locationKey() {
        return world + "," + x + "," + y + "," + z;
    }

    public static String locationKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GhostBlock that)) return false;
        return x == that.x && y == that.y && z == that.z && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return "GhostBlock{owner=" + ownerUuid + ", world=" + world
                + ", pos=[" + x + "," + y + "," + z + "], type=" + blockType + "}";
    }
}

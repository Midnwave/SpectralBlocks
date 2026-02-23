package com.blockforge.spectralblocks.storage;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.ghost.GhostBlock;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteStorage {

    private final SpectralBlocksPlugin plugin;
    private Connection connection;

    public SQLiteStorage(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        File dbFile = new File(dataFolder, "data.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}

        connection = DriverManager.getConnection(url);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ghost_blocks (
                    id          TEXT PRIMARY KEY,
                    owner_uuid  TEXT NOT NULL,
                    world       TEXT NOT NULL,
                    x           INTEGER NOT NULL,
                    y           INTEGER NOT NULL,
                    z           INTEGER NOT NULL,
                    block_type  TEXT NOT NULL,
                    placed_at   INTEGER NOT NULL,
                    UNIQUE(world, x, y, z)
                )
            """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_owner ON ghost_blocks(owner_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_world_pos ON ghost_blocks(world, x, y, z)");
        }
    }

    public void save(GhostBlock ghost) {
        String sql = """
            INSERT OR REPLACE INTO ghost_blocks
            (id, owner_uuid, world, x, y, z, block_type, placed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ghost.getId().toString());
            ps.setString(2, ghost.getOwnerUuid().toString());
            ps.setString(3, ghost.getWorld());
            ps.setInt(4, ghost.getX());
            ps.setInt(5, ghost.getY());
            ps.setInt(6, ghost.getZ());
            ps.setString(7, ghost.getBlockType());
            ps.setLong(8, ghost.getPlacedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "failed to save ghost block: " + ghost, e);
        }
    }

    public void delete(UUID ghostId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM ghost_blocks WHERE id = ?")) {
            ps.setString(1, ghostId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "failed to delete ghost block: " + ghostId, e);
        }
    }

    public void deleteAllByOwner(UUID ownerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM ghost_blocks WHERE owner_uuid = ?")) {
            ps.setString(1, ownerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "failed to delete ghost blocks for owner: " + ownerUuid, e);
        }
    }

    public List<GhostBlock> loadAll() {
        List<GhostBlock> blocks = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ghost_blocks")) {
            while (rs.next()) {
                blocks.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "failed to load ghost blocks", e);
        }
        return blocks;
    }

    public List<GhostBlock> loadByOwner(UUID ownerUuid) {
        List<GhostBlock> blocks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM ghost_blocks WHERE owner_uuid = ?")) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) blocks.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "failed to load ghost blocks for owner: " + ownerUuid, e);
        }
        return blocks;
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    private GhostBlock fromResultSet(ResultSet rs) throws SQLException {
        return new GhostBlock(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getString("block_type"),
                rs.getLong("placed_at")
        );
    }
}

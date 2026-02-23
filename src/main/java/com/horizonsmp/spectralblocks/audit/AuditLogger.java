package com.horizonsmp.spectralblocks.audit;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import com.horizonsmp.spectralblocks.ghost.GhostBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class AuditLogger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final SpectralBlocksPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SpectralBlocks-Audit");
        t.setDaemon(true);
        return t;
    });
    private File logFile;

    public AuditLogger(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        logFile = new File(plugin.getDataFolder(), "audit.log");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
    }

    public void logPlace(UUID playerUuid, String playerName, GhostBlock ghost) {
        log("PLACE", playerUuid, playerName, ghost);
    }

    public void logRemove(UUID playerUuid, String playerName, GhostBlock ghost) {
        log("REMOVE", playerUuid, playerName, ghost);
    }

    public void logAdminRemove(UUID adminUuid, String adminName, GhostBlock ghost) {
        log("ADMIN_REMOVE", adminUuid, adminName, ghost);
    }

    private void log(String action, UUID actorUuid, String actorName, GhostBlock ghost) {
        if (!plugin.getConfigManager().isAuditLogEnabled()) return;
        String timestamp = FORMATTER.format(Instant.now());
        String line = "[" + timestamp + "] [" + action + "] "
                + actorName + " (" + actorUuid + ") | "
                + ghost.getBlockType() + " @ "
                + ghost.getWorld() + " (" + ghost.getX() + ", " + ghost.getY() + ", " + ghost.getZ() + ")";
        executor.submit(() -> writeLine(line));
    }

    private void writeLine(String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.println(line);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write to audit log", e);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}

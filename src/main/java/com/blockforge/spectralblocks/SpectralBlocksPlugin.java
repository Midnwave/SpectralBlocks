package com.blockforge.spectralblocks;

import com.github.retrooper.packetevents.PacketEvents;
import com.blockforge.spectralblocks.audit.AuditLogger;
import com.blockforge.spectralblocks.commands.GhostBlocksCommand;
import com.blockforge.spectralblocks.commands.GhostBlocksTabCompleter;
import com.blockforge.spectralblocks.config.ConfigManager;
import com.blockforge.spectralblocks.config.MessagesManager;
import com.blockforge.spectralblocks.ghost.GhostBlockManager;
import com.blockforge.spectralblocks.gui.GUIClickListener;
import com.blockforge.spectralblocks.gui.MyGhostBlocksTab;
import com.blockforge.spectralblocks.gui.SearchTab;
import com.blockforge.spectralblocks.integrations.ItemsAdderIntegration;
import com.blockforge.spectralblocks.integrations.NexoIntegration;
import com.blockforge.spectralblocks.integrations.WorldGuardIntegration;
import com.blockforge.spectralblocks.items.GhostBlockItem;
import com.blockforge.spectralblocks.items.GhostRemoveTool;
import com.blockforge.spectralblocks.listeners.*;
import com.blockforge.spectralblocks.particles.ParticleManager;
import com.blockforge.spectralblocks.storage.SQLiteStorage;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class SpectralBlocksPlugin extends JavaPlugin {

    private static SpectralBlocksPlugin instance;

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private SQLiteStorage storage;
    private GhostBlockManager ghostBlockManager;
    private ParticleManager particleManager;
    private AuditLogger auditLogger;
    private WorldGuardIntegration worldGuardIntegration;
    private ItemsAdderIntegration itemsAdderIntegration;
    private NexoIntegration nexoIntegration;

    @Override
    public void onLoad() {
        instance = this;
        // packetevents must be initialized in onload
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.load();

        messagesManager = new MessagesManager(this);
        messagesManager.load();

        storage = new SQLiteStorage(this);
        try {
            storage.init();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auditLogger = new AuditLogger(this);
        auditLogger.init();

        worldGuardIntegration = new WorldGuardIntegration();
        if (worldGuardIntegration.isEnabled()) {
            getLogger().info("WorldGuard detected — region flag support enabled.");
        }

        itemsAdderIntegration = new ItemsAdderIntegration();
        if (itemsAdderIntegration.isEnabled()) {
            getLogger().info("ItemsAdder detected — custom block support enabled.");
        }

        nexoIntegration = new NexoIntegration();
        if (nexoIntegration.isEnabled()) {
            getLogger().info("Nexo detected — custom block support enabled.");
        }

        ghostBlockManager = new GhostBlockManager(this, storage);
        ghostBlockManager.loadAll();

        particleManager = new ParticleManager(this);
        particleManager.start();

        PacketEvents.getAPI().init();

        GhostBlockItem.init(this);
        GhostRemoveTool.init(this);
        MyGhostBlocksTab.init(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new BlockPlaceListener(this), this);
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new GUIClickListener(), this);
        pm.registerEvents(new SearchTab.ChatListener(this), this);

        if (isPaper()) {
            pm.registerEvents(new ChunkLoadListener(this), this);
            getLogger().info("Paper detected — per-player chunk load packets enabled.");
        } else {
            getLogger().info("Spigot mode — ghost blocks will be sent on player join.");
        }

        if (itemsAdderIntegration.isEnabled()) {
            pm.registerEvents(new ItemsAdderLoadListener(this), this);
        }

        if (nexoIntegration.isEnabled()) {
            pm.registerEvents(new NexoLoadListener(this), this);
        }

        GhostBlocksCommand cmd = new GhostBlocksCommand(this);
        GhostBlocksTabCompleter tab = new GhostBlocksTabCompleter(this);
        var ghostCmd = getCommand("ghostblocks");
        if (ghostCmd != null) {
            ghostCmd.setExecutor(cmd);
            ghostCmd.setTabCompleter(tab);
        }

        if (configManager.isMetricsEnabled()) {
            new Metrics(this, 99999);
        }

        getLogger().info("SpectralBlocks v" + getDescription().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (particleManager != null) particleManager.stop();
        if (auditLogger != null) auditLogger.shutdown();
        if (storage != null) storage.close();
        PacketEvents.getAPI().terminate();
        getLogger().info("SpectralBlocks disabled.");
    }

    public static SpectralBlocksPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public GhostBlockManager getGhostBlockManager() { return ghostBlockManager; }
    public ParticleManager getParticleManager() { return particleManager; }
    public AuditLogger getAuditLogger() { return auditLogger; }
    public WorldGuardIntegration getWorldGuardIntegration() { return worldGuardIntegration; }
    public ItemsAdderIntegration getItemsAdderIntegration() { return itemsAdderIntegration; }
    public NexoIntegration getNexoIntegration() { return nexoIntegration; }

    private boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.event.packet.PlayerChunkLoadEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

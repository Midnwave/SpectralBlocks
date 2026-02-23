package com.blockforge.spectralblocks.config;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private static final int CURRENT_VERSION = 1;

    private final SpectralBlocksPlugin plugin;

    private int cooldownSeconds;
    private int defaultLimit;
    private String worldsMode;
    private List<String> worldsList;
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleIntervalTicks;
    private double particleSpread;
    private boolean metricsEnabled;
    private boolean auditLogEnabled;
    private String guiTitle;
    private String borderMaterial;

    public ConfigManager(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        checkConfigVersion(cfg);

        cooldownSeconds = cfg.getInt("cooldown-seconds", 5);
        defaultLimit = cfg.getInt("default-limit", 50);
        worldsMode = cfg.getString("worlds.mode", "blacklist");
        worldsList = cfg.getStringList("worlds.list");
        particlesEnabled = cfg.getBoolean("particles.enabled", true);
        particleIntervalTicks = cfg.getInt("particles.interval-ticks", 20);
        particleSpread = cfg.getDouble("particles.spread", 0.5);
        metricsEnabled = cfg.getBoolean("metrics", true);
        auditLogEnabled = cfg.getBoolean("audit-log", true);
        guiTitle = cfg.getString("gui.title", "<gradient:#7B68EE:#00CED1><bold>Spectral Blocks</bold></gradient>");
        borderMaterial = cfg.getString("gui.border-material", "GRAY_STAINED_GLASS_PANE");

        String rawParticle = cfg.getString("particles.type", "END_ROD");
        try {
            particleType = Particle.valueOf(rawParticle.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type '" + rawParticle + "', falling back to END_ROD.");
            particleType = Particle.END_ROD;
        }
    }

    private void checkConfigVersion(FileConfiguration cfg) {
        int version = cfg.getInt("config-version", 1);
        if (version < CURRENT_VERSION) {
            plugin.getLogger().info("Config is outdated (version " + version + "), migrating to version " + CURRENT_VERSION + "...");
            cfg.set("config-version", CURRENT_VERSION);
            plugin.saveConfig();
        }
    }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getDefaultLimit() { return defaultLimit; }
    public String getWorldsMode() { return worldsMode; }
    public List<String> getWorldsList() { return worldsList; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public Particle getParticleType() { return particleType; }
    public int getParticleIntervalTicks() { return particleIntervalTicks; }
    public double getParticleSpread() { return particleSpread; }
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public boolean isAuditLogEnabled() { return auditLogEnabled; }
    public String getGuiTitle() { return guiTitle; }
    public String getBorderMaterial() { return borderMaterial; }

    public boolean isWorldAllowed(String worldName) {
        boolean inList = worldsList.contains(worldName);
        return worldsMode.equalsIgnoreCase("whitelist") ? inList : !inList;
    }
}

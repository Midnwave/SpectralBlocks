package com.blockforge.spectralblocks.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardIntegration {

    private final boolean enabled;

    public WorldGuardIntegration() {
        boolean ok = false;
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            ok = true;
        } catch (ClassNotFoundException ignored) {}
        this.enabled = ok;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canBuild(Player player, Location location) {
        if (!enabled) return true;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            return query.testState(weLoc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
        } catch (Exception e) {
            return true; // fail open
        }
    }
}

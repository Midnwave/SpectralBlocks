package com.blockforge.spectralblocks.listeners;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class NexoLoadListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public NexoLoadListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNexoLoaded(NexoItemsLoadedEvent event) {
        plugin.getNexoIntegration().markDataLoaded();
        plugin.getLogger().info("Nexo data loaded — custom blocks are now available.");
    }
}

package com.horizonsmp.spectralblocks.listeners;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ItemsAdderLoadListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public ItemsAdderLoadListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        plugin.getItemsAdderIntegration().markDataLoaded();
        plugin.getLogger().info("ItemsAdder data loaded — custom blocks are now available.");
    }
}

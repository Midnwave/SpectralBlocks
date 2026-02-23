package com.horizonsmp.spectralblocks.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIClickListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GhostBlockGUI gui)) return;
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        gui.handleClick(event.getRawSlot(), event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        SearchTab.clearAwaiter(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SearchTab.clearAwaiter(event.getPlayer().getUniqueId());
    }
}

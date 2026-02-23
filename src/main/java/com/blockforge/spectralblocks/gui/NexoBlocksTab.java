package com.blockforge.spectralblocks.gui;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.items.GhostBlockItem;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NexoBlocksTab {

    public static List<ItemStack> getPage(SpectralBlocksPlugin plugin, int page) {
        if (!plugin.getNexoIntegration().isEnabled()
                || !plugin.getNexoIntegration().isDataLoaded()) {
            return notLoadedPage(plugin);
        }

        List<String> sorted = getSortedIds(plugin);
        int pageSize = 36;
        int start = page * pageSize;
        if (start >= sorted.size()) return List.of();

        List<ItemStack> items = new ArrayList<>();
        int end = Math.min(start + pageSize, sorted.size());
        for (int i = start; i < end; i++) {
            String blockId = sorted.get(i);
            String blockType = "nexo:" + blockId;
            Material mat = GhostBlockItem.resolveMaterial(blockType);

            ItemStack item = GhostBlockItem.create(mat, blockType, blockId);
            ItemMeta meta = item.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(
                    meta.lore() != null ? meta.lore() : List.of());
            lore.add(plugin.getMessagesManager().format("gui-block-lore-nexo",
                    Placeholder.unparsed("block", blockType)));
            meta.lore(lore);
            item.setItemMeta(meta);
            items.add(item);
        }
        return items;
    }

    public static int getTotalBlocks(SpectralBlocksPlugin plugin) {
        if (!plugin.getNexoIntegration().isEnabled()
                || !plugin.getNexoIntegration().isDataLoaded()) return 1;
        return plugin.getNexoIntegration().getAllCustomBlockIds().length;
    }

    public static List<ItemStack> search(SpectralBlocksPlugin plugin, String query, int page) {
        if (!plugin.getNexoIntegration().isEnabled()
                || !plugin.getNexoIntegration().isDataLoaded()) return List.of();

        String q = query.toLowerCase();
        List<String> filtered = getSortedIds(plugin).stream()
                .filter(id -> id.toLowerCase().contains(q))
                .toList();

        int pageSize = 36;
        int start = page * pageSize;
        if (start >= filtered.size()) return List.of();

        List<ItemStack> items = new ArrayList<>();
        int end = Math.min(start + pageSize, filtered.size());
        for (int i = start; i < end; i++) {
            String blockId = filtered.get(i);
            String blockType = "nexo:" + blockId;
            Material mat = GhostBlockItem.resolveMaterial(blockType);
            items.add(GhostBlockItem.create(mat, blockType, blockId));
        }
        return items;
    }

    public static int searchCount(SpectralBlocksPlugin plugin, String query) {
        if (!plugin.getNexoIntegration().isEnabled()
                || !plugin.getNexoIntegration().isDataLoaded()) return 0;
        String q = query.toLowerCase();
        return (int) getSortedIds(plugin).stream()
                .filter(id -> id.toLowerCase().contains(q))
                .count();
    }

    private static List<String> getSortedIds(SpectralBlocksPlugin plugin) {
        String[] all = plugin.getNexoIntegration().getAllCustomBlockIds();
        List<String> sorted = new ArrayList<>(Arrays.asList(all));
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private static List<ItemStack> notLoadedPage(SpectralBlocksPlugin plugin) {
        ItemStack notice = new ItemStack(Material.BARRIER);
        ItemMeta meta = notice.getItemMeta();
        meta.displayName(plugin.getMessagesManager().format("gui-nexo-not-loaded"));
        notice.setItemMeta(meta);
        return List.of(notice);
    }
}

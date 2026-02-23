package com.horizonsmp.spectralblocks.gui;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import com.horizonsmp.spectralblocks.items.GhostBlockItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VanillaBlocksTab {

    private static List<Material> cachedBlocks = null;

    public static List<Material> getAllBlocks(SpectralBlocksPlugin plugin) {
        if (cachedBlocks != null) return cachedBlocks;

        List<Material> blocks = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (!mat.isBlock()) continue;
            if (mat.isAir()) continue;
            if (mat.isLegacy()) continue;
            if (!mat.isItem()) continue;
            if (isExcluded(mat)) continue;
            blocks.add(mat);
        }

        blocks.sort(Comparator.comparing(Material::name));
        cachedBlocks = blocks;
        plugin.getLogger().info("VanillaBlocksTab: loaded " + blocks.size() + " vanilla blocks.");
        return blocks;
    }

    public static void invalidateCache() {
        cachedBlocks = null;
    }

    public static List<ItemStack> getPage(SpectralBlocksPlugin plugin, int page) {
        List<Material> all = getAllBlocks(plugin);
        int pageSize = 36;
        int start = page * pageSize;
        if (start >= all.size()) return List.of();

        List<ItemStack> items = new ArrayList<>();
        int end = Math.min(start + pageSize, all.size());
        for (int i = start; i < end; i++) {
            Material mat = all.get(i);
            String blockType = "minecraft:" + mat.name().toLowerCase();
            String displayName = formatName(mat);
            items.add(GhostBlockItem.create(mat, blockType, displayName));
        }
        return items;
    }

    public static int getTotalBlocks(SpectralBlocksPlugin plugin) {
        return getAllBlocks(plugin).size();
    }

    public static List<ItemStack> search(SpectralBlocksPlugin plugin, String query, int page) {
        List<Material> all = getAllBlocks(plugin);
        String q = query.toLowerCase().replace(" ", "_").replace("minecraft:", "");
        List<Material> filtered = all.stream()
                .filter(m -> m.name().toLowerCase().contains(q))
                .toList();

        int pageSize = 36;
        int start = page * pageSize;
        if (start >= filtered.size()) return List.of();

        List<ItemStack> items = new ArrayList<>();
        int end = Math.min(start + pageSize, filtered.size());
        for (int i = start; i < end; i++) {
            Material mat = filtered.get(i);
            String blockType = "minecraft:" + mat.name().toLowerCase();
            items.add(GhostBlockItem.create(mat, blockType, formatName(mat)));
        }
        return items;
    }

    public static int searchCount(SpectralBlocksPlugin plugin, String query) {
        List<Material> all = getAllBlocks(plugin);
        String q = query.toLowerCase().replace(" ", "_").replace("minecraft:", "");
        return (int) all.stream().filter(m -> m.name().toLowerCase().contains(q)).count();
    }

    private static String formatName(Material mat) {
        String raw = mat.name().toLowerCase().replace('_', ' ');
        String[] words = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static boolean isExcluded(Material mat) {
        String name = mat.name();
        return name.equals("AIR")
                || name.equals("CAVE_AIR")
                || name.equals("VOID_AIR")
                || name.equals("BARRIER")
                || name.equals("LIGHT")
                || name.equals("STRUCTURE_BLOCK")
                || name.equals("STRUCTURE_VOID")
                || name.equals("JIGSAW")
                || name.equals("COMMAND_BLOCK")
                || name.equals("CHAIN_COMMAND_BLOCK")
                || name.equals("REPEATING_COMMAND_BLOCK")
                || name.equals("DEBUG_STICK")
                || name.startsWith("LEGACY_")
                || name.equals("MOVING_PISTON");
    }
}

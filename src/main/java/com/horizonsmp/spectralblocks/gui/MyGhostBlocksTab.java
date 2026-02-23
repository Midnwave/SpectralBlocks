package com.horizonsmp.spectralblocks.gui;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import com.horizonsmp.spectralblocks.ghost.GhostBlock;
import com.horizonsmp.spectralblocks.items.GhostBlockItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MyGhostBlocksTab {

    public static NamespacedKey GHOST_LOCATION_KEY;

    public static void init(SpectralBlocksPlugin plugin) {
        GHOST_LOCATION_KEY = new NamespacedKey(plugin, "ghost_location");
    }

    public static List<ItemStack> getPage(SpectralBlocksPlugin plugin, Player player, int page) {
        List<GhostBlock> blocks = plugin.getGhostBlockManager().getByOwner(player.getUniqueId());
        if (blocks.isEmpty()) {
            return noBlocksPage(plugin);
        }

        int pageSize = 36;
        int start = page * pageSize;
        if (start >= blocks.size()) return List.of();

        List<ItemStack> items = new ArrayList<>();
        int end = Math.min(start + pageSize, blocks.size());
        for (int i = start; i < end; i++) {
            GhostBlock ghost = blocks.get(i);
            items.add(buildEntry(plugin, ghost));
        }
        return items;
    }

    private static ItemStack buildEntry(SpectralBlocksPlugin plugin, GhostBlock ghost) {
        String blockType = ghost.getBlockType();
        String displayName = formatBlockType(blockType);
        org.bukkit.Material mat = GhostBlockItem.resolveMaterial(blockType);

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.getMessagesManager().getMiniMessage()
                .deserialize("<gradient:#7B68EE:#00CED1>✨ " + displayName + "</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.getMessagesManager().format("gui-mine-entry-lore",
                Placeholder.unparsed("world", ghost.getWorld()),
                Placeholder.unparsed("x", String.valueOf(ghost.getX())),
                Placeholder.unparsed("y", String.valueOf(ghost.getY())),
                Placeholder.unparsed("z", String.valueOf(ghost.getZ()))));
        meta.lore(lore);

        String locStr = ghost.getWorld() + "," + ghost.getX() + "," + ghost.getY() + "," + ghost.getZ();
        meta.getPersistentDataContainer().set(GHOST_LOCATION_KEY, PersistentDataType.STRING, locStr);
        meta.getPersistentDataContainer().set(GhostBlockItem.GHOST_BLOCK_KEY, PersistentDataType.STRING, blockType);

        item.setItemMeta(meta);
        return item;
    }

    private static List<ItemStack> noBlocksPage(SpectralBlocksPlugin plugin) {
        ItemStack notice = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta meta = notice.getItemMeta();
        meta.displayName(plugin.getMessagesManager().format("gui-no-ghost-blocks"));
        notice.setItemMeta(meta);
        return List.of(notice);
    }

    private static String formatBlockType(String blockType) {
        String base = blockType;
        int bracket = base.indexOf('[');
        if (bracket != -1) base = base.substring(0, bracket);
        base = base.replace("minecraft:", "").replace("itemsadder:", "").replace('_', ' ');
        String[] words = base.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}

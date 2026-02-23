package com.blockforge.spectralblocks.items;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class GhostBlockItem {

    public static final String PDC_KEY = "ghost_block";
    public static NamespacedKey GHOST_BLOCK_KEY;

    public static void init(SpectralBlocksPlugin plugin) {
        GHOST_BLOCK_KEY = new NamespacedKey(plugin, PDC_KEY);
    }

    public static ItemStack create(Material material, String blockType, String displayName) {
        SpectralBlocksPlugin plugin = SpectralBlocksPlugin.getInstance();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nameRaw = plugin.getMessagesManager().getRaw("ghost-item-name")
                .replace("<block>", displayName);
        meta.displayName(plugin.getMessagesManager().getMiniMessage().deserialize(nameRaw));

        String loreRaw = plugin.getMessagesManager().getRaw("ghost-item-lore")
                .replace("<block>", blockType);
        List<net.kyori.adventure.text.Component> lore = Arrays.stream(loreRaw.split("\n"))
                .map(line -> plugin.getMessagesManager().getMiniMessage().deserialize(line))
                .toList();
        meta.lore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        meta.getPersistentDataContainer().set(GHOST_BLOCK_KEY, PersistentDataType.STRING, blockType);

        item.setItemMeta(meta);
        return item;
    }

    public static String getBlockType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(GHOST_BLOCK_KEY, PersistentDataType.STRING);
    }

    public static boolean isGhostBlockItem(ItemStack item) {
        return getBlockType(item) != null;
    }

    public static Material resolveMaterial(String blockType) {
        if (blockType.startsWith("itemsadder:")) {
            SpectralBlocksPlugin plugin = SpectralBlocksPlugin.getInstance();
            org.bukkit.block.data.BlockData bd =
                    plugin.getItemsAdderIntegration().getBlockData(blockType);
            if (bd != null) return bd.getMaterial();
            return Material.MUSHROOM_STEM;
        }

        String clean = blockType;
        int bracket = clean.indexOf('[');
        if (bracket != -1) clean = clean.substring(0, bracket);
        clean = clean.replace("minecraft:", "").toUpperCase();
        Material mat = Material.matchMaterial(clean);
        return (mat != null && mat.isItem()) ? mat : Material.STONE;
    }
}

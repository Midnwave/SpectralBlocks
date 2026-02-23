package com.blockforge.spectralblocks.items;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class GhostRemoveTool {

    public static final String PDC_KEY = "remove_tool";
    public static NamespacedKey REMOVE_TOOL_KEY;

    public static void init(SpectralBlocksPlugin plugin) {
        REMOVE_TOOL_KEY = new NamespacedKey(plugin, PDC_KEY);
    }

    public static ItemStack create() {
        SpectralBlocksPlugin plugin = SpectralBlocksPlugin.getInstance();
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        String nameRaw = plugin.getMessagesManager().getRaw("remover-name");
        meta.displayName(plugin.getMessagesManager().getMiniMessage().deserialize(nameRaw));

        String loreRaw = plugin.getMessagesManager().getRaw("remover-lore");
        List<net.kyori.adventure.text.Component> lore = Arrays.stream(loreRaw.split("\n"))
                .map(line -> plugin.getMessagesManager().getMiniMessage().deserialize(line))
                .toList();
        meta.lore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        meta.getPersistentDataContainer().set(REMOVE_TOOL_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRemoveTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(REMOVE_TOOL_KEY, PersistentDataType.BYTE);
    }
}

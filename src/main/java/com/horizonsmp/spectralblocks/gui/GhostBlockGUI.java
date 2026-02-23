package com.horizonsmp.spectralblocks.gui;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import com.horizonsmp.spectralblocks.ghost.GhostBlock;
import com.horizonsmp.spectralblocks.ghost.GhostBlockManager;
import com.horizonsmp.spectralblocks.items.GhostBlockItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GhostBlockGUI implements InventoryHolder {

    public enum Tab { VANILLA, ITEMSADDER, MINE, SEARCH }

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int CONTENT_ROWS = 4;
    private static final int CONTENT_SIZE = CONTENT_ROWS * 9;

    private static final int SLOT_PREV = 36;
    private static final int SLOT_PAGE_INFO = 40;
    private static final int SLOT_NEXT = 44;
    private static final int SLOT_TAB_VANILLA = 46;
    private static final int SLOT_TAB_IA = 47;
    private static final int SLOT_TAB_MINE = 48;
    private static final int SLOT_TAB_SEARCH = 49;

    private final SpectralBlocksPlugin plugin;
    private final Player player;
    private Tab currentTab;
    private int currentPage;
    private List<ItemStack> currentContent = new ArrayList<>();
    private Inventory inventory;
    private String searchQuery = "";

    public GhostBlockGUI(SpectralBlocksPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.currentTab = Tab.VANILLA;
        this.currentPage = 0;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public void open() {
        Component title = plugin.getMessagesManager().getMiniMessage()
                .deserialize(plugin.getConfigManager().getGuiTitle());
        inventory = Bukkit.createInventory(this, SIZE, title);
        refresh();
        player.openInventory(inventory);
    }

    public void refresh() {
        inventory.clear();
        buildContent();
        renderContent();
        renderBorders();
        renderNavBar();
        renderTabBar();
    }

    private void buildContent() {
        currentContent.clear();
        switch (currentTab) {
            case VANILLA -> currentContent = VanillaBlocksTab.getPage(plugin, currentPage);
            case ITEMSADDER -> currentContent = ItemsAdderBlocksTab.getPage(plugin, currentPage);
            case MINE -> currentContent = MyGhostBlocksTab.getPage(plugin, player, currentPage);
            case SEARCH -> currentContent = SearchTab.getResults(plugin, searchQuery, currentPage);
        }
    }

    private void renderContent() {
        for (int i = 0; i < Math.min(currentContent.size(), CONTENT_SIZE); i++) {
            inventory.setItem(i, currentContent.get(i));
        }
    }

    private void renderBorders() {
        Material border = resolveBorderMaterial();
        ItemStack pane = buildBorderPane(border);

        for (int i = 36; i < 45; i++) {
            if (i != SLOT_PREV && i != SLOT_PAGE_INFO && i != SLOT_NEXT) {
                inventory.setItem(i, pane);
            }
        }
        for (int i = 45; i < 54; i++) {
            if (i != SLOT_TAB_VANILLA && i != SLOT_TAB_IA
                    && i != SLOT_TAB_MINE && i != SLOT_TAB_SEARCH) {
                inventory.setItem(i, pane);
            }
        }
    }

    private void renderNavBar() {
        int totalPages = getTotalPages();
        MiniMessage mm = plugin.getMessagesManager().getMiniMessage();

        if (currentPage > 0) {
            inventory.setItem(SLOT_PREV, buildNavButton(
                    Material.ARROW,
                    plugin.getMessagesManager().format("gui-prev-page")));
        }

        if (currentPage < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, buildNavButton(
                    Material.ARROW,
                    plugin.getMessagesManager().format("gui-next-page")));
        }

        Component pageInfo = plugin.getMessagesManager().format("gui-page-info",
                Placeholder.unparsed("page", String.valueOf(currentPage + 1)),
                Placeholder.unparsed("total", String.valueOf(Math.max(1, totalPages))));
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(pageInfo);
        meta.lore(List.of());
        info.setItemMeta(meta);
        inventory.setItem(SLOT_PAGE_INFO, info);
    }

    private void renderTabBar() {
        inventory.setItem(SLOT_TAB_VANILLA, buildTabButton(Tab.VANILLA,
                Material.GRASS_BLOCK, plugin.getMessagesManager().format("gui-tab-vanilla")));
        inventory.setItem(SLOT_TAB_IA, buildTabButton(Tab.ITEMSADDER,
                Material.MUSHROOM_STEM, plugin.getMessagesManager().format("gui-tab-itemsadder")));
        inventory.setItem(SLOT_TAB_MINE, buildTabButton(Tab.MINE,
                Material.ENDER_CHEST, plugin.getMessagesManager().format("gui-tab-mine")));
        inventory.setItem(SLOT_TAB_SEARCH, buildTabButton(Tab.SEARCH,
                Material.COMPASS, plugin.getMessagesManager().format("gui-tab-search")));
    }

    public void handleClick(int rawSlot, InventoryClickEvent event) {
        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= SIZE) return;

        if (rawSlot == SLOT_TAB_VANILLA) { switchTab(Tab.VANILLA); return; }
        if (rawSlot == SLOT_TAB_IA)      { switchTab(Tab.ITEMSADDER); return; }
        if (rawSlot == SLOT_TAB_MINE)    { switchTab(Tab.MINE); return; }
        if (rawSlot == SLOT_TAB_SEARCH)  { initiateSearch(); return; }

        if (rawSlot == SLOT_PREV && currentPage > 0) { currentPage--; refresh(); return; }
        if (rawSlot == SLOT_NEXT && currentPage < getTotalPages() - 1) { currentPage++; refresh(); return; }

        if (rawSlot < CONTENT_SIZE) {
            ItemStack clicked = inventory.getItem(rawSlot);
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (currentTab == Tab.MINE) {
                if (event.isRightClick()) {
                    handleMineRemove(clicked);
                }
            } else {
                handleGiveBlock(clicked);
            }
        }
    }

    private void handleGiveBlock(ItemStack clicked) {
        String blockType = GhostBlockItem.getBlockType(clicked);
        if (blockType == null) return;

        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessagesManager().send(player, "get-inventory-full");
            return;
        }

        player.getInventory().addItem(clicked.clone());
        plugin.getMessagesManager().send(player, "get-given",
                Placeholder.unparsed("block", blockType));
    }

    private void handleMineRemove(ItemStack clicked) {
        String blockType = GhostBlockItem.getBlockType(clicked);
        if (blockType == null) return;

        String locStr = clicked.getItemMeta().getPersistentDataContainer()
                .get(MyGhostBlocksTab.GHOST_LOCATION_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (locStr == null) return;

        String[] parts = locStr.split(",");
        if (parts.length != 4) return;
        String world = parts[0];
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);

        GhostBlock ghost = plugin.getGhostBlockManager().getAtLocation(world, x, y, z);
        if (ghost == null) {
            plugin.getMessagesManager().send(player, "remove-not-found");
            refresh();
            return;
        }

        org.bukkit.World bWorld = plugin.getServer().getWorld(world);
        if (bWorld == null) return;

        GhostBlockManager.RemoveResult result = plugin.getGhostBlockManager().remove(
                player, new org.bukkit.Location(bWorld, x, y, z), false);
        if (result == GhostBlockManager.RemoveResult.SUCCESS) {
            plugin.getMessagesManager().send(player, "removed");
        }
        refresh();
    }

    private void initiateSearch() {
        player.closeInventory();
        plugin.getMessagesManager().send(player, "gui-search-prompt-name");

        SearchTab.awaitInput(plugin, player, query -> {
            searchQuery = query;
            currentPage = 0;
            currentTab = Tab.SEARCH;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                GhostBlockGUI gui = new GhostBlockGUI(plugin, player);
                gui.searchQuery = query;
                gui.currentTab = Tab.SEARCH;
                gui.open();
            });
        });
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        currentPage = 0;
        refresh();
    }

    private int getTotalPages() {
        int total;
        switch (currentTab) {
            case VANILLA -> total = VanillaBlocksTab.getTotalBlocks(plugin);
            case ITEMSADDER -> total = ItemsAdderBlocksTab.getTotalBlocks(plugin);
            case MINE -> total = plugin.getGhostBlockManager().getByOwner(player.getUniqueId()).size();
            case SEARCH -> total = SearchTab.getTotalResults(plugin, searchQuery);
            default -> total = 0;
        }
        return Math.max(1, (int) Math.ceil((double) total / CONTENT_SIZE));
    }

    private ItemStack buildTabButton(Tab tab, Material mat, Component label) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(label);
        if (currentTab == tab) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavButton(Material mat, Component label) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(label);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBorderPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    private Material resolveBorderMaterial() {
        Material mat = Material.matchMaterial(plugin.getConfigManager().getBorderMaterial());
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }
}

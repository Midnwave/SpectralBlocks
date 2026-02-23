package com.blockforge.spectralblocks.gui;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.items.GhostBlockItem;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

public class SearchTab {

    private static final Map<UUID, Consumer<String>> awaiters = new HashMap<>();

    public static List<ItemStack> getResults(SpectralBlocksPlugin plugin, String query, int page) {
        if (query == null || query.isBlank()) return promptPage(plugin);

        List<ItemStack> results = new ArrayList<>();
        results.addAll(VanillaBlocksTab.search(plugin, query, page));

        int vanillaCount = VanillaBlocksTab.searchCount(plugin, query);
        int remaining = 36 - results.size();
        if (remaining > 0 && plugin.getItemsAdderIntegration().isEnabled()) {
            int iaStart = Math.max(0, page - (int) Math.ceil((double) vanillaCount / 36));
            results.addAll(ItemsAdderBlocksTab.search(plugin, query, Math.max(0, iaStart)));
        }

        if (results.isEmpty()) {
            return noResultsPage(plugin, query);
        }
        return results;
    }

    public static int getTotalResults(SpectralBlocksPlugin plugin, String query) {
        if (query == null || query.isBlank()) return 1;
        int count = VanillaBlocksTab.searchCount(plugin, query);
        count += ItemsAdderBlocksTab.searchCount(plugin, query);
        return count;
    }

    public static void awaitInput(SpectralBlocksPlugin plugin, Player player, Consumer<String> callback) {
        awaiters.put(player.getUniqueId(), callback);
        plugin.getMessagesManager().send(player, "gui-search-prompt-lore");
    }

    public static void clearAwaiter(UUID uuid) {
        awaiters.remove(uuid);
    }

    public static class ChatListener implements Listener {
        private final SpectralBlocksPlugin plugin;

        public ChatListener(SpectralBlocksPlugin plugin) {
            this.plugin = plugin;
        }

        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(AsyncPlayerChatEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            Consumer<String> callback = awaiters.remove(uuid);
            if (callback == null) return;

            event.setCancelled(true);
            String query = event.getMessage();
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(query));
        }
    }

    private static List<ItemStack> promptPage(SpectralBlocksPlugin plugin) {
        ItemStack prompt = new ItemStack(Material.COMPASS);
        ItemMeta meta = prompt.getItemMeta();
        meta.displayName(plugin.getMessagesManager().format("gui-search-prompt-name"));
        meta.lore(List.of(plugin.getMessagesManager().format("gui-search-prompt-lore")));
        prompt.setItemMeta(meta);
        return List.of(prompt);
    }

    private static List<ItemStack> noResultsPage(SpectralBlocksPlugin plugin, String query) {
        ItemStack notice = new ItemStack(Material.BARRIER);
        ItemMeta meta = notice.getItemMeta();
        meta.displayName(plugin.getMessagesManager().format("gui-no-results",
                Placeholder.unparsed("query", query)));
        notice.setItemMeta(meta);
        return List.of(notice);
    }
}

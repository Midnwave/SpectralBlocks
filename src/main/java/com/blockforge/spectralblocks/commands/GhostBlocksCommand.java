package com.blockforge.spectralblocks.commands;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.ghost.GhostBlockManager;
import com.blockforge.spectralblocks.gui.GhostBlockGUI;
import com.blockforge.spectralblocks.items.GhostBlockItem;
import com.blockforge.spectralblocks.items.GhostRemoveTool;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GhostBlocksCommand implements CommandExecutor {

    private final SpectralBlocksPlugin plugin;

    public GhostBlocksCommand(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> handleGui(sender);
            case "get" -> handleGet(sender, args);
            case "toggle" -> handleToggle(sender);
            case "remover" -> handleRemover(sender);
            case "admin" -> handleAdmin(sender, args);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> plugin.getMessagesManager().send(sender, "unknown-subcommand");
        }
        return true;
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("spectralblocks.gui")) {
            plugin.getMessagesManager().send(player, "no-permission");
            return;
        }
        new GhostBlockGUI(plugin, player).open();
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("spectralblocks.get")) {
            plugin.getMessagesManager().send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.getMessagesManager().send(player, "get-usage");
            return;
        }

        String blockId = args[1].toLowerCase();
        ItemStack item = resolveGhostBlockItem(blockId);
        if (item == null) {
            plugin.getMessagesManager().send(player, "get-unknown-block",
                    Placeholder.unparsed("block", blockId));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessagesManager().send(player, "get-inventory-full");
            return;
        }

        player.getInventory().addItem(item);
        plugin.getMessagesManager().send(player, "get-given",
                Placeholder.unparsed("block", blockId));
    }

    private ItemStack resolveGhostBlockItem(String blockId) {
        if (blockId.startsWith("itemsadder:")) {
            if (!plugin.getItemsAdderIntegration().isEnabled()
                    || !plugin.getItemsAdderIntegration().isDataLoaded()) return null;
            String id = blockId.substring("itemsadder:".length());
            var cb = plugin.getItemsAdderIntegration().getCustomBlock(id);
            if (cb == null) return null;
            Material mat = GhostBlockItem.resolveMaterial(blockId);
            return GhostBlockItem.create(mat, blockId, cb.getNamespacedID());
        } else {
            String matName = blockId.replace("minecraft:", "").toUpperCase();
            Material mat = Material.matchMaterial(matName);
            if (mat == null || !mat.isBlock() || mat.isAir()) return null;
            String fullId = "minecraft:" + matName.toLowerCase();
            String displayName = matName.toLowerCase().replace('_', ' ');
            return GhostBlockItem.create(mat, fullId, displayName);
        }
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("spectralblocks.toggle")) {
            plugin.getMessagesManager().send(player, "no-permission");
            return;
        }
        boolean enabled = plugin.getGhostBlockManager().toggleParticles(player.getUniqueId());
        plugin.getMessagesManager().send(player, enabled ? "toggle-particles-on" : "toggle-particles-off");
    }

    private void handleRemover(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("spectralblocks.use")) {
            plugin.getMessagesManager().send(player, "no-permission");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessagesManager().send(player, "get-inventory-full");
            return;
        }
        player.getInventory().addItem(GhostRemoveTool.create());
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectralblocks.admin")) {
            plugin.getMessagesManager().send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "remove" -> handleAdminRemove(sender, args);
            case "list"   -> handleAdminList(sender, args);
            case "reveal" -> handleAdminReveal(sender);
            default       -> sendAdminHelp(sender);
        }
    }

    private void handleAdminRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectralblocks.admin.remove")) {
            plugin.getMessagesManager().send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /ghostblocks admin remove <player>");
            return;
        }
        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = null;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            var offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                targetUuid = offlinePlayer.getUniqueId();
                targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : targetName;
            }
        }

        if (targetUuid == null) {
            plugin.getMessagesManager().send(sender, "admin-player-not-found",
                    Placeholder.unparsed("player", args[2]));
            return;
        }

        Player adminPlayer = (sender instanceof Player p) ? p : null;
        int count = plugin.getGhostBlockManager().removeAllByOwner(targetUuid, adminPlayer);

        if (count == 0) {
            plugin.getMessagesManager().send(sender, "admin-removed-none",
                    Placeholder.unparsed("player", targetName));
        } else {
            plugin.getMessagesManager().send(sender, "admin-removed-player",
                    Placeholder.unparsed("player", targetName),
                    Placeholder.unparsed("count", String.valueOf(count)));
        }
    }

    private void handleAdminList(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /ghostblocks admin list <player>");
            return;
        }
        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid;
        String resolvedName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            resolvedName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            var offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            targetUuid = offlinePlayer.getUniqueId();
            resolvedName = offlinePlayer.getName() != null ? offlinePlayer.getName() : targetName;
        }

        var blocks = plugin.getGhostBlockManager().getByOwner(targetUuid);
        plugin.getMessagesManager().send(sender, "admin-list-header",
                Placeholder.unparsed("player", resolvedName),
                Placeholder.unparsed("count", String.valueOf(blocks.size())));

        for (var ghost : blocks) {
            plugin.getMessagesManager().send(sender, "admin-list-entry",
                    Placeholder.unparsed("block", ghost.getBlockType()),
                    Placeholder.unparsed("world", ghost.getWorld()),
                    Placeholder.unparsed("x", String.valueOf(ghost.getX())),
                    Placeholder.unparsed("y", String.valueOf(ghost.getY())),
                    Placeholder.unparsed("z", String.valueOf(ghost.getZ())));
        }
    }

    private void handleAdminReveal(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("spectralblocks.admin.seeghost")) {
            plugin.getMessagesManager().send(player, "no-permission");
            return;
        }
        boolean on = plugin.getGhostBlockManager().toggleAdminReveal(player.getUniqueId());
        plugin.getMessagesManager().send(player, on ? "admin-reveal-on" : "admin-reveal-off");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("spectralblocks.admin.reload")) {
            plugin.getMessagesManager().send(sender, "no-permission");
            return;
        }
        plugin.getConfigManager().load();
        plugin.getMessagesManager().load();
        plugin.getMessagesManager().send(sender, "reload-success");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gradient:#7B68EE:#00CED1><bold>SpectralBlocks</bold></gradient> <gray>Commands:</gray>"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks gui</gold> <gray>— Open the ghost blocks GUI"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks get <minecraft:block|itemsadder:ns:id></gold> <gray>— Get a ghost block item"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks toggle</gold> <gray>— Toggle your particle shimmer"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks remover</gold> <gray>— Get the ghost remove tool"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks admin <remove|list|reveal></gold> <gray>— Admin commands"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks reload</gold> <gray>— Reload config"));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gradient:#7B68EE:#00CED1>SpectralBlocks Admin Commands</gradient>"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks admin remove <player></gold> <gray>— Remove all ghost blocks for a player"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks admin list <player></gold> <gray>— List ghost blocks for a player"));
        sender.sendMessage(plugin.getMessagesManager().getMiniMessage().deserialize(
                "<gold>/ghostblocks admin reveal</gold> <gray>— Toggle seeing all ghost block shimmers"));
    }
}

package com.blockforge.spectralblocks.commands;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.gui.VanillaBlocksTab;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GhostBlocksTabCompleter implements TabCompleter {

    private final SpectralBlocksPlugin plugin;

    public GhostBlocksTabCompleter(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("help");
            if (sender.hasPermission("spectralblocks.gui")) subs.add("gui");
            if (sender.hasPermission("spectralblocks.get")) subs.add("get");
            if (sender.hasPermission("spectralblocks.toggle")) subs.add("toggle");
            if (sender.hasPermission("spectralblocks.use")) subs.add("remover");
            if (sender.hasPermission("spectralblocks.admin")) subs.add("admin");
            if (sender.hasPermission("spectralblocks.admin.reload")) subs.add("reload");
            return filterPrefix(subs, args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "get" -> sender.hasPermission("spectralblocks.get") && args.length == 2 ? completeBlockId(args[1]) : List.of();
            case "admin" -> completeAdmin(sender, args);
            default -> List.of();
        };
    }

    private List<String> completeAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectralblocks.admin")) return List.of();

        if (args.length == 2) {
            return filterPrefix(Arrays.asList("remove", "list", "reveal"), args[1]);
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("list"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private List<String> completeBlockId(String partial) {
        List<String> suggestions = new ArrayList<>();
        String lower = partial.toLowerCase();

        for (Material mat : VanillaBlocksTab.getAllBlocks(plugin)) {
            String id = "minecraft:" + mat.name().toLowerCase();
            if (id.startsWith(lower) || (lower.startsWith("m") && id.contains(lower.replace("minecraft:", "")))) {
                suggestions.add(id);
            }
        }

        if (plugin.getItemsAdderIntegration().isEnabled()
                && plugin.getItemsAdderIntegration().isDataLoaded()) {
            for (String iaId : plugin.getItemsAdderIntegration().getAllBlockIds()) {
                if (iaId.startsWith(lower)) suggestions.add(iaId);
            }
        }

        if (plugin.getNexoIntegration().isEnabled()
                && plugin.getNexoIntegration().isDataLoaded()) {
            for (String nexoId : plugin.getNexoIntegration().getAllBlockIds()) {
                if (nexoId.startsWith(lower)) suggestions.add(nexoId);
            }
        }

        return suggestions.stream()
                .filter(s -> s.startsWith(lower))
                .sorted()
                .limit(50)
                .collect(Collectors.toList());
    }

    private List<String> filterPrefix(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        return options.stream()
                .filter(s -> s.startsWith(lower))
                .collect(Collectors.toList());
    }
}

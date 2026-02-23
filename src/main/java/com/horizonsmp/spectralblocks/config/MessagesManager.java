package com.horizonsmp.spectralblocks.config;

import com.horizonsmp.spectralblocks.SpectralBlocksPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessagesManager {

    private final SpectralBlocksPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration messages;

    public MessagesManager(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        String raw = getRaw(key);
        if (raw == null || raw.isBlank()) return;
        TagResolver prefix = Placeholder.component("prefix", format("prefix"));
        TagResolver[] all = new TagResolver[resolvers.length + 1];
        all[0] = prefix;
        System.arraycopy(resolvers, 0, all, 1, resolvers.length);
        sender.sendMessage(mm.deserialize(raw, all));
    }

    public Component format(String key, TagResolver... resolvers) {
        String raw = getRaw(key);
        if (raw == null || raw.isBlank()) return Component.empty();
        if (resolvers.length == 0) return mm.deserialize(raw);
        return mm.deserialize(raw, resolvers);
    }

    public String getRaw(String key) {
        return messages.getString(key, "");
    }

    public MiniMessage getMiniMessage() {
        return mm;
    }
}

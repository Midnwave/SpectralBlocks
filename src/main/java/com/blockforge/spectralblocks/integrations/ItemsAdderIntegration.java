package com.blockforge.spectralblocks.integrations;

import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemsAdderIntegration {

    private boolean enabled;
    private boolean dataLoaded = false;

    public ItemsAdderIntegration() {
        try {
            Class.forName("dev.lone.itemsadder.api.CustomBlock");
            this.enabled = true;
        } catch (ClassNotFoundException ignored) {
            this.enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDataLoaded() {
        return dataLoaded;
    }

    public void markDataLoaded() {
        this.dataLoaded = true;
    }

    public Collection<String> getAllCustomBlockIds() {
        if (!enabled || !dataLoaded) return List.of();
        try {
            return CustomBlock.getNamespacedIdsInRegistry();
        } catch (Exception e) {
            return List.of();
        }
    }

    public CustomBlock getCustomBlock(String namespaceId) {
        if (!enabled || !dataLoaded) return null;
        try {
            String id = namespaceId.startsWith("itemsadder:") ? namespaceId.substring("itemsadder:".length()) : namespaceId;
            return CustomBlock.getInstance(id);
        } catch (Exception e) {
            return null;
        }
    }

    public BlockData getBlockData(String namespaceId) {
        if (!enabled || !dataLoaded) return null;
        try {
            String id = namespaceId.startsWith("itemsadder:") ? namespaceId.substring("itemsadder:".length()) : namespaceId;
            CustomBlock cb = CustomBlock.getInstance(id);
            if (cb == null) return null;
            return cb.getBaseBlockData();
        } catch (Exception e) {
            return null;
        }
    }

    public String getBlockDataString(String namespaceId) {
        BlockData bd = getBlockData(namespaceId);
        return bd != null ? bd.getAsString() : null;
    }

    public List<String> getAllBlockIds() {
        if (!enabled || !dataLoaded) return List.of();
        List<String> ids = new ArrayList<>();
        try {
            for (String nsId : CustomBlock.getNamespacedIdsInRegistry()) {
                ids.add("itemsadder:" + nsId);
            }
        } catch (Exception ignored) {}
        return ids;
    }
}

package com.blockforge.spectralblocks.integrations;

import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

public class NexoIntegration {

    private boolean enabled;
    private boolean dataLoaded = false;

    public NexoIntegration() {
        try {
            Class.forName("com.nexomc.nexo.api.NexoBlocks");
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

    public String[] getAllCustomBlockIds() {
        if (!enabled || !dataLoaded) return new String[0];
        try {
            return NexoBlocks.blockIDs();
        } catch (Exception e) {
            return new String[0];
        }
    }

    public BlockData getBlockData(String namespaceId) {
        if (!enabled || !dataLoaded) return null;
        try {
            String id = namespaceId.startsWith("nexo:") ? namespaceId.substring("nexo:".length()) : namespaceId;
            return NexoBlocks.blockData(id);
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
            for (String blockId : NexoBlocks.blockIDs()) {
                ids.add("nexo:" + blockId);
            }
        } catch (Exception ignored) {}
        return ids;
    }
}

package com.blockforge.spectralblocks.listeners;

import com.blockforge.spectralblocks.SpectralBlocksPlugin;
import com.blockforge.spectralblocks.ghost.GhostBlockManager;
import com.blockforge.spectralblocks.items.GhostBlockItem;
import com.blockforge.spectralblocks.items.GhostRemoveTool;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final SpectralBlocksPlugin plugin;

    public PlayerInteractListener(SpectralBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        var player = event.getPlayer();

        if (GhostRemoveTool.isRemoveTool(item)) {
            event.setCancelled(true);
            Block target = getPlacementBlock(clickedBlock, event.getBlockFace());
            if (target == null) return;

            GhostBlockManager.RemoveResult result = plugin.getGhostBlockManager().remove(
                    player, target.getLocation(), false);
            switch (result) {
                case SUCCESS -> plugin.getMessagesManager().send(player, "removed");
                case NOT_FOUND -> plugin.getMessagesManager().send(player, "remove-not-found");
                case NOT_YOURS -> plugin.getMessagesManager().send(player, "remove-not-yours");
            }
            return;
        }

        String blockType = GhostBlockItem.getBlockType(item);
        if (blockType == null) return;

        if (!player.hasPermission("spectralblocks.use")) {
            plugin.getMessagesManager().send(player, "no-permission");
            event.setCancelled(true);
            return;
        }

        // let normal interactions through when not sneaking
        if (!player.isSneaking() && isInteractable(clickedBlock.getType())) {
            return;
        }

        event.setCancelled(true);

        Block target = getPlacementBlock(clickedBlock, event.getBlockFace());
        if (target == null) return;

        String finalBlockData = resolveBlockData(blockType, player, event.getBlockFace(), target.getLocation());

        GhostBlockManager.PlaceResult result = plugin.getGhostBlockManager().place(
                player, target.getLocation(), finalBlockData);

        switch (result) {
            case SUCCESS -> {
                plugin.getMessagesManager().send(player, "placed",
                        Placeholder.unparsed("x", String.valueOf(target.getX())),
                        Placeholder.unparsed("y", String.valueOf(target.getY())),
                        Placeholder.unparsed("z", String.valueOf(target.getZ())));
                playPlacementSound(target.getLocation(), finalBlockData);
            }
            case WORLD_DENIED -> plugin.getMessagesManager().send(player, "place-world-denied");
            case REGION_DENIED -> plugin.getMessagesManager().send(player, "place-region-denied");
            case COOLDOWN -> plugin.getMessagesManager().send(player, "place-cooldown");
            case LIMIT_REACHED -> plugin.getMessagesManager().send(player, "place-limit-reached",
                    Placeholder.unparsed("limit",
                            String.valueOf(plugin.getGhostBlockManager().resolveLimit(player))));
            case OCCUPIED -> plugin.getMessagesManager().send(player, "place-occupied");
        }
    }

    private Block getPlacementBlock(Block clicked, BlockFace face) {
        return clicked.getRelative(face);
    }

    private String resolveBlockData(String blockType, org.bukkit.entity.Player player,
                                     BlockFace clickedFace, Location targetLoc) {
        if (blockType.startsWith("itemsadder:")) return blockType;

        String baseType = blockType;
        int bracket = baseType.indexOf('[');
        if (bracket != -1) baseType = baseType.substring(0, bracket);
        baseType = baseType.replace("minecraft:", "").toUpperCase();

        Material mat = Material.matchMaterial(baseType);
        if (mat == null) return blockType;

        BlockData bd = mat.createBlockData();

        if (bd instanceof Directional dir) {
            dir.setFacing(getHorizontalFacing(player));
        }

        if (bd instanceof Stairs stairs) {
            stairs.setFacing(getHorizontalFacing(player));
            stairs.setHalf(clickedFace == BlockFace.DOWN
                    ? Bisected.Half.TOP
                    : Bisected.Half.BOTTOM);
        }

        if (bd instanceof Slab slab) {
            slab.setType(clickedFace == BlockFace.DOWN ? Slab.Type.TOP : Slab.Type.BOTTOM);
        }

        if (bd instanceof Bisected bis && !(bd instanceof Stairs)) {
            bis.setHalf(clickedFace == BlockFace.DOWN
                    ? Bisected.Half.TOP
                    : Bisected.Half.BOTTOM);
        }

        if (bd instanceof Rotatable rot) {
            rot.setRotation(getHorizontalFacing(player));
        }

        return bd.getAsString();
    }

    private BlockFace getHorizontalFacing(org.bukkit.entity.Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private void playPlacementSound(Location location, String blockType) {
        if (blockType.startsWith("itemsadder:")) return;
        String base = blockType;
        int b = base.indexOf('[');
        if (b != -1) base = base.substring(0, b);
        Material mat = Material.matchMaterial(base.replace("minecraft:", "").toUpperCase());
        if (mat == null) return;

        Sound sound = mat.createBlockData().getSoundGroup().getPlaceSound();
        if (sound != null) {
            location.getWorld().playSound(location, sound, 1f, 1f);
        }
    }

    private boolean isInteractable(Material material) {
        if (!material.isInteractable()) return false;
        String name = material.name();
        if (name.contains("STAINED_GLASS")) return false;
        if (name.contains("LEAVES")) return false;
        if (name.contains("PLANKS")) return false;
        if (name.contains("SLAB")) return false;
        if (name.contains("STAIRS")) return false;
        if (name.contains("_WALL") && !name.contains("SIGN")) return false;
        return true;
    }
}

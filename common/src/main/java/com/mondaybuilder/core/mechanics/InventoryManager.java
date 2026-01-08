package com.mondaybuilder.core.mechanics;

import com.mondaybuilder.config.ConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public class InventoryManager {
    private final Map<UUID, Map<Item, Integer>> pendingShrinks = new HashMap<>();

    private List<String> getItemsPool() {
        List<String> pool = ConfigManager.items.pool;
        if (pool == null || pool.isEmpty()) {
            return Arrays.asList(
                "minecraft:white_wool",
                "minecraft:yellow_wool",
                "minecraft:orange_wool",
                "minecraft:pink_wool",
                "minecraft:red_wool",
                "minecraft:lime_wool",
                "minecraft:light_gray_wool",
                "minecraft:black_wool",
                "minecraft:brown_wool"
            );
        }
        return pool;
    }

    public void giveStartingItems(ServerPlayer player, int amount) {
        getItemsPool().forEach(id -> {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).ifPresent(item -> {
                player.getInventory().add(new ItemStack(item, amount));
            });
        });
        player.containerMenu.broadcastChanges();
    }

    /**
     * Queues an item to be shrunk in the next tick.
     * This avoids scheduling multiple tasks for rapid block placement.
     */
    public void queueShrink(ServerPlayer player, Item item) {
        synchronized (pendingShrinks) {
            pendingShrinks.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                          .merge(item, 1, Integer::sum);
        }
    }

    public boolean isBuildingBlock(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        return getItemsPool().contains(key.toString());
    }

    /**
     * Processes pending shrinks and optionally performs inventory sanitization.
     */
    public void tick(MinecraftServer server, List<UUID> activePlayers, boolean sanitize) {
        processShrinks(server);
        if (sanitize) {
            sanitizeInventories(server, activePlayers);
        }
    }

    private void processShrinks(MinecraftServer server) {
        Map<UUID, Map<Item, Integer>> toProcess;
        synchronized (pendingShrinks) {
            if (pendingShrinks.isEmpty()) return;
            toProcess = new HashMap<>(pendingShrinks);
            pendingShrinks.clear();
        }

        toProcess.forEach((uuid, items) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                items.forEach((item, count) -> {
                    shrinkItem(player, item, count);
                });
            }
        });
    }

    private void shrinkItem(ServerPlayer player, Item item, int count) {
        int remaining = count;
        
        // Priority 1: Main hand
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem().equals(item)) {
            int toShrink = Math.min(mainHand.getCount(), remaining);
            mainHand.shrink(toShrink);
            remaining -= toShrink;
        }

        // Priority 2: Inventory scan
        if (remaining > 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem().equals(item)) {
                    int toShrink = Math.min(stack.getCount(), remaining);
                    stack.shrink(toShrink);
                    remaining -= toShrink;
                    if (remaining <= 0) break;
                }
            }
        }
        
        player.containerMenu.broadcastChanges();

        // Check if inventory is empty
        if (player.getInventory().isEmpty()) {
            com.mondaybuilder.core.GameManager.getInstance().getScoring().grantAdvancement(
                ((ServerLevel)player.level()).getServer(), 
                player, 
                ResourceLocation.fromNamespaceAndPath("mondaybuilder", "used_all_blocks")
            );
        }
    }

    private void sanitizeInventories(MinecraftServer server, List<UUID> activePlayers) {
        for (UUID uuid : activePlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null && p.isCreative()) {
                // Force close any menu to prevent using the full creative inventory screen
                if (p.containerMenu != p.inventoryMenu) {
                    p.closeContainer();
                }
                
                // Sanitization: Remove any non-building items. 
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    if (!stack.isEmpty() && !isBuildingBlock(stack.getItem())) {
                        p.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }
}

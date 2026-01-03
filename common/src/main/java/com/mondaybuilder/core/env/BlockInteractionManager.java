package com.mondaybuilder.core.env;

import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.RoundContext;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import java.util.UUID;

public class BlockInteractionManager {
    private final ArenaManager arena;

    public BlockInteractionManager(ArenaManager arena) {
        this.arena = arena;
    }

    public EventResult onBlockBreak(Level level, BlockPos pos, ServerPlayer player, GameState state, RoundContext currentRound) {
        if (state == GameState.BUILDING && currentRound != null && 
            com.mondaybuilder.core.GameManager.getInstance().getPlayerRole(player.getUUID()) == com.mondaybuilder.core.session.PlayerRole.BUILDER) {
            
            // ONLY the builder can break blocks, and only within the stage area
            if (arena.isWithinStage(level, pos)) {
                BlockState brokenState = level.getBlockState(pos);
                Item brokenItem = brokenState.getBlock().asItem();

                // Restriction: ONLY handle building blocks
                if (com.mondaybuilder.core.GameManager.getInstance().getInventoryManager().isBuildingBlock(brokenItem)) {
                    arena.removePlacedBlock(pos);
                    
                    boolean found = false;
                    ItemStack mainHand = player.getMainHandItem();
                    
                    // Priority 1: Main hand
                    if (!mainHand.isEmpty() && mainHand.getItem().equals(brokenItem)) {
                        mainHand.setCount(mainHand.getCount() + 1);
                        found = true;
                    }

                    if (!found) {
                        // Priority 2: Inventory scan
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (!stack.isEmpty() && stack.getItem().equals(brokenItem)) {
                                stack.setCount(stack.getCount() + 1);
                                found = true;
                                break;
                            }
                        }
                    }
                    
                    if (!found) {
                        // Fix: If the inventory does not contain the block (last block placed), add one
                        player.getInventory().add(new ItemStack(brokenItem, 1));
                    }
                    
                    player.containerMenu.broadcastChanges();
                    return EventResult.pass();
                } else {
                    // Prevent breaking non-building blocks (like floor/walls) and prevent getting other blocks
                    return EventResult.interruptFalse();
                }
            }
        }
        return EventResult.interruptFalse();
    }

    public InteractionResult onLeftClickBlock(ServerPlayer player, BlockPos pos, GameState state, RoundContext currentRound) {
        if (state != GameState.BUILDING || currentRound == null) return InteractionResult.FAIL;
        
        // ONLY the builder can interact with blocks
        if (com.mondaybuilder.core.GameManager.getInstance().getPlayerRole(player.getUUID()) != com.mondaybuilder.core.session.PlayerRole.BUILDER) return InteractionResult.FAIL;
        
        // If outside stage or below the stage floor, block it.
        if (!arena.isWithinStage(player.level(), pos) || pos.getY() < ConfigManager.map.stageArea.y1) {
            return InteractionResult.FAIL;
        }

        // Return PASS to allow Creative mode's native instant break logic to take over.
        // This prevents the "sweeping" bug caused by manual server-side block removal.
        return InteractionResult.PASS;
    }

    public EventResult onBlockPlace(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, GameState state, RoundContext currentRound) {
        if (state != GameState.BUILDING || currentRound == null) return EventResult.interruptFalse();
        if (com.mondaybuilder.core.GameManager.getInstance().getPlayerRole(player.getUUID()) != com.mondaybuilder.core.session.PlayerRole.BUILDER) return EventResult.interruptFalse();
        
        // Ensure building is ONLY allowed while the round timer is actually running
        if (!currentRound.getTimer().isRunning()) {
            return EventResult.interruptFalse();
        }

        // Allow building ONLY within the stage area (including the floor Y level for placing ON it)
        if (!arena.isWithinStage(level, pos)) {
             return EventResult.interruptFalse();
        }
        
        // Restriction: ONLY allow building blocks to be placed to prevent Creative inventory abuse.
        if (!com.mondaybuilder.core.GameManager.getInstance().getInventoryManager().isBuildingBlock(blockState.getBlock().asItem())) {
            return EventResult.interruptFalse();
        }

        arena.addPlacedBlock(pos);

        // Faked survival behavior in Creative: decrement the block count when placed
        // In Creative mode, vanilla restores the item stack after placement, so we must shrink it in the next tick.
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem().equals(blockState.getBlock().asItem())) {
            com.mondaybuilder.core.GameManager.getInstance().getInventoryManager().queueShrink(player, mainHand.getItem());
            return EventResult.pass();
        }

        // Fallback to inventory scan ONLY if not in main hand
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem().equals(blockState.getBlock().asItem())) {
                com.mondaybuilder.core.GameManager.getInstance().getInventoryManager().queueShrink(player, stack.getItem());
                return EventResult.pass();
            }
        }

        // If for some reason the block isn't in their inventory, don't let them place it
        return EventResult.interruptFalse();
    }
}

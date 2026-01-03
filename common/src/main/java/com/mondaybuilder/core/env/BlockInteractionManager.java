package com.mondaybuilder.core.env;

import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.RoundContext;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
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
            if (arena.isWithinStage(level, pos) && pos.getY() >= ConfigManager.map.stageArea.y1) {
                arena.removePlacedBlock(pos);
                // Faked survival behavior in Creative: increment the block count when broken
                BlockState brokenState = level.getBlockState(pos);
                if (brokenState.getBlock().getDescriptionId().contains("wool")) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && stack.getItem().equals(brokenState.getBlock().asItem())) {
                            stack.grow(1);
                            player.getInventory().setItem(i, stack);
                            player.containerMenu.broadcastChanges();
                            break;
                        }
                    }
                }
                return EventResult.pass();
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

        arena.addPlacedBlock(pos);
        
        // Restriction: ONLY allow Wool blocks to be placed to prevent Creative inventory abuse.
        if (!blockState.getBlock().getDescriptionId().contains("wool")) {
            return EventResult.interruptFalse();
        }

        // Faked survival behavior in Creative: decrement the block count when placed
        // In Creative mode, vanilla restores the item stack after placement, so we must shrink it in the next tick.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem().equals(blockState.getBlock().asItem())) {
                final int slot = i;
                final net.minecraft.world.item.Item expectedItem = stack.getItem();
                
                com.mondaybuilder.core.GameManager.getInstance().scheduleTask(() -> {
                    ItemStack currentStack = player.getInventory().getItem(slot);
                    if (!currentStack.isEmpty() && currentStack.getItem().equals(expectedItem)) {
                        currentStack.shrink(1);
                        player.getInventory().setItem(slot, currentStack);
                        player.containerMenu.broadcastChanges();
                    }
                });
                return EventResult.pass();
            }
        }

        // If for some reason the block isn't in their inventory, don't let them place it
        return EventResult.interruptFalse();
    }
}

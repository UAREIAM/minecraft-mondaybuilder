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

public class BlockInteractionManager {
    private final ArenaManager arena;

    public BlockInteractionManager(ArenaManager arena) {
        this.arena = arena;
    }

    public EventResult onBlockBreak(Level level, BlockPos pos, ServerPlayer player, GameState state, RoundContext currentRound) {
        if (state != GameState.BUILDING || currentRound == null) return EventResult.pass();
        if (!player.getUUID().equals(currentRound.getBuilder())) return EventResult.interruptFalse();
        if (arena.isFloor(pos)) return EventResult.interruptFalse();

        BlockState blockState = level.getBlockState(pos);
        if (blockState.is(Blocks.BARRIER)) return EventResult.interruptFalse();

        // Give block back to builder
        if (!blockState.isAir()) {
            player.getInventory().add(blockState.getBlock().asItem().getDefaultInstance());
        }

        return EventResult.pass();
    }

    public InteractionResult onLeftClickBlock(ServerPlayer player, BlockPos pos, GameState state, RoundContext currentRound) {
        if (state != GameState.BUILDING || currentRound == null) return InteractionResult.PASS;
        if (!player.getUUID().equals(currentRound.getBuilder())) return InteractionResult.PASS;
        if (arena.isFloor(pos)) return InteractionResult.PASS;

        Level level = player.level();
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir() || blockState.is(Blocks.BARRIER)) return InteractionResult.PASS;

        player.getInventory().add(blockState.getBlock().asItem().getDefaultInstance());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return InteractionResult.SUCCESS;
    }

    public EventResult onBlockPlace(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, GameState state, RoundContext currentRound) {
        if (state != GameState.BUILDING || currentRound == null) return EventResult.pass();
        if (!player.getUUID().equals(currentRound.getBuilder())) return EventResult.interruptFalse();
        
        // Ensure building is ONLY allowed while the round timer is actually running
        if (!currentRound.getTimer().isRunning()) {
            return EventResult.interruptFalse();
        }

        // Allow building ONLY within the stage area (including the floor Y level)
        if (!arena.isWithinStage(level, pos)) {
             return EventResult.interruptFalse();
        }
        
        if (!level.getBlockState(pos).isAir()) return EventResult.interruptFalse();

        return EventResult.pass();
    }
}

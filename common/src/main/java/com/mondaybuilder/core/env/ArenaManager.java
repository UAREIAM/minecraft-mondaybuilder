package com.mondaybuilder.core.env;

import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArenaManager {
    private final Set<BlockPos> placedBlocks = new HashSet<>();
    private ResourceKey<Level> cachedStageWorldKey;
    
    // Pre-calculated bounds for performance
    private int minX, maxX, minY, maxY, minZ, maxZ;

    public void onServerStarted(MinecraftServer server) {
        ModConfig.Area stage = ConfigManager.map.stageArea;
        cachedStageWorldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(stage.world));

        // Pre-calculate bounds
        minX = (int) Math.min(stage.x1, stage.x2);
        maxX = (int) Math.max(stage.x1, stage.x2);
        minY = (int) Math.min(stage.y1, stage.y2);
        maxY = (int) Math.max(stage.y1, stage.y2);
        minZ = (int) Math.min(stage.z1, stage.z2);
        maxZ = (int) Math.max(stage.z1, stage.z2);

        ModConfig.Area area = ConfigManager.map.joiningArea;
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(area.world));
        ServerLevel level = server.getLevel(worldKey);
        if (level != null) {
            int x = (int)((area.x1 + area.x2) / 2.0);
            int y = (int)area.y1 + 1;
            int z = (int)((area.z1 + area.z2) / 2.0);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), 
                String.format("setworldspawn %d %d %d 0", x, y, z));
            server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS).set(0, server);
        }

        // Also ensure building world has no spawn protection issues
        ResourceKey<Level> stageWorldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(stage.world));
        ServerLevel stageLevel = server.getLevel(stageWorldKey);
        if (stageLevel != null) {
             // Move world spawn far away from stage area if it's in the same world
             if (stage.world.equals(area.world)) {
                 // Already set above, but maybe it's too close?
             } else {
                 server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(stageLevel).withSuppressedOutput(), 
                    "setworldspawn 1000 100 1000 0");
             }
        }
    }

    public void addPlacedBlock(BlockPos pos) {
        placedBlocks.add(pos.immutable());
    }

    public void removePlacedBlock(BlockPos pos) {
        placedBlocks.remove(pos);
    }

    public void clearPlacedBlocks() {
        placedBlocks.clear();
    }

    public void cleanupStage(MinecraftServer server) {
        ResourceKey<Level> worldKey = cachedStageWorldKey != null ? cachedStageWorldKey : ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(ConfigManager.map.stageArea.world));
        ServerLevel level = server.getLevel(worldKey);
        if (level == null) return;

        // If we have tracked blocks, clear ONLY those for maximum performance
        if (!placedBlocks.isEmpty()) {
            for (BlockPos pos : placedBlocks) {
                if (level.getBlockState(pos).isAir()) continue;
                // Use flag 2 (UPDATE_CLIENTS) to avoid neighbor updates and lighting re-calculations for every single block
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            placedBlocks.clear();
            // Final update to sync all changes to clients
            level.getChunkSource().getLightEngine().checkBlock(new BlockPos(0,0,0)); // Dummy trigger or rely on flag 2
            return;
        }

        // Fallback: Clear EVERYTHING within the stage area (legacy/failsafe)
        // This is only called if tracking was lost or at first round start
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.BARRIER)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }


    public void teleport(ServerPlayer player, ModConfig.Location loc) {
        ResourceKey<Level> worldKey;
        if (cachedStageWorldKey != null && loc.world.equals(ConfigManager.map.stageArea.world)) {
            worldKey = cachedStageWorldKey;
        } else {
            worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.world));
        }
        ServerLevel level = ((ServerLevel)player.level()).getServer().getLevel(worldKey);
        if (level != null) {
            player.teleportTo(level, loc.x, loc.y, loc.z, Collections.<Relative>emptySet(), (float)loc.yaw, (float)loc.pitch, true);
        }
    }

    public void teleportToLobby(ServerPlayer player) {
        teleport(player, ConfigManager.map.lobby);
    }

    public void teleportToArena(ServerPlayer player) {
        teleport(player, ConfigManager.map.arenaLobby);
    }

    public void teleportToStage(ServerPlayer player) {
        teleport(player, ConfigManager.map.stage);
    }

    public boolean isWithinStage(Level level, BlockPos pos) {
        if (cachedStageWorldKey != null && !level.dimension().equals(cachedStageWorldKey)) {
            return false;
        }
        
        // Optimization: Use pre-calculated bounds
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ &&
               y >= ConfigManager.map.stageArea.y1;
    }

    public boolean isFloor(BlockPos pos) {
        // Floor is strictly BELOW the build area (y < y1)
        return pos.getY() < ConfigManager.map.stageArea.y1;
    }
}

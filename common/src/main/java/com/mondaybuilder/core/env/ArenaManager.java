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
import java.util.Collections;
import java.util.Set;

public class ArenaManager {

    public void onServerStarted(MinecraftServer server) {
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
        ModConfig.Area stage = ConfigManager.map.stageArea;
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

    public void cleanupStage(MinecraftServer server) {
        ModConfig.Area stage = ConfigManager.map.stageArea;
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(stage.world));
        ServerLevel level = server.getLevel(worldKey);
        if (level == null) return;

        int minX = (int) Math.min(stage.x1, stage.x2);
        int maxX = (int) Math.max(stage.x1, stage.x2);
        int minY = (int) Math.min(stage.y1, stage.y2);
        int maxY = (int) Math.max(stage.y1, stage.y2);
        int minZ = (int) Math.min(stage.z1, stage.z2);
        int maxZ = (int) Math.max(stage.z1, stage.z2);

        // Clear EVERYTHING within the stage area including the base Y level (y1)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    public void setupBoundaries(ServerLevel level) {
        ModConfig.Area stage = ConfigManager.map.stageArea;
        for (int x = (int)stage.x1; x <= (int)stage.x2; x++) {
            for (int y = (int)stage.y1; y <= (int)stage.y2; y++) {
                for (int z = (int)stage.z1; z <= (int)stage.z2; z++) {
                    if (x == (int)stage.x1 || x == (int)stage.x2 || z == (int)stage.z1 || z == (int)stage.z2 || y == (int)stage.y2) {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState());
                    }
                }
            }
        }
    }

    public void teleport(ServerPlayer player, ModConfig.Location loc) {
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.world));
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
        ModConfig.Area stage = ConfigManager.map.stageArea;
        // Use floating point comparison to ensure precision
        String world = level.dimension().location().toString();
        return stage.contains(world, pos.getX(), pos.getY(), pos.getZ()) && pos.getY() >= stage.y1;
    }

    public boolean isFloor(BlockPos pos) {
        // Floor is strictly BELOW the build area (y < y1)
        return pos.getY() < ConfigManager.map.stageArea.y1;
    }
}

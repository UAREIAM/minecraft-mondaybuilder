package com.mondaybuilder;

import com.mondaybuilder.commands.ModCommands;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.events.ModEvents;
import com.mondaybuilder.registry.ModSounds;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MondayBuilder {
    public static final String MOD_ID = "mondaybuilder";

    public static void init() {
        ConfigManager.loadAll();
        ModSounds.register();

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            ModCommands.register(dispatcher);
        });

        LifecycleEvent.SERVER_STARTED.register(server -> {
            GameManager.getInstance().onServerStarted(server);
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            GameManager.getInstance().onPlayerJoinServer(player);
        });

        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            GameManager.getInstance().onPlayerRespawn(player);
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            GameManager.getInstance().removePlayer(player.getUUID(), ((ServerLevel)player.level()).getServer());
        });

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                ModEvents.PLAYER_DEATH.invoker().onDeath(player);
                GameManager.getInstance().onPlayerDeath(player);
            }
            return EventResult.pass();
        });

        TickEvent.SERVER_POST.register(server -> {
            GameManager.getInstance().tick(server);
        });

        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getFoodData().setFoodLevel(20);
                serverPlayer.getFoodData().setSaturation(20.0f);
            }
        });

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                // Only allow damage if the source is another player (PvP)
                if (!(source.getEntity() instanceof ServerPlayer)) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });

        ChatEvent.RECEIVED.register((player, component) -> {
            if (player != null) {
                GameManager.getInstance().onPlayerChat(player, component);
            }
            return EventResult.pass();
        });

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                return GameManager.getInstance().onBlockBreak(level, pos, serverPlayer, xp);
            }
            return EventResult.pass();
        });

        BlockEvent.PLACE.register((level, pos, state, entity) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                return GameManager.getInstance().onBlockPlace(level, pos, state, serverPlayer);
            }
            return EventResult.pass();
        });

        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                return GameManager.getInstance().onLeftClickBlock(serverPlayer, pos);
            }
            return InteractionResult.PASS;
        });
    }
}

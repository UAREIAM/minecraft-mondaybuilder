package com.minigames.pool.crazychicken;

import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class StateManager {
    private final CrazyChickenGame game;
    private CrazyChickenState internalState = CrazyChickenState.JOIN;
    private int currentRound = 1;
    private final int maxRounds = 10;
    private int stateTimer = 0;
    private int totalStateTicks = 0;
    private final Map<UUID, ServerBossEvent> bossBars = new HashMap<>();
    private ServerLevel level;

    public enum CrazyChickenState {
        JOIN,
        BEFORE_ROUND,
        ROUND,
        AFTER_ROUND,
        BEFORE_GAME_END,
        GAME_END
    }

    public StateManager(CrazyChickenGame game) {
        this.game = game;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public void setInternalState(CrazyChickenState state, int seconds) {
        this.internalState = state;
        this.totalStateTicks = seconds * 20;
        this.stateTimer = totalStateTicks;
    }

    public CrazyChickenState getInternalState() {
        return internalState;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void tick(List<UUID> participants) {
        if (stateTimer > 0) {
            stateTimer--;
            updateAllBossBars(participants);
            if (stateTimer == 0) {
                game.transitionState();
            }
        }
    }

    private void updateAllBossBars(List<UUID> participants) {
        if (level == null) return;
        
        if (internalState == CrazyChickenState.AFTER_ROUND || 
            internalState == CrazyChickenState.BEFORE_GAME_END || 
            internalState == CrazyChickenState.GAME_END) {
            clearBossBars();
            return;
        }

        String stateName = switch (internalState) {
            case JOIN -> "Preparing";
            case BEFORE_ROUND -> "Round starting";
            case ROUND -> "Round " + currentRound;
            default -> internalState.name();
        };

        Component name = Component.literal(stateName + " - " + (stateTimer / 20) + "s");
        float progress = totalStateTicks > 0 ? (float) stateTimer / totalStateTicks : 0.0f;

        for (UUID uuid : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                ServerBossEvent bar = bossBars.computeIfAbsent(uuid, k -> {
                    ServerBossEvent b = new ServerBossEvent(name, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
                    b.addPlayer(player);
                    return b;
                });
                bar.setName(name);
                bar.setProgress(progress);
            }
        }
    }

    public void clearBossBars() {
        bossBars.values().forEach(ServerBossEvent::removeAllPlayers);
        bossBars.clear();
    }

    public int getStateTimer() {
        return stateTimer;
    }
}

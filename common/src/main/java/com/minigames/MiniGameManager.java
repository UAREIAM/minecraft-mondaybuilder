package com.minigames;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages mini-games, including registration and lifecycle control.
 */
public class MiniGameManager {
    private static MiniGameManager instance;
    private final Map<String, MiniGame> registeredGames = new HashMap<>();
    private final List<MiniGameListener> listeners = new ArrayList<>();
    private MiniGame activeGame;

    private MiniGameManager() {}

    public static MiniGameManager getInstance() {
        if (instance == null) {
            instance = new MiniGameManager();
        }
        return instance;
    }

    public void addListener(MiniGameListener listener) {
        listeners.add(listener);
    }

    public void notifyScoreUpdate(UUID playerUuid, int points) {
        for (MiniGameListener listener : listeners) {
            listener.onScoreUpdate(playerUuid, points);
        }
    }

    public void notifyGameStart(MiniGame game) {
        for (MiniGameListener listener : listeners) {
            listener.onGameStart(game);
        }
    }

    public void notifyGameUpdate(MiniGame game) {
        for (MiniGameListener listener : listeners) {
            listener.onGameUpdate(game);
        }
    }

    public void notifyGameEnd(MiniGame game) {
        for (MiniGameListener listener : listeners) {
            listener.onGameEnd(game);
        }
    }

    /**
     * Registers a new mini-game.
     */
    public void registerGame(MiniGame game) {
        registeredGames.put(game.getName().toLowerCase(), game);
    }

    /**
     * Starts a registered mini-game by name.
     */
    public void startGame(String name) {
        getRegisteredGame(name).ifPresent(this::startGame);
    }

    /**
     * Starts a specific mini-game.
     */
    public void startGame(MiniGame game) {
        if (activeGame != null && activeGame.getState() == MiniGameState.RUNNING) {
            return; // Already a game running
        }

        activeGame = game;
        activeGame.start();
        notifyGameStart(activeGame);
    }

    public Optional<MiniGame> getRegisteredGame(String name) {
        return Optional.ofNullable(registeredGames.get(name.toLowerCase()));
    }

    public void pauseActiveGame() {
        if (activeGame != null) {
            activeGame.pause();
        }
    }

    public void resumeActiveGame() {
        if (activeGame != null) {
            activeGame.resume();
        }
    }

    public void stopActiveGame() {
        if (activeGame != null) {
            MiniGame game = activeGame;
            activeGame.stop();
            notifyGameEnd(game);
            activeGame = null;
        }
    }

    public void onBlockClick(ServerPlayer player, BlockPos pos) {
        if (activeGame != null) {
            activeGame.onBlockClick(player, pos);
        }
    }

    public void onMobDeath(net.minecraft.world.entity.Mob mob, net.minecraft.world.damagesource.DamageSource source) {
        if (activeGame != null) {
            activeGame.onMobDeath(mob, source);
        }
    }

    public void tick() {
        if (activeGame != null) {
            activeGame.tick();
        }
    }

    public Optional<MiniGame> getActiveGame() {
        return Optional.ofNullable(activeGame);
    }

    public void showResults() {
        // To be implemented: MiniGameResultScreen logic
    }

    public void showLeaderboard() {
        // To be implemented: MiniGameLeaderboardScreen logic
    }
}

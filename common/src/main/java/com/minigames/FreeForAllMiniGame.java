package com.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract class for Free-for-all mini-games.
 */
public abstract class FreeForAllMiniGame extends MiniGame {
    protected List<UUID> participants = new ArrayList<>();
    protected Map<UUID, Integer> scores = new HashMap<>();
    protected List<UUID> eliminatedPlayers = new ArrayList<>();

    protected FreeForAllMiniGame(String name) {
        super(name);
    }

    public void setupParticipants(List<UUID> players) {
        this.participants = new ArrayList<>(players);
        this.scores.clear();
        this.eliminatedPlayers.clear();
        for (UUID uuid : players) {
            scores.put(uuid, 0);
        }
    }

    protected void eliminatePlayer(UUID uuid) {
        if (!eliminatedPlayers.contains(uuid)) {
            eliminatedPlayers.add(uuid);
            onPlayerEliminated(uuid);
        }
        
        if (eliminatedPlayers.size() >= participants.size() - 1) {
            finishGame();
        }
    }

    protected void updateScore(UUID uuid, int points) {
        scores.put(uuid, scores.getOrDefault(uuid, 0) + points);
        MiniGameManager.getInstance().notifyScoreUpdate(uuid, points);
    }

    protected abstract void onPlayerEliminated(UUID uuid);
    protected abstract void finishGame();
}

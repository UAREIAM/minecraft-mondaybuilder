package com.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abstract class for 1vs1 (Play-Off/Gauntlet style) mini-games.
 */
public abstract class OneVsOneMiniGame extends MiniGame {
    protected List<UUID> playerQueue = new ArrayList<>();
    protected UUID currentWinner = null;
    protected UUID currentChallenger = null;
    protected int matchIndex = 0;

    protected OneVsOneMiniGame(String name) {
        super(name);
    }

    /**
     * Initializes the match queue based on provided player UUIDs.
     * Usually sorted by highest score as per requirements.
     */
    public void setupQueue(List<UUID> players) {
        this.playerQueue = new ArrayList<>(players);
        this.matchIndex = 0;
        this.currentWinner = null;
        this.currentChallenger = null;
    }

    protected void nextMatch() {
        if (playerQueue.size() < 2) {
            finishGame();
            return;
        }

        if (currentWinner == null) {
            // First match
            currentWinner = playerQueue.get(0);
            currentChallenger = playerQueue.get(1);
            matchIndex = 2;
        } else {
            // Subsequent match
            if (matchIndex < playerQueue.size()) {
                currentChallenger = playerQueue.get(matchIndex);
                matchIndex++;
            } else {
                finishGame();
                return;
            }
        }
        onMatchStart(currentWinner, currentChallenger);
    }

    protected void resolveMatch(UUID winner) {
        this.currentWinner = winner;
        onMatchEnd(winner);
        MiniGameManager.getInstance().notifyScoreUpdate(winner, 5); // Example points
        nextMatch();
    }

    protected abstract void onMatchStart(UUID player1, UUID player2);
    protected abstract void onMatchEnd(UUID winner);
    protected abstract void finishGame();
}

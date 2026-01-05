package com.minigames;

import java.util.UUID;

/**
 * Interface for receiving updates from the mini-game system.
 */
public interface MiniGameListener {
    void onScoreUpdate(UUID playerUuid, int points);
    void onGameEnd(MiniGame game);
}

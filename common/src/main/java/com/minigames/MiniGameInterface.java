package com.minigames;

/**
 * Interface defining the contract for all mini-games.
 */
public interface MiniGameInterface {
    void start();
    void pause();
    void resume();
    void stop();
    void restart();
    
    String getName();
    MiniGameState getState();
    MiniGameTimer getTimer();
}

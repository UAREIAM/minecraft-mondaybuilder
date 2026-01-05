package com.minigames;

/**
 * Handles the registration of all mini-games.
 */
public class MiniGameRegistry {
    public static void registerAll() {
        MiniGameManager.getInstance().registerGame(new com.minigames.pool.tictactoe.core.TicTacToeGame());
    }
}

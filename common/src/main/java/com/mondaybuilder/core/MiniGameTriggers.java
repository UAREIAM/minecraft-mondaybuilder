package com.mondaybuilder.core;

import com.minigames.MiniGameManager;
import net.minecraft.server.MinecraftServer;
import java.util.Random;

/**
 * Handles mapping of main game events to mini-game triggers.
 */
public class MiniGameTriggers {
    private static final Random RANDOM = new Random();

    public static void onRoundEnd(MinecraftServer server, int roundNumber) {
        // Example: 20% chance to start a random mini-game between rounds
        if (RANDOM.nextFloat() < 0.20f) {
            // MiniGameManager.getInstance().startGame("tictactoe");
        }
    }
}

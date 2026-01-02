package com.mondaybuilder.core.session;

import com.mondaybuilder.core.mechanics.GameTimer;
import java.util.UUID;

public class RoundContext {
    private final int roundNumber;
    private final String word;
    private final WordCategory category;
    private final UUID builder;
    private final GameTimer timer;
    private boolean winnerFound = false;

    public RoundContext(int roundNumber, String word, WordCategory category, UUID builder, GameTimer timer) {
        this.roundNumber = roundNumber;
        this.word = word;
        this.category = category;
        this.builder = builder;
        this.timer = timer;
    }

    public int getRoundNumber() { return roundNumber; }
    public String getWord() { return word; }
    public WordCategory getCategory() { return category; }
    public UUID getBuilder() { return builder; }
    public GameTimer getTimer() { return timer; }
    
    public boolean isWinnerFound() { return winnerFound; }
    public RoundContext withWinnerFound() {
        RoundContext newCtx = new RoundContext(roundNumber, word, category, builder, timer);
        newCtx.winnerFound = true;
        return newCtx;
    }
}

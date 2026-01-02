package com.mondaybuilder.core.session;

public enum WordCategory {
    EASY("Easy", 60, 10),
    INTERMEDIATE("Intermediate", 90, 20),
    STRONG("Strong", 120, 30);

    private final String displayName;
    private final int timerSeconds;
    private final int blockAmount;

    WordCategory(String displayName, int timerSeconds, int blockAmount) {
        this.displayName = displayName;
        this.timerSeconds = timerSeconds;
        this.blockAmount = blockAmount;
    }

    public String getDisplayName() { return displayName; }
    public int getTimerSeconds() { return timerSeconds; }
    public int getBlockAmount() { return blockAmount; }
}

package com.mondaybuilder.core.session;

public enum WordCategory {
    EASY("Easy", 60, 30),
    INTERMEDIATE("Intermediate", 90, 45),
    STRONG("Strong", 120, 60);

    private final String displayName;
    private final int timerSeconds;
    private final int blockAmount;

    WordCategory(String displayName, int timerSeconds, int blockAmount) {
        this.displayName = displayName;
        this.timerSeconds = timerSeconds;
        this.blockAmount = blockAmount;
    }

    public String getDisplayName() { return displayName; }
    public String getTranslationKey() { return "ui.category." + name().toLowerCase(); }
    public int getTimerSeconds() { return timerSeconds; }
    public int getBlockAmount() { return blockAmount; }
}

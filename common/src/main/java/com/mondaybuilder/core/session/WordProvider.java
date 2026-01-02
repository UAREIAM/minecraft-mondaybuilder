package com.mondaybuilder.core.session;

import com.mondaybuilder.config.ConfigManager;
import java.util.List;
import java.util.Random;

public class WordProvider {
    private final Random random = new Random();

    public String getRandomWord() {
        return getRandomWord(WordCategory.EASY);
    }

    public String getRandomWord(WordCategory category) {
        List<String> wordList = ConfigManager.words.categories.get(category.name());
        if (wordList != null && !wordList.isEmpty()) {
            return wordList.get(random.nextInt(wordList.size()));
        }
        return "Minecraft"; // Fallback
    }

    public boolean isCorrect(String guess, String word) {
        if (guess == null || word == null) return false;
        return guess.trim().equalsIgnoreCase(word.trim());
    }
}

package com.mondaybuilder.core.mechanics;

import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.PlayerRole;
import com.mondaybuilder.core.session.RoundContext;
import com.mondaybuilder.core.session.WordProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;

public class GuessingManager {
    private final WordProvider wordProvider = new WordProvider();

    public void handleChat(ServerPlayer player, Component message, GameState state, RoundContext currentRound, PlayerRole role, Consumer<ServerPlayer> onCorrectGuess) {
        if (state != GameState.BUILDING || currentRound == null) return;
        
        // Spectators and the Builder cannot guess
        if (role == PlayerRole.SPECTATOR || role == PlayerRole.BUILDER) return;

        if (wordProvider.isCorrect(message.getString(), currentRound.getWord())) {
            onCorrectGuess.accept(player);
        }
    }

    public WordProvider getWordProvider() {
        return wordProvider;
    }
}

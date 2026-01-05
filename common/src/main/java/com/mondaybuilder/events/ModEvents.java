package com.mondaybuilder.events;

import com.mondaybuilder.core.session.WordCategory;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class ModEvents {
    // Role Events
    public static final Event<SetRole> SET_ROLE = EventFactory.createLoop(SetRole.class);

    // Game Status Events
    public static final Event<GameStatus> GAME_START = EventFactory.createLoop(GameStatus.class);
    public static final Event<GameStatus> GAME_OVER = EventFactory.createLoop(GameStatus.class);

    // Round Events
    public static final Event<RoundStatus> ROUND_START = EventFactory.createLoop(RoundStatus.class);
    public static final Event<RoundStatus> ROUND_END = EventFactory.createLoop(RoundStatus.class);
    public static final Event<RoundPrepare> ROUND_PREPARE = EventFactory.createLoop(RoundPrepare.class);
    public static final Event<WordGuessed> WORD_GUESSED = EventFactory.createLoop(WordGuessed.class);
    public static final Event<WordNotGuessed> WORD_NOT_GUESSED = EventFactory.createLoop(WordNotGuessed.class);
    public static final Event<TimerTick> TIMER_TICK = EventFactory.createLoop(TimerTick.class);
    public static final Event<TicTacToeWin> TIC_TAC_TOE_WIN = EventFactory.createLoop(TicTacToeWin.class);

    // Player Lifecycle Events
    public static final Event<PlayerLobby> PLAYER_JOIN_LOBBY = EventFactory.createLoop(PlayerLobby.class);
    public static final Event<PlayerLobby> PLAYER_LEAVE_LOBBY = EventFactory.createLoop(PlayerLobby.class);
    public static final Event<PlayerGame> PLAYER_JOIN_GAME = EventFactory.createLoop(PlayerGame.class);
    public static final Event<PlayerGame> PLAYER_QUIT_GAME = EventFactory.createLoop(PlayerGame.class);
    public static final Event<PlayerDeath> PLAYER_DEATH = EventFactory.createLoop(PlayerDeath.class);

    public interface SetRole {
        void onSetRole(ServerPlayer player, String role);
    }

    public interface GameStatus {
        void onStatusChange(MinecraftServer server);
    }

    public interface RoundStatus {
        void onRoundChange(MinecraftServer server, int roundNumber);
    }

    public interface RoundPrepare {
        void onPrepare(ServerPlayer builder, String word, WordCategory category);
    }

    public interface WordGuessed {
        void onGuessed(ServerPlayer winner, String word, int points);
    }

    public interface WordNotGuessed {
        void onNotGuessed(MinecraftServer server, String word);
    }

    public interface TimerTick {
        void onTick(MinecraftServer server, int ticksRemaining);
    }

    public interface PlayerLobby {
        void onLobbyEvent(ServerPlayer player);
    }

    public interface PlayerGame {
        void onGameEvent(ServerPlayer player);
    }

    public interface PlayerDeath {
        void onDeath(ServerPlayer player);
    }

    public interface TicTacToeWin {
        void onWin(ServerPlayer winner);
    }
}

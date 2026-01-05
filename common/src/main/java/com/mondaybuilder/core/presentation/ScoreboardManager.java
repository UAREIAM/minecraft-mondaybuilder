package com.mondaybuilder.core.presentation;

import com.mondaybuilder.MondayBuilder;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.mechanics.ScoringSystem;
import com.mondaybuilder.core.GameManager;
import com.minigames.MiniGameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.UUID;
import java.util.Optional;

public class ScoreboardManager {
    private static final String OBJECTIVE_NAME = "mb_scores";
    private final ScoringSystem scoring;

    public ScoreboardManager(ScoringSystem scoring) {
        this.scoring = scoring;
    }

    public void initScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            scoreboard.removeObjective(objective);
        }
        // Increase width by padding title
        String paddedTitle = "   " + ConfigManager.getLang("ui.scoreboard.title") + "   ";
        objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal(paddedTitle), ObjectiveCriteria.RenderType.INTEGER, true, null);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
    }

    public void updateScoreboard(MinecraftServer server) {
        // Re-init to clear old entries and apply new formatting/width
        initScoreboard(server);
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) return;

        GameManager gm = GameManager.getInstance();
        UUID builderUuid = gm.getCurrentBuilder();
        var activeGame = MiniGameManager.getInstance().getActiveGame();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int score = scoring.getScore(player);
            
            String color = gm.getPlayerColorHex(player.getUUID());
            String prefix = "";
            
            if (activeGame.isPresent() && activeGame.get().getName().equalsIgnoreCase("TicTacToe")) {
                var game = activeGame.get();
                var gameColor = game.getPlayerColor(player.getUUID());
                var gamePrefix = game.getPlayerPrefix(player.getUUID());
                
                if (gameColor.isPresent()) {
                    color = gameColor.get().toString();
                }
                if (gamePrefix.isPresent()) {
                    prefix = gamePrefix.get();
                }
            }
            
            // Format: Bold if builder
            String bold = player.getUUID().equals(builderUuid) ? "§l" : "";
            
            String name = player.getScoreboardName();
            // Only use custom name in main game if it doesn't contain the TicTacToe prefix
            if (player.hasCustomName() && player.getCustomName() != null && activeGame.isEmpty()) {
                String customName = player.getCustomName().getString();
                if (!customName.startsWith("* ")) {
                    name = customName;
                }
            }

            // Increase width by padding player names
            String displayName = "   " + color + prefix + bold + name + "   ";
            
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(displayName), objective).set(score);
        }
    }

    public void clearScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            scoreboard.removeObjective(objective);
        }
    }
}

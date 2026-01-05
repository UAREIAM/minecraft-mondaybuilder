package com.mondaybuilder.core.presentation;

import com.mondaybuilder.MondayBuilder;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.mechanics.ScoringSystem;
import com.mondaybuilder.core.GameManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.UUID;

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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int score = scoring.getScore(player);
            String color = gm.getPlayerColorHex(player.getUUID());
            
            // Format: Bold if builder
            String bold = player.getUUID().equals(builderUuid) ? "§l" : "";
            
            String name = player.getScoreboardName();
            if (player.hasCustomName() && player.getCustomName() != null) {
                name = player.getCustomName().getString();
            }

            // Increase width by padding player names
            String displayName = "   " + color + bold + name + "   ";
            
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

package com.mondaybuilder.core.presentation;

import com.mondaybuilder.MondayBuilder;
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
        objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal("§6§lMONTAGSMALER"), ObjectiveCriteria.RenderType.INTEGER, true, null);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
    }

    public void updateScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) return;

        GameManager gm = GameManager.getInstance();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int score = scoring.getScore(player);
            String color = gm.getPlayerColorHex(player.getUUID());
            String displayName = color + player.getScoreboardName();
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

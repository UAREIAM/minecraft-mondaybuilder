package com.mondaybuilder.core.mechanics;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class ScoringSystem {
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> guessStreaks = new HashMap<>();

    public int calculate(GuessContext context) {
        int elapsedTicks = context.totalTicks() - context.ticksRemaining();
        int elapsedSeconds = elapsedTicks / 20;

        int basePoints;
        if (elapsedSeconds <= 10) basePoints = 20;
        else if (elapsedSeconds <= 25) basePoints = 15;
        else if (elapsedSeconds <= 45) basePoints = 10;
        else basePoints = 5;

        // Apply streak bonus (planned in REFACTOR_PLAN but not fully detailed, let's add a simple one)
        return basePoints + (context.currentStreak() * 2);
    }

    public void addScore(UUID player, int points) {
        scores.put(player, scores.getOrDefault(player, 0) + points);
    }

    public int getScore(UUID player) {
        return scores.getOrDefault(player, 0);
    }

    public void incrementStreak(UUID player) {
        guessStreaks.put(player, guessStreaks.getOrDefault(player, 0) + 1);
    }

    public void resetStreak(UUID player) {
        guessStreaks.put(player, 0);
    }

    public int getStreak(UUID player) {
        return guessStreaks.getOrDefault(player, 0);
    }

    public void clearScores() {
        scores.clear();
    }

    public void clearStreaks() {
        guessStreaks.clear();
    }

    public void grantAdvancement(MinecraftServer server, ServerPlayer player, ResourceLocation id) {
        if (server == null || player == null || id == null) return;
        
        if (server.getAdvancements() == null) {
            System.err.println("[Montagsmaler] CRITICAL ERROR: server.getAdvancements() is NULL!");
            return;
        }

        AdvancementHolder advancement = server.getAdvancements().get(id);
        if (advancement == null) {
            System.err.println("[Montagsmaler] ERROR: Advancement not found: " + id);
            // List all advancements found in registry to debug
            System.err.println("[Montagsmaler] Total advancements in registry: " + server.getAdvancements().getAllAdvancements().size());
            Set<String> namespaces = new HashSet<>();
            for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                namespaces.add(holder.id().getNamespace());
                if (holder.id().getNamespace().equals("mondaybuilder") || holder.id().getPath().contains("root")) {
                    System.err.println("[Montagsmaler] Found potential match: " + holder.id());
                }
            }
            System.err.println("[Montagsmaler] Registered namespaces in advancements: " + namespaces);
            return;
        }

        // Ensure parent is granted first (Minecraft requirement)
        advancement.value().parent().ifPresent(parentLocation -> {
            grantAdvancement(server, player, parentLocation);
        });

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
            System.out.println("[Montagsmaler] Successfully granted advancement: " + id + " to " + player.getName().getString());
        }
    }

    public void resetAdvancements(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) return;
        // Revoke all advancements belonging to this mod
        for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
            if (advancement.id().getNamespace().equals("mondaybuilder")) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revoke(advancement, criterion);
                }
            }
        }
    }

    public Map<UUID, Integer> getScores() {
        return scores;
    }

    public UUID getWinner() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

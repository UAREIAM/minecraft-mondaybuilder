package com.minigames.pool.crazychicken.core;

import com.minigames.pool.crazychicken.GameManager;
import com.minigames.pool.crazychicken.RoundManager;
import com.minigames.pool.crazychicken.ScoreManager;
import com.minigames.pool.crazychicken.StateManager;
import com.minigames.pool.crazychicken.StateManager.CrazyChickenState;
import com.minigames.MiniGame;
import com.minigames.MiniGameManager;
import com.minigames.MiniGameState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

import java.util.*;

public class CrazyChickenGame extends MiniGame {
    private final GameManager gameManager;
    private final RoundManager roundManager;
    private final ScoreManager scoreManager;
    private final StateManager stateManager;
    
    private final List<UUID> participants = new ArrayList<>();
    private ServerLevel level;
    private final Random random = new Random();
    private net.minecraft.sounds.SoundEvent currentAmbience;

    public CrazyChickenGame() {
        super("CrazyChicken");
        this.gameManager = new GameManager(this);
        this.roundManager = new RoundManager(this);
        this.scoreManager = new ScoreManager(this);
        this.stateManager = new StateManager(this);
    }

    @Override
    protected void onStart() {
        gameManager.setLevel(level);
        roundManager.setLevel(level);
        roundManager.clearMobs();
        scoreManager.clearAllData();
        stateManager.setLevel(level);
        stateManager.setCurrentRound(1);
        
        gameManager.initializeGame(participants);
        stateManager.setInternalState(CrazyChickenState.JOIN, 10);
    }

    @Override
    protected void onPause() {
    }

    @Override
    protected void onResume() {
    }

    @Override
    protected void onStop() {
        roundManager.clearMobs();
        stateManager.clearBossBars();
        gameManager.cleanup();
    }

    @Override
    protected void onTick() {
        gameManager.updateParticipants();
        if (gameManager.getParticipants().isEmpty() && stateManager.getInternalState() != CrazyChickenState.JOIN) {
            MiniGameManager.getInstance().stopActiveGame();
            return;
        }

        stateManager.tick(gameManager.getParticipants());

        if (stateManager.getInternalState() == CrazyChickenState.ROUND) {
            roundManager.tick(stateManager.getCurrentRound(), gameManager.getParticipants());
        }
    }

    public void transitionState() {
        switch (stateManager.getInternalState()) {
            case JOIN -> {
                stateManager.setInternalState(CrazyChickenState.BEFORE_ROUND, 5);
                announceRound();
            }
            case BEFORE_ROUND -> {
                stateManager.setInternalState(CrazyChickenState.ROUND, 30);
                startRound();
            }
            case ROUND -> {
                stateManager.setInternalState(CrazyChickenState.AFTER_ROUND, 5);
                endRound();
            }
            case AFTER_ROUND -> {
                if (stateManager.getCurrentRound() < stateManager.getMaxRounds()) {
                    stateManager.setCurrentRound(stateManager.getCurrentRound() + 1);
                    stateManager.setInternalState(CrazyChickenState.BEFORE_ROUND, 5);
                    announceRound();
                } else {
                    stateManager.setInternalState(CrazyChickenState.BEFORE_GAME_END, 60);
                    announceGameEnd();
                }
            }
            case BEFORE_GAME_END -> {
                stateManager.setInternalState(CrazyChickenState.GAME_END, 5);
                finalizeGame();
            }
            case GAME_END -> {
                MiniGameManager.getInstance().stopActiveGame();
            }
        }
    }

    @Override
    public void onMobDeath(Mob mob, DamageSource source) {
        if (stateManager.getInternalState() != CrazyChickenState.ROUND) return;
        if (!roundManager.getActiveMobs().contains(mob)) return;

        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player && gameManager.getTotalParticipants().contains(player.getUUID())) {
            scoreManager.trackKill(player, mob.getType());
        }

        mob.discard();
        roundManager.getActiveMobs().remove(mob);
    }

    public boolean canUseItems(ServerPlayer player) {
        if (!gameManager.getTotalParticipants().contains(player.getUUID())) return true;
        return stateManager.getInternalState() == CrazyChickenState.ROUND;
    }

    private void announceRound() {
        broadcastTitle("Round " + stateManager.getCurrentRound(), "Get ready for hunt!");
        broadcastMessage("Next round starts in 5 seconds");
    }

    private void startRound() {
        scoreManager.clearRoundData();
        broadcastMessage("Round " + stateManager.getCurrentRound() + " started!");

        // Play random ambience
        net.minecraft.sounds.SoundEvent[] ambientSounds = {
            com.mondaybuilder.registry.ModSounds.AMBIENCE_1,
            com.mondaybuilder.registry.ModSounds.AMBIENCE_2,
            com.mondaybuilder.registry.ModSounds.AMBIENCE_3
        };
        currentAmbience = ambientSounds[random.nextInt(ambientSounds.length)];
        broadcastSound(currentAmbience, 0.3f, 1.0f);
    }

    private void endRound() {
        roundManager.clearMobs();
        String title = "Round " + stateManager.getCurrentRound() + " finished!";
        
        for (UUID uuid : gameManager.getTotalParticipants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                String subtitle = player.getName().getString() + ": " + scoreManager.getTotalPoints(uuid);
                sendTitle(player, title, subtitle);
            }
        }
    }

    private void announceGameEnd() {
        broadcastTitle("Game end!", "Let's view the score");
        showScoreboard();
    }

    private void showScoreboard() {
        broadcastMessage("--- Final Scores ---");
        for (UUID uuid : gameManager.getTotalParticipants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            String name = player != null ? player.getName().getString() : uuid.toString();
            broadcastMessage(name + ": " + scoreManager.getTotalPoints(uuid) + " points");
        }
    }

    private void finalizeGame() {
        broadcastTitle("Game ended!", "Returning to lobby...");
        stateManager.clearBossBars();

        for (UUID uuid : gameManager.getTotalParticipants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getInventory().clearContent();
                com.mondaybuilder.core.GameManager.getInstance().getArenaManager().teleportToLobby(player);
            }
        }
    }

    public void setParticipants(List<UUID> playerUuids) {
        this.participants.clear();
        this.participants.addAll(playerUuids);
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    private void broadcastMessage(String message) {
        if (level == null) return;
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal(message).withStyle(ChatFormatting.YELLOW),
            false
        );
    }

    private void broadcastTitle(String title, String subtitle) {
        if (level == null) return;
        for (UUID uuid : gameManager.getTotalParticipants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                sendTitle(player, title, subtitle);
            }
        }
    }

    private void sendTitle(ServerPlayer player, String title, String subtitle) {
        player.sendSystemMessage(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendSystemMessage(Component.literal(subtitle).withStyle(ChatFormatting.YELLOW));
        }
        // In a real Fabric mod, you'd use player.sendTitle() or similar. 
        // For now, I'll stick to system messages but formatted as requested if I can't find a better way.
        // Actually, let's try to use the proper packets if available, but the current code used sendSystemMessage.
        // I will keep it as system messages for now but ensure both title and subtitle are sent.
    }

    private void broadcastSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level == null) return;
        for (UUID uuid : gameManager.getTotalParticipants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.playNotifySound(sound, net.minecraft.sounds.SoundSource.MASTER, volume, pitch);
            }
        }
    }
}

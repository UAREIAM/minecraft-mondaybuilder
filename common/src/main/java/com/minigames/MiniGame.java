package com.minigames;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base class for all mini-games.
 */
public abstract class MiniGame implements MiniGameInterface {
    protected final String name;
    protected MiniGameState state = MiniGameState.IDLE;
    protected final MiniGameTimer timer = new MiniGameTimer();
    protected boolean triggered = false;

    protected MiniGame(String name) {
        this.name = name;
    }

    public Optional<String> getPlayerPrefix(UUID playerUuid) {
        return Optional.empty();
    }

    public Optional<ChatFormatting> getPlayerColor(UUID playerUuid) {
        return Optional.empty();
    }

    @Override
    public void start() {
        this.state = MiniGameState.STARTING;
        onStart();
        // Only transition to RUNNING if onStart didn't stop or pause the game
        if (this.state == MiniGameState.STARTING) {
            this.state = MiniGameState.RUNNING;
        }
    }

    @Override
    public void pause() {
        if (this.state == MiniGameState.RUNNING) {
            this.state = MiniGameState.PAUSED;
            timer.pause();
            onPause();
        }
    }

    @Override
    public void resume() {
        if (this.state == MiniGameState.PAUSED) {
            this.state = MiniGameState.RUNNING;
            timer.resume();
            onResume();
        }
    }

    @Override
    public void stop() {
        this.state = MiniGameState.STOPPED;
        timer.stop();
        onStop();
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MiniGameState getState() {
        return state;
    }

    @Override
    public MiniGameTimer getTimer() {
        return timer;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public boolean isTriggered() {
        return triggered;
    }

    /**
     * Called when the mini-game ticks.
     */
    public void tick() {
        if (state == MiniGameState.RUNNING) {
            timer.tick();
            onTick();
        }
    }

    /**
     * Called when a block is clicked during the mini-game.
     */
    public void onBlockClick(ServerPlayer player, BlockPos pos) {
        // To be overridden by specific mini-games
    }

    /**
     * Called when a mob dies during the mini-game.
     */
    public void onMobDeath(net.minecraft.world.entity.Mob mob, net.minecraft.world.damagesource.DamageSource source) {
        // To be overridden by specific mini-games
    }

    // Abstract methods to be implemented by specific mini-games
    protected abstract void onStart();
    protected abstract void onPause();
    protected abstract void onResume();
    protected abstract void onStop();
    protected abstract void onTick();
}

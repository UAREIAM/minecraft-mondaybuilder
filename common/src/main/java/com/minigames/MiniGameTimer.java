package com.minigames;

import java.util.function.Consumer;

public class MiniGameTimer {
    private int ticksRemaining;
    private boolean paused = false;
    private boolean running = false;
    private Runnable onFinish;
    private Consumer<Integer> onTick;

    public void start(int seconds, Consumer<Integer> onTick, Runnable onFinish) {
        this.ticksRemaining = seconds * 20; // Minecraft ticks
        this.onTick = onTick;
        this.onFinish = onFinish;
        this.running = true;
        this.paused = false;
    }

    public void tick() {
        if (!running || paused) return;

        if (onTick != null) {
            onTick.accept(ticksRemaining);
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else {
            stop();
            if (onFinish != null) {
                onFinish.run();
            }
        }
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public void stop() {
        this.running = false;
        this.paused = false;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getSecondsRemaining() {
        return ticksRemaining / 20;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }
}

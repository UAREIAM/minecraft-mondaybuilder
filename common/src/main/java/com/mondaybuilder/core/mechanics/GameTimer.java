package com.mondaybuilder.core.mechanics;

import java.util.function.Consumer;

public class GameTimer {
    public enum State {
        IDLE,
        RUNNING,
        PAUSED,
        FINISHED
    }

    private int ticksRemaining;
    private int totalTicks;
    private State state = State.IDLE;
    private Runnable onFinish;
    private Consumer<Integer> onTick;

    public void start(int ticks, Consumer<Integer> onTick, Runnable onFinish) {
        this.ticksRemaining = ticks;
        this.totalTicks = ticks;
        this.onTick = onTick;
        this.onFinish = onFinish;
        this.state = State.RUNNING;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public void tick() {
        if (state != State.RUNNING) return;

        if (onTick != null) {
            onTick.accept(ticksRemaining);
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else {
            state = State.FINISHED;
            if (onFinish != null) {
                onFinish.run();
            }
        }
    }

    public void stop() {
        this.state = State.FINISHED;
    }

    public void stopAndFinish() {
        this.state = State.FINISHED;
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public void pause() {
        if (this.state == State.RUNNING) {
            this.state = State.PAUSED;
        }
    }

    public void resume() {
        if (this.state == State.PAUSED) {
            this.state = State.RUNNING;
        }
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getSecondsRemaining() {
        return ticksRemaining / 20;
    }

    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }
}

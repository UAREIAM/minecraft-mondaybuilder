package com.mondaybuilder.core.mechanics;

import java.util.function.Consumer;
import java.util.*;

public class GameTimer {
    private final Map<Integer, List<Runnable>> scheduledTasks = new HashMap<>();
    private int elapsedTicks = 0;

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

    public void scheduleTask(int delayTicks, Runnable task) {
        scheduledTasks.computeIfAbsent(elapsedTicks + delayTicks, k -> new ArrayList<>()).add(task);
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public void tick() {
        // Process scheduled tasks regardless of timer state (global timer behavior)
        List<Runnable> tasks = scheduledTasks.remove(elapsedTicks);
        if (tasks != null) {
            tasks.forEach(Runnable::run);
        }
        elapsedTicks++;

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

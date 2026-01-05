package com.minigames;

/**
 * Abstract base class for all mini-games.
 */
public abstract class MiniGame implements MiniGameInterface {
    protected final String name;
    protected MiniGameState state = MiniGameState.IDLE;
    protected final MiniGameTimer timer = new MiniGameTimer();

    protected MiniGame(String name) {
        this.name = name;
    }

    @Override
    public void start() {
        this.state = MiniGameState.STARTING;
        onStart();
        this.state = MiniGameState.RUNNING;
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

    /**
     * Called when the mini-game ticks.
     */
    public void tick() {
        if (state == MiniGameState.RUNNING) {
            timer.tick();
            onTick();
        }
    }

    // Abstract methods to be implemented by specific mini-games
    protected abstract void onStart();
    protected abstract void onPause();
    protected abstract void onResume();
    protected abstract void onStop();
    protected abstract void onTick();
}

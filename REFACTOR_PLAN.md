# GameManager Refactoring & Bug Fix Roadmap

This document outlines the step-by-step deconstruction of the monolithic `GameManager` into specialized Action-Objects, as per `game_actions.md` and refined architectural best practices.

## Phase 0: Prerequisites & Critical Fixes
- [ ] **Build Restoration**: Resolve `./gradlew classes` failing with `java.io.IOException: Input/output error`.
- [ ] **Baseline Backup**: Ensure a clean commit/state exists before structural changes.

## Phase 1: Mechanics Domain (The Clock & The Rules)
*Goal: Remove timing and scoring logic from GameManager.*
- [ ] **Create `GameTimer`**:
    - Implement "Action COUNTDOWN" logic.
    - Support `onTick` and `onFinish` callbacks.
    - Support states: `IDLE`, `RUNNING`, `PAUSED`, `FINISHED`.
- [ ] **Create `ScoringSystem`**:
    - Implement "Action SCORE" logic.
    - Refactor 10+ point methods into a strategy-based `calculate(GuessContext)` method.
    - Integrate manual advancement granting logic.

## Phase 2: Environment Domain (The Stage)
*Goal: Isolate physical world interactions.*
- [ ] **Create `ArenaManager`**:
    - **Fix Bug**: Implement reliable `cleanupStage()` starting from `minY`.
    - Implement floor protection logic for `y=50`.
    - Centralize all `teleport` and `setupBoundaries` methods.

## Phase 3: Data & Session Domain (The Context)
*Goal: Separate long-lived game state from temporary round data.*
- [ ] **Define `PlayerRole` Enum**: Replace booleans with `BUILDER`, `GUESSER`, `SPECTATOR`.
- [ ] **Create `RoundContext`**: A POJO to hold current word, builder, points, and timer instance.
- [ ] **Create `WordProvider`**: Implement "Action WORD" random selection and validation.

## Phase 4: Presentation Domain (The Bridge)
*Goal: Decouple game logic from Minecraft UI/Sound packets.*
- [ ] **Create `NotificationService`**:
    - Centralize "Action UI" and "Action SOUND".
    - **Fix Timing Bug**: Ensure `sendTitle` is only called during the 5s prepare phase.
    - **Fix Timing Bug**: Ensure the action bar overlay logic is correctly tied to the `GameTimer`.

## Phase 5: Orchestration (The New GameManager)
*Goal: Final integration.*
- [ ] **Refactor `GameManager`**:
    - Strip all business logic.
    - Transform it into a high-level **Coordinator** that manages transitions between `RoundContext` instances using the modules from Phases 1-4.
- [ ] **Final Verification**: 
    - Verify stage resets correctly.
    - Verify advancement toasts appear.
    - Verify timing of word announcements.

## Architectural Rules for Zencoder
1. **No God Objects**: No class should exceed 250 lines.
2. **Role Safety**: Always use the `PlayerRole` enum; never allow dual-roles.
3. **Event-Driven**: Logic modules should never call `NotificationService` directly; they should emit events that the service listens to.
4. **Immutability**: Treat `RoundContext` as immutable once a round starts, where possible.

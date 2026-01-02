# Montagsmaler (MondayBuilder) Repository

This repository contains the source code for the **Montagsmaler** (MondayBuilder) Minecraft mod, a Pictionary-style game for Minecraft 1.21.10 using the **Architectury** framework and **Fabric** loader.

## Project Structure

- **`.zencoder/`**: Contains AI assistant configuration, documentation, and development rules.
- **`common/`**: The core mod logic shared across all platforms.
  - **`src/main/java/com/mondaybuilder/`**:
    - `MondayBuilder.java`: Main entry point and event registration.
    - **`commands/`**: Command handling (`ModCommands.java`).
    - **`config/`**: Configuration management (`ConfigManager.java`, `ModConfig.java`).
    - **`core/`**:
      - **`env/`**: World and arena management (`ArenaManager.java`, `BlockInteractionManager.java`).
      - **`mechanics/`**: Game logic mechanics (`GameTimer.java`, `ScoringSystem.java`).
      - **`presentation/`**: UI and player feedback (`NotificationService.java`, `ScoreboardManager.java`, `CategorySelectionUI.java`).
      - **`session/`**: Game session data (`RoundContext.java`, `WordProvider.java`, `WordCategory.java`).
      - `GameManager.java`: Orchestrates the game lifecycle.
      - `GameState.java`: Defines possible game states.
    - **`events/`**: Event listeners and definitions (`ModEvents.java`).
    - **`registry/`**: Mod registry entries (`ModSounds.java`).
- **`fabric/`**: Fabric-specific implementation and resources.
- **`gradle/`**: Gradle wrapper and build configuration.

## Key Features

- **Game Orchestration**: `GameManager` manages transitions between Lobby, Round, and Game Over states.
- **Arena Management**: `ArenaManager` handles arena setup, player teleportation, and world boundaries.
- **Scoring & Roles**: Dynamic role assignment (Builder/Guesser) and a sophisticated `ScoringSystem`.
- **UI & Feedback**: Custom scoreboards, title notifications, and category selection menus.
- **Word Management**: `WordProvider` serves words from configurable categories.
- **Interactions**: `BlockInteractionManager` controls where and how players can build during rounds.

## Build and Run

- **Compile**: `./gradlew classes`
- **Run Fabric Client**: `./gradlew :fabric:runClient`
- **Run Fabric Server**: `./gradlew :fabric:runServer`

## Dependencies (v1.0.0)

- **Minecraft**: 1.21.10
- **Architectury API**: 18.0.6
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.138.4+1.21.10

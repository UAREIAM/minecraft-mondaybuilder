# Montagsmaler (MondayBuilder) Repository

This repository contains the source code for the **Montagsmaler** (MondayBuilder) Minecraft mod, a Pictionary-style game implemented for Minecraft 1.21.10 using the **Architectury** framework and **Fabric** loader.

## Project Structure

- **`.zencoder/docs`**: Contains the documentation and concept as well as development conceptual docs.
- **`common/`**: Contains the core mod logic shared across all platforms.
  - **`src/main/java/com/mondaybuilder/`**:
    - `MondayBuilder.java`: Main mod entry point and event registration.
    - `core/GameManager.java`: Core game logic, managing rounds, scores, and game states.
    - `commands/ModCommands.java`: Registration and handling of mod commands.
    - `config/ConfigManager.java`: Configuration management for mod settings.
    - `events/ModEvents.java`: Custom event definitions for game-specific triggers.
- **`fabric/`**: Fabric-specific implementation and resources.
- **`mondaybuilder/`**: Additional subproject or tool (boilerplate implementation).

## Core Features

- **Game Management**: A central `GameManager` handles the game lifecycle, including lobby, round transitions, and winner detection.
- **Role System**: Players are assigned roles such as "builder" and "guesser".
- **Event-Driven Architecture**: Uses Architectury's event system to hook into Minecraft's lifecycle (chat, block placement/breaking, player join/quit).
- **Configuration**: Customizable settings through a `ModConfig` system.

## Build and Run

The project uses Gradle for build management.

- **Compile**: `./gradlew classes`
- **Run Fabric Client**: `./gradlew :fabric:runClient`
- **Run Fabric Server**: `./gradlew :fabric:runServer`

## Dependencies

- **Minecraft**: 1.21.10
- **Architectury API**: 18.0.6
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.138.4+1.21.10

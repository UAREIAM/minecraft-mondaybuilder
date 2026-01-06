# Monday Builder (Montagsmaler)

A Pictionary-style game mod for Minecraft 1.21.10, built using the **Architectury** framework and **Fabric** loader.

@Zencoder: first run: only read this file.

## 🚀 Core Technologies
- **Minecraft**: 1.21.10
- **Framework**: Architectury API (Multi-loader support)
- **Loader**: Fabric
- **Language**: Java 21
- **Build System**: Gradle

## 📂 Repository Structure

### Root Directory
- `common/`: Core mod logic shared across all platforms.
- `fabric/`: Fabric-specific implementation and entry points.
- `mondaybuilder/`: Sub-project for external tools or builders.
- `.zencoder/`: AI assistant configuration and development rules.

### Common Module (`common/src/main/java/com/mondaybuilder/`)
- **`commands/`**: Command registrations (`ModCommands.java`).
- **`config/`**: Configuration management (`ModConfig.java`, `ConfigManager.java`).
- **`core/`**: Central game logic:
    - **`env/`**: World and arena management (`ArenaManager.java`, `BlockInteractionManager.java`).
    - **`mechanics/`**: Gameplay rules, timers, guessing, and scoring.
    - **`presentation/`**: UI, notifications, and scoreboard management.
    - **`session/`**: Player roles, rounds, word management, and colors.
    - **`GameManager.java`**: Orchestrates the overall game flow.
    - **`GameState.java`**: Defines the different states of the game.
- **`events/`**: Mod-specific event handling (`ModEvents.java`).
- **`mixin/`**: Mixins for injecting code into Minecraft.
- **`registry/`**: Mod registry entries (e.g., `ModSounds.java`).
- **`MondayBuilder.java`**: Main common mod entry point.

### Resources (`common/src/main/resources/`)
- **`assets/mondaybuilder/`**: Sounds, textures, and fonts.
- **`mondaybuilder/`**: Custom JSON data:
    - `config.json`: Default configuration.
    - `localization.json`: Custom string localizations.
    - `words.json`: Word list for the game.
- `mondaybuilder.mixins.json`: Mixin configuration.
- `mondaybuilder.zip`: Packaged resources.

## 🛠 Commands
- `/mb start <rounds>`: Starts the game with a specific number of rounds.
- `/mb pause`: Pauses the current game state and timer.
- `/mb resume`: Resumes a paused game.
- `/mb stop`: Stops the game and returns players to the lobby.
- `/mb role <Player> <ROLE>`: Changes a player's role (Debug mode only).
- `/mb color <Player> <#HEX>`: Sets a player's color using a HEX value.

## 📦 Build & Test
- will be executed by the user only

## 📑 Project Info
- **Version**: 0.0.3
- **Architectury API**: 18.0.6
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.138.4+1.21.10

# Zencoder rules
- NEVER start developing until the user prompts for it!
- DO NOT modify any existing code unless absolutely necessary.
- Use meaningful names for variables, methods, classes, etc.
- Follow consistent indentation and formatting conventions.
- Write clear and concise comments explaining complex sections of code.
- Ensure proper error handling and logging mechanisms are in place.

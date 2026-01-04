# Monday Builder (Montagsmaler)

A Pictionary-style game mod for Minecraft 1.21.10, built using the **Architectury** framework and **Fabric** loader.

## 🚀 Core Technologies
- **Minecraft**: 1.21.10
- **Framework**: Architectury API (Multi-loader support)
- **Loader**: Fabric
- **Language**: Java 21
- **Build System**: Gradle

### Root Directory
- `common/`: Core mod logic shared across all platforms.
- `fabric/`: Fabric-specific implementation and entry points.
- `mondaybuilder/`: Sub-project for specific tools or builders.
- `.zencoder/`: AI assistant configuration and development rules.

### Common Module (`common/src/main/java/com/mondaybuilder/`)
- **`commands/`**: Command registrations and implementations.
- **`config/`**: Configuration management (`ModConfig.java`, `ConfigManager.java`).
- **`core/`**: Central game logic:
    - **`env/`**: World and arena management (Lobby, Arena).
    - **`mechanics/`**: Gameplay rules, timers, and scoring.
    - **`presentation/`**: UI, notifications, and scoreboard.
    - **`session/`**: Player roles, rounds, and word management.
    - **`GameManager.java`**: Orchestrates the game flow.
- **`events/`**: Mod-specific event handling.
- **`mixin/`**: Mixins for injecting code into Minecraft.
- **`registry/`**: Mod registry entries (e.g., sounds).
- **`MondayBuilder.java`**: Main common mod entry point.

### Resources (`common/src/main/resources/`)
- **`assets/mondaybuilder/`**: Textures, models, and translations.
- **`mondaybuilder/`**: Custom JSON data:
    - `config.json`: Default configuration.
    - `localization.json`: Custom string localizations.
    - `words.json`: Word list for the game.


## Dependencies (v1.0.0)

## 📦 Dependencies
- **Architectury API**: 18.0.6
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.138.4+1.21.10

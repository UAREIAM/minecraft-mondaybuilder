# Mondaybuilder Repository

This repository contains the source code for the **Montagsmaler** (MondayBuilder) Minecraft mod, a Pictionary-style game for Minecraft 1.21.10 using the **Architectury** framework and **Fabric** loader.

## Project Structure

- **`.zencoder/`**: Contains AI assistant configuration, documentation, and development rules.
- **`common/`**: The core mod logic shared across all platforms.
  - **`src/main/java/com/mondaybuilder/`**: Main Java source package.
    - **`commands/`**: Command registrations and implementations (e.g., `ModCommands.java`).
    - **`config/`**: Configuration management and data structures (`ConfigManager.java`).
    - **`core/`**: Central game logic divided into sub-modules:
      - **`env/`**: World and arena management.
      - **`mechanics/`**: Gameplay rules, timers, and scoring.
      - **`presentation/`**: UI, notifications, and scoreboard management.
      - **`session/`**: Player roles, rounds, and word management.
    - **`events/`**: Mod-specific event handling.
    - **`mixin/`**: Mixins for injecting code into Minecraft.
    - **`registry/`**: Mod registry entries (e.g., sounds).
    - **`MondayBuilder.java`**: Main common mod entry point.
  - **`src/main/resources/`**: Static assets and mod data.
    - **`assets/mondaybuilder/`**: Textures, models, and translations.
    - **`data/`**: Minecraft data packs and tags.
    - **`mondaybuilder/`**: Custom JSON data for `config`, `localization`, and `words`.
    - **`mondaybuilder.mixins.json`**: Mixin configuration.


## Dependencies (v1.0.0)

- **Minecraft**: 1.21.10
- **Architectury API**: 18.0.6
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.138.4+1.21.10

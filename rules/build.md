# Build & Development Rules (WSL2 + Windows)

To prevent `java.io.IOException: Input/output error` and file locking conflicts:

## 1. Environment Separation
- **Code Editing**: Use WSL2 (Linux) for development, refactoring, and git operations.
- **Execution & Testing**: ALWAYS run `./gradlew` (or `gradlew.bat`) from a **Windows Terminal** (CMD or PowerShell) for building and running the Minecraft client.
- **Reason**: The Minecraft client and Gradle Daemons must run in the same environment that holds the file locks (Windows) to avoid cross-OS mount conflicts.

## 2. Shared Cache Configuration
- Ensure `GRADLE_USER_HOME` is pointed to a Windows-native path in both environments:
    - **Windows**: `setx GRADLE_USER_HOME "C:\gradle_cache"`
    - **WSL2**: `export GRADLE_USER_HOME="/mnt/c/gradle_cache"`

## 3. Gradle Performance
- `org.gradle.vfs.watch=false`: Disabled to prevent WSL2/Windows mount sync issues.
- `org.gradle.configuration-cache=true`: Enabled to speed up initialization.
- **Memory**: 3GB allocated to JVM for smooth mod building.

## 4. Code Standards
- **Line Endings**: Always use `LF` (enforced by `.editorconfig`) to prevent checksum mismatches in shared caches.

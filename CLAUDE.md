# Proxy Perm Group

## Project Overview

ProxyPermGroup is a [Velocity](https://velocitypowered.com/) proxy plugin that automatically assigns players to a configurable LuckPerms permission group when they connect to the proxy, and removes them when they disconnect.

## Build Commands

```bash
# Build the plugin jar
./gradlew build

# Run a local Velocity server for testing (downloads Velocity automatically)
./gradlew runVelocity

# Compile only (no jar packaging)
./gradlew compileJava
```

The built jar is placed in `build/libs/`. There are no tests in this project.

## Architecture

The entire plugin logic lives in a single class: [src/main/java/org/rainbowhunter/proxypermgroup/ProxyPermGroup.java](src/main/java/org/rainbowhunter/proxypermgroup/ProxyPermGroup.java).

**Startup (`onProxyInitialization`):** Loads `config.yml`, reads the `permission_group` key, then checks LuckPerms whether that group already exists. If it does not exist, the group is created. If it does exist, all users currently holding the group node are stripped of it (cleanup for ungraceful shutdowns).

**Login (`onLoginEvent`):** Adds the configured LuckPerms `InheritanceNode` to the connecting player.

**Disconnect (`onDisconnectEvent`):** Removes the node from the disconnecting player.

**Config loading:** The plugin copies the bundled `/config.yml` to the Velocity data directory on first run, then reads it via Spongepowered Configurate (YAML). The only meaningful config key is `permission_group` (default: `velocity`).

**`BuildConstants.java`** lives in `src/main/templates/` and is processed by a Gradle `Copy` task that substitutes `${version}` before compilation. Do not edit the generated copy under `build/`.

## Key Dependencies

| Dependency                    | Role                                                            |
| ----------------------------- | --------------------------------------------------------------- |
| `velocity-api:3.4.0-SNAPSHOT` | Core proxy API (compile-only)                                   |
| `luckperms:api:5.4`           | Permission group management (compile-only, required at runtime) |
| `configurate-yaml:4.1.2`      | YAML config loading (bundled in jar)                            |

## Coding Requirement

- Keep it simple and concise.
- No emojis.
- Fix all IDE warnings and error when it is possible and appropriate.

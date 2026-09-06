# Bundle Browser - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Drop the jar in your client's `mods` folder alongside its declared dependencies (see `fabric.mod.json`). No server-side installation needed. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## Building from Source

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

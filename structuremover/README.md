# Structure Mover (NeoForge 1.21.1)

Wand that selects a whole connected building and moves it.

## Build
1. Copy `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` from any official NeoForge 1.21.1 MDK into this folder
   (or just run `gradle wrapper --gradle-version 8.10.2` if you have Gradle installed).
2. Needs JDK 21. Run `./gradlew build` -> jar is in `build/libs/`.
   (Or: drop `src/` + `build.gradle` + `gradle.properties` into the official MDK.)

## Use
- Craft: ender pearl / diamond / stick (vertical), or take it from the Tools tab in creative.
- Right-click a block of the building: selects everything connected (green box).
- Aim somewhere: orange box = where it will land (bottom-centre of the building goes on the block above the one you aim at).
- Right-click air: rotate 90 degrees.
- Right-click a block: move it. Sneak+click: cancel.
- `/structuremover undo` reverts the last move.

## Config (structuremover-common.toml)
maxBlocks, diagonalConnections, ignoreTerrain, ignoredBlocks, overwriteExisting, requireCreativeMode

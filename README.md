# MORGUE-CLIENT

A standalone client-side Minecraft project for **Minecraft Java 26.3 + Fabric**.

> "PLS USE PLS. THIS WILL CHANGE UR LIFE" — someone.

## Current milestone

- Independent Fabric client; no AUTISM Client dependency.
- Right Shift opens/closes the MORGUE ClickGUI.
- Dark charcoal interface with white typography and restrained red accents.
- Category navigation and starter module toggles.
- Build workflow uploads the compiled JAR as a GitHub Actions artifact.

## Development environment

- Minecraft: 26.3
- Java: 25
- Fabric Loader: 0.20.1
- Fabric API: 0.166.0+26.3
- Fabric Loom: 1.19-SNAPSHOT
- Gradle: 9.6.0

Run the build with:

```bash
gradle build
```

The output JAR is written to `build/libs/`.

## Controls

| Key | Action |
| --- | --- |
| Right Shift | Open / close ClickGUI |
| Escape | Close the current screen |

## Project direction

The first milestone focuses on the ClickGUI foundation. Planned UI work includes a draggable layout, searchable module list, per-module settings, keybind editor, profile manager, and a HUD editor.

Privacy features are limited to ordinary user-controlled UI preferences. The project will not implement concealment intended to evade moderation, anti-cheat, or screen-share inspections.

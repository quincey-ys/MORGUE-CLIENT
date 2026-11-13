# MORGUE-CLIENT

A standalone client-side Minecraft project for **Minecraft Java 26.3 + Fabric**.

> "PLS USE PLS. THIS WILL CHANGE UR LIFE" — someone.

## Current implementation

- Independent Fabric client with no AUTISM Client runtime dependency.
- Right Shift opens the MORGUE ClickGUI.
- Dark charcoal interface with white typography and restrained red accents.
- **Mace Hitter (original baseline):** target selection, custom entity-type filtering, range/FOV/fall-distance controls, rotation, auto-swap, auto-jump, shield-stun, and target priority. The later spear/Breach rework is intentionally excluded.
- **Auto Totem:** Smart, Switch, and PickUp inventory-search modes, hotbar/inventory priority, switch delay, and pause-while-using-item option.
- **KillAura:** independent MORGUE implementation with player/hostile/all-living target modes, range/FOV, priority, rotation, auto-sword, shield breaking, and switch-back settings.
- The GUI also includes the planned Movement, Player, Render, World, Misc, Privacy, and Config sections; modules not listed above are still prototypes/placeholders.

## Development environment

- Minecraft: 26.3
- Java: 25
- Fabric Loader: 0.20.1
- Fabric API: 0.166.0+26.3
- Fabric Loom: 1.19.4
- Gradle: 9.8.0

Build with:

```bash
gradle build
```

The output JAR is written to `build/libs/`. GitHub Actions uploads it as a workflow artifact when the build succeeds.

## Controls

| Key | Action |
| --- | --- |
| Right Shift | Open / close ClickGUI |
| Escape | Close the current screen |

## Compatibility and privacy

The combat modules are client-side gameplay features and are not guaranteed to work on every server or configuration. They use Minecraft's normal client game-mode and inventory interaction APIs; they do not attempt to forge server responses or conceal modifications from server moderation.

Privacy-related UI is limited to transparent, user-controlled client preferences. Forced server resource-pack requirements and anti-cheat/mod-detection checks are not bypassed.

## Source and licensing

The current KillAura and Mace Hitter implementations were written for MORGUE rather than copied from AUTISM Client source. MORGUE does not import AUTISM Client classes or require that client at runtime. Review third-party dependencies and their licenses before redistribution.

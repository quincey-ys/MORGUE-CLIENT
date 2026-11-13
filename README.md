# MORGUE-CLIENT

A standalone client-side Minecraft project for **Minecraft Java 26.3 + Fabric**.

> "PLS USE PLS. THIS WILL CHANGE UR LIFE" — someone.

## Current implementation

- Independent Fabric client with no AUTISM Client runtime dependency.
- Dark charcoal ClickGUI with white typography and restrained red accents.
- Right Shift opens/closes the ClickGUI; its panel can be moved, resized, and filtered with the module search field.
- **Mace Hitter (original baseline):** target selection, custom entity-type filtering, range/FOV/fall-distance controls, rotation, auto-swap, auto-jump, shield-stun, target priority, and team/wall checks. The later spear/Breach rework is intentionally excluded.
- **Auto Totem:** Smart, Switch, and PickUp inventory-search modes, hotbar/inventory priority, switch delay, and pause-while-using-item option.
- **KillAura:** independent MORGUE implementation with player/hostile/all-living target modes, range/FOV, target priority, rotation, auto-sword, shield breaking, and switch-back settings.
- **Shared Target Intelligence:** one local evaluator ranks eligible targets by distance, health, or a threat heuristic that considers entity hostility, proximity, approach movement, and held weapon. Both combat modules use the same explainable evaluator.
- **AI Brain dashboard:** displays the active combat controller, latest target assessment, selected priority, distance, threat score, health, and selection reason. This is an on-device heuristic engine, not a remote or trained model.
- Mace Hitter and KillAura are mutually exclusive to avoid competing attack loops.
- Target Intelligence centralizes ranking only; each combat module retains its own target filters and action logic.
- Settings persist in `config/morgue-client.properties`.
- The Resource Pack Manager opens Minecraft's normal local resource-pack screen.
- The Privacy Dashboard explains MORGUE's network/privacy behavior; it does not hide mods, fake server responses, or evade anti-cheat.
- Movement, Player, Render, World, and some Misc/Privacy/Config items are currently UI placeholders.

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
| M | Toggle Mace Hitter |
| K | Toggle KillAura |
| V | Toggle Auto Totem |
| Escape | Close the current screen |

M, K, and V can be rebound in **Options → Controls → Key Binds**, or opened from the Config → Keybinds panel.

## Compatibility and privacy

The combat modules are client-side gameplay features and are not guaranteed to work on every server or configuration. They use Minecraft's normal client game-mode and inventory interaction APIs; they do not attempt to forge server responses or conceal modifications from server moderation.

The Resource Pack Manager changes local client pack selections. Server-required pack requests are still handled by Minecraft and cannot be bypassed through MORGUE.

## Source and licensing

The current KillAura and Mace Hitter implementations were written for MORGUE rather than copied from AUTISM Client source. MORGUE does not import AUTISM Client classes or require that client at runtime. Review third-party dependencies and their licenses before redistribution.

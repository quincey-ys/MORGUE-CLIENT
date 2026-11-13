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

The normal Loom JAR and sources JAR are written to `build/libs/`. The protected distributable, SHA-256 checksum, build metadata, and CycloneDX SBOM are written to `build/release/` and uploaded by GitHub Actions. Install the JAR from `build/release/`.

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

## Protection and release integrity

MORGUE protects its distributed build and makes package changes visible; these controls do **not** hide the client from servers or defeat anti-cheat systems.

- **Bytecode obfuscation:** the release pipeline uses ProGuard 7.10.0 to repackage and obfuscate MORGUE's internal implementation classes. The Fabric entrypoint class and required callback are retained. Shrinking and optimization are disabled to reduce compatibility risk. The mapping file is kept locally at `build/reports/morgue/proguard-mapping.txt` and is not distributed.
- **Runtime integrity check:** at startup, MORGUE compares every packaged JAR entry (except its own manifest) against embedded SHA-256 values. Missing, unexpected, duplicated, or modified entries mark the package as failed and block MORGUE gameplay-module activation. Development runs from class folders are explicitly marked as skipped and remain usable.
- **Integrity limitation:** an embedded checksum manifest is not a digital signature. A determined person who modifies the code can also replace the manifest or patch the verifier. Use the separately distributed release checksum to compare a downloaded JAR against the build output.
- **External SHA-256:** the release bundle contains a `.sha256` sidecar. Verify it with `sha256sum -c MORGUE-CLIENT-0.1.0.jar.sha256` (use the matching versioned filename).
- **Software Bill of Materials:** the build produces CycloneDX JSON and XML SBOM files listing the resolved project dependencies.
- **Source separation:** the distributable release bundle contains the obfuscated runtime JAR and build metadata, not the unobfuscated sources JAR. The source JAR remains a local build output under `build/libs`.
- MORGUE currently has no custom telemetry or remote account service. Its normal Minecraft/Fabric networking remains subject to Minecraft, Fabric Loader, the server, and installed dependencies.

GitHub artifact attestations are conditionally enabled only for public repositories. This repository is currently private; GitHub's artifact-attestation service requires an Enterprise Cloud plan for private/internal repositories.

## Source and licensing

The current KillAura and Mace Hitter implementations were written for MORGUE rather than copied from AUTISM Client source. MORGUE does not import AUTISM Client classes or require that client at runtime. Review third-party dependencies and their licenses before redistribution.

## References and inspiration

These public projects were reviewed for architectural ideas, feature behavior, and implementation patterns while planning MORGUE. They are **research references only**: MORGUE does not bundle these clients, depend on their runtime, or claim their code as its own.

| Project | What we studied | Repository / license |
| --- | --- | --- |
| [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) | Combat target filtering, priority settings, range handling, and modular settings organization. | [GPL-3.0](https://github.com/MeteorDevelopment/meteor-client) |
| [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) | Event-driven module architecture, coordinated target state, and inventory-action scheduling patterns for offhand management. | [GPL-3.0](https://github.com/CCBlueX/LiquidBounce) |
| [Wurst 7](https://github.com/Wurst-Imperium/Wurst7) | FightBot/pathfinding concepts and how movement decisions can be separated from combat actions. | [GPL-3.0](https://github.com/Wurst-Imperium/Wurst7) |
| [Baritone](https://github.com/cabaletta/baritone) | Goal-based navigation and pathfinding concepts for autonomous movement. | [LGPL-3.0](https://github.com/cabaletta/baritone) |
| [Anomoly](https://github.com/DistilledSlop/anomoly) | Minecraft 26.3 Fabric project structure, ClickGUI ideas, and local server-profile diagnostics. | [CC0-1.0](https://github.com/DistilledSlop/anomoly) |
| [BleachHack](https://github.com/BleachDev/BleachHack) | Historical Fabric utility-client patterns for GUI organization, AutoTotem, combat modules, and automation. This project is discontinued and targets older Minecraft versions. | See the repository's license and redistribution terms. |

### Attribution and code-use note

The references above informed research and design discussions; they are not a list of MORGUE dependencies or code contributions. MORGUE's current combat and target-intelligence implementation is written for this project. No source code is intentionally copied from the projects listed above. If code is ever copied or adapted from any reference, its exact license, attribution requirements, source-disclosure obligations, and compatibility with MORGUE's distribution license must be reviewed before merging or distributing that change. A repository's license applies to its own covered code; dependencies and bundled components may have separate terms.


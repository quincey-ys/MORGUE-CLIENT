# MORGUE-CLIENT

A standalone client-side Minecraft project for **Minecraft Java 26.3 + Fabric**.

> "PLS USE PLS. THIS WILL CHANGE UR LIFE" — someone.

## Current implementation

- Independent Fabric client with no AUTISM Client runtime dependency.
- Gothic midnight-blue and black ClickGUI with mathematical double-struck typography, steel-blue borders, icy-blue accents, animated controls, and a scrollable module list.
- Right Shift opens/closes the ClickGUI; its panel can be moved, resized, and filtered with the module search field.
- **Mace Hitter (original baseline):** target selection, custom entity-type filtering, range/FOV/fall-distance controls, rotation, auto-swap, auto-jump, shield-stun, target priority, and team/wall checks. The later spear/Breach rework is intentionally excluded.
- **Auto Totem:** Smart, Switch, and PickUp inventory-search modes, hotbar/inventory priority, switch delay, pause-while-using-item option, configurable health activation threshold, recent-damage, fall-damage, and nearby-hostile pressure heuristics, and optional restoration of the displaced offhand item after the totem is consumed.
- **KillAura:** independent MORGUE implementation with player/hostile/all-living/custom entity target modes, a paginated entity picker, persisted entity selections, range/FOV, target priority, hurt-time and minimum-age filters, vanilla-critical-only option, throwable pause, rotation, auto-sword, shield breaking, and switch-back settings.
- **Shared Target Intelligence:** one local evaluator ranks eligible targets by distance, health, or a threat heuristic that considers entity hostility, proximity, approach movement, and held weapon. Both combat modules use the same explainable evaluator.
- **AI Brain dashboard:** displays the active combat controller, latest target assessment, selected priority, distance, threat score, health, and selection reason. This is an on-device heuristic engine, not a remote or trained model.
- **Shared Combat Action Coordinator:** Mace Hitter and KillAura may both be enabled. Mace Hitter gets first opportunity to claim one attack action per client tick; KillAura acts only when Mace Hitter does not attack.
- Target Intelligence centralizes ranking only; each combat module retains its own target filters and action logic.
- Settings persist in `config/morgue-client.properties`; three named profile slots can be exported/imported under `config/morgue-client/profiles/`.
- The Resource Pack Manager opens Minecraft's normal local resource-pack screen.
- The Privacy Dashboard explains MORGUE's network/privacy behavior; it does not hide mods, fake server responses, or evade anti-cheat.
- **Movement:** optional auto-sprint while horizontal movement is detected and Safe Walk edge protection that preserves the user's own sneak key.
- **World Info / Environment / Client Metadata:** local player coordinates, health, absorption, rain/thunder state, world time, target version, and privacy-scope information.
- **Module List / Movement Settings / Visual Settings:** live module status and the current movement/theme configuration.
- **Inventory Helper:** refills low-count hotbar stacks from matching main-inventory stacks through vanilla container input; AutoTotem has priority if both request an inventory action in the same tick.
- **Auto Eat:** optionally selects the most nutritious hotbar food at a configurable hunger threshold, uses vanilla item-use input, and restores the previous slot after eating.
- **Notifications:** optional local chat notices for AutoTotem and Inventory Helper actions; notifications remain on-device.
- **Profiles:** three local configuration slots with import/export. Safe Walk and Inventory Helper are functional; World Settings, Screenshot Privacy, Client Utilities, and Notifications now have dedicated screens. Custom HUD layout, Aim Assist, Click Assist, Theme editing, and several other entries remain informational or unfinished.

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
- **Runtime integrity check:** at startup, MORGUE compares every packaged JAR entry (except its own manifest) against embedded SHA-256 values. Missing, unexpected, duplicated, or modified entries mark the package as failed and block MORGUE gameplay-module activation. Only a confirmed Fabric development environment can skip the packaged-JAR check; an unrecognized or unverifiable production package fails closed and blocks MORGUE gameplay-module activation.
- **Integrity test:** CI loads the obfuscated verifier from the packaged JAR, checks that a clean build reports `VERIFIED`, then adds a test entry to a copy and checks that it reports `FAILED` and denies module activation.
- **Integrity limitation:** an embedded checksum manifest is not a digital signature. A determined person who modifies the code can also replace the manifest or patch the verifier. Use the separately distributed release checksum to compare a downloaded JAR against the build output.
- **External SHA-256:** the release bundle contains a `.sha256` sidecar. Verify it with `sha256sum -c MORGUE-CLIENT-0.1.0.jar.sha256` (use the matching versioned filename).
- **Software Bill of Materials:** the build produces CycloneDX JSON and XML SBOM files listing the resolved project dependencies.
- **Source separation:** the distributable release bundle contains the obfuscated runtime JAR and build metadata, not the unobfuscated sources JAR. The source JAR remains a local build output under `build/libs`.
- MORGUE currently has no custom telemetry or remote account service. Its normal Minecraft/Fabric networking remains subject to Minecraft, Fabric Loader, the server, and installed dependencies.

GitHub artifact attestations are conditionally enabled only for public repositories. This repository is currently private; GitHub's artifact-attestation service requires an Enterprise Cloud plan for private/internal repositories.


## Credits and acknowledgements

- **Project lead and maintainer:** `quincey-ys` — project direction, feature requirements, testing feedback, and release decisions.
- **AI-assisted development:** **OpenAI ChatGPT** — assisted with planning, code drafting, debugging, technical explanations, and interface iteration. This is an acknowledgement of AI assistance, not a claim that the model is the project owner or sole author.
- **Minecraft:** Minecraft Java Edition is developed by Mojang Studios, part of Microsoft. MORGUE is an independent, unofficial project and is not affiliated with, endorsed by, or sponsored by Mojang Studios or Microsoft.
- **Mod platform:** Fabric Loader and Fabric API are maintained by the Fabric community / FabricMC.
- **Build and protection tooling:** Gradle, Fabric Loom, and ProGuard (Guardsquare).
- **Software bill of materials:** CycloneDX Gradle plugin and the CycloneDX project.

The research projects listed in [References and inspiration](#references-and-inspiration) are credited separately as research references only; they are not presented as MORGUE contributors or runtime dependencies. Third-party dependency authors and their license terms remain applicable to the corresponding components.

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
| [AUTISM Client](https://github.com/AutismDevelopment/Autism-Client) | AutoTotem and KillAura feature behavior and module architecture; reviewed as a research reference only, without copying its source code. | [GPL-3.0](https://github.com/AutismDevelopment/Autism-Client) |
| [BleachHack](https://github.com/BleachDev/BleachHack) | Historical Fabric utility-client patterns for GUI organization, AutoTotem, combat modules, and automation. This project is discontinued and targets older Minecraft versions. | See the repository's license and redistribution terms. |

### Attribution and code-use note

The references above informed research and design discussions; they are not a list of MORGUE dependencies or code contributions. MORGUE's current combat and target-intelligence implementation is written for this project. No source code is intentionally copied from the projects listed above. If code is ever copied or adapted from any reference, its exact license, attribution requirements, source-disclosure obligations, and compatibility with MORGUE's distribution license must be reviewed before merging or distributing that change. A repository's license applies to its own covered code; dependencies and bundled components may have separate terms.


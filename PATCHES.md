# MORGUE Patch Index

These changes are separate commits on `main`. The index keeps each patch identifiable.
The full fix is the combined stack; the CI workflow patch depends on the enum smoke-test file.

## Patch 01 — Preserve enum classes in ProGuard
- Commit: [227eef47](https://github.com/quincey-ys/MORGUE-CLIENT/commit/227eef47ee7ab6f2c54c274f829f52c94e208d6f)
- File: `build.gradle`
- Adds a ProGuard keep rule so enum classes and constants retain JVM enum identity.

## Patch 02 — Remove reflective priority conversion from Mace Hitter
- Commit: [bb8e594e](https://github.com/quincey-ys/MORGUE-CLIENT/commit/bb8e594ec19791e34ac5f003eda28fdba431569a)
- File: `src/client/java/dev/morgue/client/modules/combat/MaceHitterModule.java`
- Replaces `Enum.valueOf()` conversion between matching priority enums with an explicit mapping.

## Patch 03 — Remove reflective priority conversion from KillAura
- Commit: [5eb805a9](https://github.com/quincey-ys/MORGUE-CLIENT/commit/5eb805a9fc172949fd59c7375a607a9c7c48ed88)
- File: `src/client/java/dev/morgue/client/modules/combat/KillAuraModule.java`
- Applies the same explicit priority mapping in KillAura.

## Patch 04 — Make config enum loading fail safely
- Commit: [91515afd](https://github.com/quincey-ys/MORGUE-CLIENT/commit/91515afd72df3ed15d7dcd888ecfe2960eeb9b35)
- File: `src/client/java/dev/morgue/client/config/MorgueConfig.java`
- Resolves requested enum values from the constants array and falls back when the value is invalid or unavailable.

## Patch 05 — Add the packaged-enum CI check
- Commit: [0a0ad154](https://github.com/quincey-ys/MORGUE-CLIENT/commit/0a0ad1548f15a3d565891d4059f9578595ab241c)
- File: `.github/workflows/build.yml`
- Compiles and runs the enum smoke test during protected-release verification.

## Patch 06 — Add the enum runtime smoke test
- Commit: [e82b2407](https://github.com/quincey-ys/MORGUE-CLIENT/commit/e82b2407b8e97f76347566cc53062b3f0740d121)
- File: `tools/EnumSmokeTest.java`
- Loads enum classes from the protected JAR and checks every constant through `Enum.valueOf()`.

## Application note

All six commits are on `main` already. Use the latest main build for testing; do not cherry-pick only Patch 05 without Patch 06, because the workflow invokes the smoke-test file added by Patch 06.

## Patch 07 — Remove duplicate vanilla button captions and add module scrolling
- Commit: [e72087ab](https://github.com/quincey-ys/MORGUE-CLIENT/commit/e72087abaae8bbceec7f96a3ec8a826ffd07e500)
- File: `src/client/java/dev/morgue/client/MorgueScreen.java`
- Temporarily suppresses vanilla button captions while the base widgets render, then restores their labels and draws the custom MORGUE controls once. This removes the doubled text visible in the screenshot.
- Adds mouse-wheel scrolling and a compact scroll indicator to the module list; the header and category sidebar remain fixed.

## Patch 08 — Keep the module list in a compact scroll viewport
- Commit: [9d1bd67e](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9d1bd67e911895c98aaa28a3a57ed52565914c77)
- File: `src/client/java/dev/morgue/client/MorgueScreen.java`
- Caps the visible module rows at four so the Combat page can scroll even when the window is tall. Mouse-wheel scrolling moves through the remaining modules while the surrounding UI stays fixed.

## Patch 09 — Mathematical double-struck UI typography
- Commit: [5d9ab099](https://github.com/quincey-ys/MORGUE-CLIENT/commit/5d9ab099f04068fe0b53bac4abf8724a9fb2e7a3)
- File: `src/client/java/dev/morgue/client/MorgueScreen.java`
- Converts Latin letters and digits in rendered ClickGUI labels to the mathematical double-struck Unicode alphabet shown in the user's example. Uses Minecraft's default Unicode-capable font provider for those glyphs so the Oxanium TTF does not replace them with missing-glyph boxes.

## Patch 10 — Do not block direct mace-in-hand attacks on vanilla cooldown
- Commit: [ae8f3869](https://github.com/quincey-ys/MORGUE-CLIENT/commit/ae8f38697fe4d3a9764eb9b1cbf1052edf960aac)
- File: `src/client/java/dev/morgue/client/modules/combat/MaceHitterModule.java`
- Skips the client-side vanilla attack-strength cooldown gate when the player is already holding a mace. Mace Hitter's falling-smash conditions and its own Attack Delay setting are preserved. The server still controls whether an attack is accepted and deals damage.

## Patch 11 — Configurable KillAura entity targets
Implementation is split into ordered commits:
- [11a — Add CUSTOM targeting and selected entity types](https://github.com/quincey-ys/MORGUE-CLIENT/commit/a16a49dd8d5f170a1cc3609bc008379129cdf51c) — `KillAuraModule.java`
- [11b — Persist entity selections in config](https://github.com/quincey-ys/MORGUE-CLIENT/commit/febfa6180837c1d835a2881b29d0447c1cbe44d1) — `MorgueConfig.java`
- [11c — Add entity selector UI](https://github.com/quincey-ys/MORGUE-CLIENT/commit/58104474b2b2a34f2c028c4705be7e2bfc1fbef2) — `MorgueScreen.java`
- [11d — Align selector layout](https://github.com/quincey-ys/MORGUE-CLIENT/commit/7df424d454f12aa465c9fbaf814b0c2315ea1397) — `MorgueScreen.java`
- [11e — Keep Other from implicitly selecting players](https://github.com/quincey-ys/MORGUE-CLIENT/commit/0d0795c7cae7d677b7bfe726225b9672784dc28e) — `KillAuraModule.java`

KillAura now supports PLAYERS, HOSTILE_MOBS, ALL_LIVING, and CUSTOM. CUSTOM uses the shared paginated entity picker, persists selected registry IDs under `aura.entityTypes`, and keeps the player checkbox independent from the Other option.

## Patch 12 — Gothic midnight-blue ClickGUI palette
- Commit: [3ce2b362](https://github.com/quincey-ys/MORGUE-CLIENT/commit/3ce2b362951d747bce75b78fccf2ef859f7dcc1d)
- File: `src/client/java/dev/morgue/client/MorgueScreen.java`
- Replaces the crimson accents with icy blue highlights, shifts panels and controls to deeper black/navy tones, and recolors borders, hover sweeps, scanlines, corner brackets, search outlines, scroll thumb, and status indicator. Keeps the existing layout, typography, animation behavior, and module interactions.

## Patch 13 — Project and development credits
- [13a — Add an in-client Credits page](https://github.com/quincey-ys/MORGUE-CLIENT/commit/2ac1f3c2c72806713a5c145243d0a79db515aa18) — `MorgueScreen.java`. Adds Credits under Misc, with project lead, AI-assisted development acknowledgement, Minecraft/Fabric attribution, build tooling, SBOM tooling, and research-reference acknowledgements.
- [13b — Add README credits and acknowledgements](https://github.com/quincey-ys/MORGUE-CLIENT/commit/5e01e8bcb532e4d1d615e4bba0610531c5049bf8) — `README.md`. Documents project ownership, ChatGPT development assistance, platform/tooling credits, and Minecraft non-affiliation.

## Patch 14 — Shared combat action coordination
- [14a — Add one-action-per-tick coordinator](https://github.com/quincey-ys/MORGUE-CLIENT/commit/1d6a6d2cdcc05256edfea13cf91e57dbcbf10db8) — new `CombatActionCoordinator.java`.
- [14b–14c — Route Mace Hitter attacks through the coordinator](https://github.com/quincey-ys/MORGUE-CLIENT/commit/43e38f3343cb48424d23401d9330244f594fc740) — shield stun and falling smash share the action claim.
- [14d–14f — Evaluate Mace Hitter before KillAura and allow both to stay enabled](https://github.com/quincey-ys/MORGUE-CLIENT/commit/7290cd3b8b76f55c2f98f104fcab79d4a7297e81) — KillAura can act when Mace Hitter has no eligible attack.
- [14g — Persist both enabled states](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9a71667af52accbcd57dab78851ad65e311aa2ee) — removes the old startup mutual-exclusion rule.

## Patch 15 — AutoTotem health activation
- [15a — Add configurable effective-health threshold](https://github.com/quincey-ys/MORGUE-CLIENT/commit/ac87e283c86325d52a479fa8aede3e2b8d6c9240) — threshold includes absorption hearts; setting 20 keeps the previous always-active behavior.
- [15b–15d — Persist the threshold and preserve the default](https://github.com/quincey-ys/MORGUE-CLIENT/commit/31a768458aea41c144a764ae8363e15a66868812).

## Patch 16 — Expanded KillAura controls
- [16a — Add hurt-time filter, minimum entity age, vanilla-critical-only gate, throwable pause, and shared attack arbitration](https://github.com/quincey-ys/MORGUE-CLIENT/commit/6ea9af21ace84961f32d991cc725770bbb3c308b).
- [16b — Persist the new KillAura settings](https://github.com/quincey-ys/MORGUE-CLIENT/commit/01226fbaebe3ac4db0c36e50f5b49e1537292965).
- [16c–16d — Expose the settings in ClickGUI and correct row layout](https://github.com/quincey-ys/MORGUE-CLIENT/commit/2363c4f44c5614d0b88aee3a1846c5c1f3e655ed).

## Patch 17 — Update feature and research documentation
- [Commit](https://github.com/quincey-ys/MORGUE-CLIENT/commit/d1f1789f3bf1d6d58f05e247d1ae57a466624b7b) — documents shared combat arbitration, new settings, remaining prototype screens, and AUTISM Client as a GPL-3.0 research reference only.

## Patch 18 — Local configuration profiles
- [18a — Add validated profile import/export](https://github.com/quincey-ys/MORGUE-CLIENT/commit/78257183b6ea9912e0c6ef4d2f7349489605a8a4) — saves and loads named properties snapshots under `config/morgue-client/profiles`; profile names are restricted to safe filename characters.
- [18b — Add three profile slots in ClickGUI](https://github.com/quincey-ys/MORGUE-CLIENT/commit/94f5fce3135293898ee1b3e8502fb720ca939f02) — save/load slots 1–3 and display operation status.

## Patch 19 — Functional Sprint module
- [19a–19b — Add Sprint module and register its client tick](https://github.com/quincey-ys/MORGUE-CLIENT/commit/ab6972bd6979b63c2f98f104fcab79d4a7297e81) — enables sprint while horizontal movement is detected.
- [19c–19d — Persist and expose Sprint controls](https://github.com/quincey-ys/MORGUE-CLIENT/commit/b02f9338872172ee791fc48b2848284b2c71e071).

## Patch 20 — World Info and Client Metadata
- [Commit](https://github.com/quincey-ys/MORGUE-CLIENT/commit/f60e9bad3c8e1fb6f0b3e17a14a8fc34f246eadd) — adds live player coordinates, health, absorption, world time, client target version, and privacy-scope screens.
- [Documentation](https://github.com/quincey-ys/MORGUE-CLIENT/commit/e5f8d7f4ce7923d296b5904fe0f55e011e4372ab) — updates README with the new screens and remaining prototype list.

## Patch 21 — Safe Walk
- [21a — Add projected-edge crouch protection](https://github.com/quincey-ys/MORGUE-CLIENT/commit/8e8c666d089fe7289c7346b2cd52d83d48677db6) — checks the projected movement support block and uses the vanilla sneak state; user-held sneak takes precedence.
- [21b–21d — Register, persist, and expose Safe Walk](https://github.com/quincey-ys/MORGUE-CLIENT/commit/b9e910d3e195a0342e7b3f5f8e8734a405b78a05).

## Patch 22 — Inventory Helper
- [22a — Add hotbar refill using normal container input](https://github.com/quincey-ys/MORGUE-CLIENT/commit/620c27eec351d890c2eb4423941ad1a9120ee553) — swaps low-count hotbar stacks with matching item-and-component stacks from the main inventory.
- [22b–22d — Register, persist, and expose Inventory Helper](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9e6592c55e65e2b4b14776d1d571c7cdac3c1583).

## Patch 23 — Serialize automated inventory actions
- [23a — Add shared inventory action coordinator](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9e59308d6ac09cb156b5971a670dccd86a3f8615).
- [23b–23c — Give AutoTotem priority and serialize Inventory Helper](https://github.com/quincey-ys/MORGUE-CLIENT/commit/3b317865d0175042a99f3be81dfc9a57dc1e976e).
- [Documentation](https://github.com/quincey-ys/MORGUE-CLIENT/commit/59503188655f2b5bbcc5cd4e96fc7f4d33b6b99b).

## Patch 24 — Additional functional status and settings screens
- [24a — Add Movement Settings, live enabled-module list, Environment status, and Visual Settings pages](https://github.com/quincey-ys/MORGUE-CLIENT/commit/ffb0f755589e11decd2862e1819f654e302560eb).
- [24b — Document the new pages](https://github.com/quincey-ys/MORGUE-CLIENT/commit/e50d9b87ef4aad3c7aec094a43a813e01f3e58e5).

## Patch 25 — Local action notifications
- [25a — Add local-only notification helper](https://github.com/quincey-ys/MORGUE-CLIENT/commit/7df846b8db0426f7605bc5e52a03c3d72a15b051).
- [25b–25c — Notify on AutoTotem and inventory refill actions](https://github.com/quincey-ys/MORGUE-CLIENT/commit/26eb24bd530e20cf55087a726064ebc5fee0934a).
- [25d–25e — Persist notification preference and expose it in ClickGUI](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9fa9df0a654b9deae5ffbc1fe85bf0ff4edf2321).
- [Documentation](https://github.com/quincey-ys/MORGUE-CLIENT/commit/9c71b13ec4e046219959d17361b0c2247840e8b7).

## Patch 26 — Replace additional placeholder screens
- [26a — Add Client Utilities navigation, Screenshot Privacy disclosure, World Settings disclosure, Item Use status, and HUD Editor status](https://github.com/quincey-ys/MORGUE-CLIENT/commit/af8ae2bfe8cb88f706079138f9c50b077c64a43a).
- [Documentation](https://github.com/quincey-ys/MORGUE-CLIENT/commit/f864108c69721845bbe057a03cbf9f75e22c0a81).

## Patch 27 — AutoTotem prediction and offhand restoration
- [27a — Add recent-damage and fall-damage health estimates plus displaced-offhand restoration](https://github.com/quincey-ys/MORGUE-CLIENT/commit/659b5c532177d5c2c90a1ae8d0ad9be2edef987b).
- [27b–27c — Persist and expose the new AutoTotem options](https://github.com/quincey-ys/MORGUE-CLIENT/commit/1de340f8e5aba65a7041f21198eb137712ae70b9).
- [Documentation](https://github.com/quincey-ys/MORGUE-CLIENT/commit/52caa7ee929bd92de88141a13d80276b04160eca).

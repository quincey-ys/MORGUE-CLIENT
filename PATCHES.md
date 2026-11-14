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

# Combined addon verification

## Contents and source boundaries

| Module | Version | Source |
| --- | --- | --- |
| Grand Explosion / Thundercrash / Thunderstar / Cinderstar | 0.3.3 | `src/` |
| Crimson Susanoo | 0.1.1 | `modules/crimson-susanoo/` |
| Ignis Armor Compatibility | 0.1.0 | `modules/ignis-armor-compat/` |

The modules retain their own registry namespaces, configs, authors and mod IDs in one production JAR. The root package uses SRG names; the independent ForgeGradle modules use official mappings and are reobfuscated before assembly. Both required mixin configurations are retained. Dependencies, worlds, personal recordings, credentials and test-output logs are excluded from source history and the release.

## Artifact identity

```text
multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-thunderstar-recall3.jar
SHA-256: D74BFA26C913D2E3C540D81ED8A468D7F8A32817A169D9516543E275DCCBE4F0
```

This is the exact final combined JAR tested on October 2, 2026, rather than either failed intermediate recall candidate. Its `.integration.json` sidecar records all inputs and 132 preserved payload entries; only `META-INF/mods.toml`, `META-INF/MANIFEST.MF` and `pack.mcmeta` are merged.

| Input | SHA-256 |
| --- | --- |
| `grand-explosion-0.3.3-thunderstar-visual3.jar` | `A2CEA39971E6FC3B7782EB8FAA69D411BF5F5B85BCAA31813DD112CE3A1A8C4B` |
| `crimson_susanoo-0.1.1.jar` | `8E87D88E84859F1AC4AC6109FD957C0146CD7FFF2D39674D20C9A5615A714F21` |
| `ignis_armor_compat-0.1.0.jar` | `488A355F6D09BD6B8746B321180F64C0EBE38DBDEA698BC0F3311BCF44E95919` |

## Completed checks

| Check | Evidence |
| --- | --- |
| Java 17 root/module compilation; module reobfuscation | Passed |
| Guardian assets/release | 22 bones, 118 cubes, 11 clips; dependency classes excluded |
| Guardian interpolation and clocks | 202,319 checks across 249 axis tracks at 30/60/144 FPS |
| Guardian foot planting | 630,149 checks; worst planted-corner drift 0.00000034 blocks |
| Full blade/pose clearance | 17,334 poses across 54 scenarios; minimum steel clearance 0.165676 blocks |
| Guardian final required-dependency server | 278 passed, zero failed, normal exit 0 |
| **Exact final combined JAR in copied full pack, Forge 47.4.10** | **278 guardian checks passed, zero failed; startup completed** |
| Live second-cast recall | Owner confirmed recall/fade and cooldown; client/server JAR hashes matched |
| Thunderstar gameplay | 63 copied-pack checks, zero failed, normal exit 0; stats, slot/NBT and unique damage multiplier |
| Player-driven Thundercrash damage | Identical zero-armor targets: 40.00 without cuirass; 55.44 with it = 40 × 1.05 × 1.10 × 1.20 |
| Refined Thunderstar appearance | Owner reported improved appearance in disposable client; texture lookup corrected |
| Ignis enchanting | 499 copied-pack checks, zero failed, normal exit 0; actual table/anvil menus, XP/lapis/NBT and compatible registered enchants |
| Thundercrash audio | STB Vorbis decoding and ten sound lifecycle checks passed; owner reported live spell played well |

The earlier armor checks used predecessors whose armor gameplay and Ignis payloads are unchanged in this final combined artifact. Final recall diagnostics additionally exercised the exact final combined artifact. The per-entry assembly report verifies the input payload preservation. Separate server runs are documented as separate results, not one all-feature run.

## Limits and deployment

Two-human-client visuals, real transport latency and production performance remain checks for the server owners. The changed right-hand rig has startup/log checks and numerical validation; these do not establish a new continuous remote movement recording. Automatic enchantment eligibility checks do not establish every third-party enchantment's gameplay effect. Detailed armor cloth motion was not independently recorded.

Install the same combined JAR on matching clients and server after stopping them and backing up the world/configs. Remove older combined and separate module JARs, retain normal dependencies, and omit diagnostic flags. Grand Explosion retains its existing terrain/PvP configuration. For Susanoo co-op play, keep `friendlyFire=false`.

Worlds previously using the standalone Crimson resource pack may report an old `mod:crimson_susanoo` pack entry missing on their first combined startup: Forge exposes the shared resources through the root pack entry. All feature resources remain present; preserve the root pack entry and verify crafting after that transition.

See [Susanoo details](modules/crimson-susanoo/TESTING-0.1.1.md), [recall](CRIMSON-RECALL.md), [Thunderstar](THUNDERSTAR-CUIRASS.md), [Thundercrash](THUNDERCRASH-POLISH.md) and [Ignis](IGNIS-ARMOR-INTEGRATION.md). Publishing this update branch does not merge it into `main` or install it on a production server.

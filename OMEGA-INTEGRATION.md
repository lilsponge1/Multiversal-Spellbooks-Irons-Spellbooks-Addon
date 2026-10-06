# Omega Rush combined release

This branch adds the completed Omega Rush 0.1.0 spell to Multiversal Spellbooks as one installable JAR. Grand Explosion / Thundercrash 0.3.3, Crimson Susanoo 0.1.1, and Ignis Armor Compatibility 0.1.0 keep their existing code, assets, IDs, dependencies, and settings. Omega Rush keeps `irons_omega_rush:omega_rush` and its existing config filenames, so standalone scrolls and settings remain valid after upgrading.

## Artifact

`multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.1.0.jar`

SHA-256: `0B698D1635ED84618BFE297CF0AC752A761B163A9DF88DCD866AFF88E4006DBB`

The accepted base input is the previously published `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-thunderstar-recall3.jar`, SHA-256 `D74BFA26C913D2E3C540D81ED8A468D7F8A32817A169D9516543E275DCCBE4F0`. Omega Rush was compiled from `modules/omega-rush/` using Java 17 and the exact Forge/Iron's runtime. Assembly preserves 205 non-shared payload entries across the two inputs. Only `META-INF/mods.toml`, `META-INF/MANIFEST.MF`, and `pack.mcmeta` are merged.

The output contains four mod IDs in a single file and all three Mixin configurations. The preserved independent mod IDs are the same packaging convention already used by this repository. No nested module JARs, test harnesses, or third-party dependencies are bundled.

## Verification

- Seven packaging tests passed, including four-module assembly, optional Ignis compatibility, appending Omega Rush to an accepted combined base, identical output from both assembly paths, all Mixin configurations, collision rejection, and signed/dependency input rejection.
- `tools/verify_omega_integration.py` verified all 205 payload entries against the inputs, dependency and mod metadata, all referenced Mixin classes, mono 44.1 kHz Vorbis sound resources, and exclusion of the test harness. The `.integration.json` and `.verification.json` release sidecars record these results.
- The same combined JAR passed **114 assertions** on the disposable copied-pack dedicated server (Minecraft 1.20.1, Forge 47.4.10, Iron's Spells 3.16.3). Checks include native Nature Scroll Forge crafting and inscription, sound registration, flight/steering, collision and lifecycle cleanup, 4.05-block spherical damage, three-hit limits, and actual Thundercrash arithmetic/mutual movement ownership. The separate development-only harness is excluded from the production JAR. Runtime evidence is recorded in the local `build/omega-combined-validation.log` and the release's `.runtime.json` summary.

An initial exact-health assertion exposed random critical hits from the pack's Apothic Attributes mod: three recorded successful hits sometimes removed 42 rather than 36 health. The test fixture now sets only its synthetic caster's critical chance to zero for this assertion and restores it afterward. It also explicitly checks the successful-hit count. No production spell or damage behavior was changed to satisfy the fixture.

Earlier standalone gameplay, networking, visual, and sound validation is recorded in [the module's test report](modules/omega-rush/TESTING.md). This integration preserves the completed module payload. Live two-player graphics and audio were not repeated for the one-file assembly; moving ships and arbitrary future mod/driver versions remain outside this acceptance run. Production client and server folders were not edited.

## Install

Replace the previous combined addon and standalone Omega Rush JAR with this same single combined file on the server and every client. Remove any separate copies of Grand Explosion, Crimson Susanoo, or Ignis compatibility. Keep the ordinary dependency mods. Existing Omega Rush worlds retain saved settings; set `damageRadius = 4.05` in the world's `serverconfig/irons_omega_rush-server.toml` if it still has the earlier 3.0 value.

Craft Omega Rush using Legendary Ink, Paper, and Poisonous Potato in the Scroll Forge, then inscribe it normally. Operator command: `/createScroll irons_omega_rush:omega_rush 1`.

## Reproduce

Build Omega Rush with `modules/omega-rush/build.ps1`, supplying runtime paths as needed. Assemble with `build-combined.ps1 -CombinedBaseJar <accepted-previous-jar>` or with the normal separately compiled module inputs. The assembly requires Python 3.11+ and refuses to overwrite accepted outputs. `-OmegaJar ''` in module-input mode reproduces the older packaging without Omega Rush. The current build is distributed as a release asset, not a committed binary.

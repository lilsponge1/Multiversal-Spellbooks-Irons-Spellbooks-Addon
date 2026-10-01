# Grand Explosion addon

Standalone Forge 1.20.1 addon for Iron's Spells 'n Spellbooks 3.16.3. It adds Grand Explosion, Thundercrash, the Cinderstar Staff, and Cinderstar Regalia. See [PLAN.md](PLAN.md) for design and validation scope.

## Build

Run `./build.ps1` from this directory with PowerShell. The script currently uses sibling development-workspace paths for the JDK, Forge libraries, and exact Iron's Spells and Ballistix JARs; adapt those paths for another computer. The release output is `build/grand-explosion-0.3.0.jar`. The separate `build-test.ps1` produces a disposable-world test mod and must not be installed with the release.

## Install and use

The audited Thundercrash distribution is `build/release/grand-explosion-0.3.0.jar` (checksum beside it). It includes the final source-audit changes and passed 118 disposable-server assertions; the same binary passed the Grand Explosion regressions. The older root-level `build/grand-explosion-0.3.0.jar` is preserved as the build the user tested in GPT. See the Thundercrash testing document for runtime evidence and deferred multiplayer/ship checks.

Thundercrash was accepted as finished on September 30, 2026 after the user confirmed steering, visuals, sound and lifecycle cleanup. Multiplayer/ship checks and broader latency/performance measurements remain deferred. The final distribution has not been installed into GPT automatically.

Copy only `grand-explosion-0.3.0.jar` into both the Forge server and matching clients' `mods` directories. Its display name is "Grand Explosion" and its author field says "Sponge." The spell ID is `irons_ultimate_explosion:grand_explosion`. The craftable `cinderstar_staff` comes imbued with Grand Explosion V; an admin can also put the spell into a spellbook. Cinderstar Regalia has four armor items: `cinderstar_hat`, `cinderstar_robe`, `cinderstar_leggings`, and `cinderstar_boots`. The hair-free model has cream shoulders, a crimson robe with a dark front panel, one light and one dark leg, orange boots, and a separate pointed hat that leaves the player's face visible. Each item is a smithing upgrade from the matching Pyromancer piece using Cinder Essence as the template and a Nether Star as the addition. Grand Explosion damages entities within 60 blocks of the impact. Wearing all four pieces doubles its center damage before the 320 cap and extends its entity damage radius to 80 blocks. The crater radius does not change with the set.

The default server config allows a crater when Iron's `spellGriefing` is also enabled, and allows Grand Explosion to damage **other players**. The caster is always excluded from the spell's entity target list. `pvpDamage=false` disables damage to other players; the server's own PvP setting and normal protection rules still apply. After casting, the caster gets 15 seconds of Slowness II by default (configurable from 0 to 20 seconds). With Ballistix installed, the crater uses its nuclear raycast, energy, block explosion hooks, occasional surface fire, and block-break debris; it does not create radiation or irradiated blocks. Ballistix's `EXPLOSIVE_NUCLEAR_SIZE` defaults to 45 in this pack, but is a raycast setting rather than a strict spherical radius. Without Ballistix, the addon uses a downward bowl with configurable `craterRadius` (45 by default). Existing worlds with saved settings must set `terrainDamage=true`, `pvpDamage=true`, `visualRadius=128`, and `exhaustionSeconds=15` to use these defaults.

When Ballistix 1.1.1 is installed on a client, the detonation calls its nuclear shockwave and mushroom-cloud presentation; without it, the addon uses its standard-particle fallback. The detonation plays Ballistix's nuclear explosion sound at the same volume and pitch as its nuke, including its nearby-player reinforcement when blocks start breaking. Alex's Caves plus vanilla sounds are the fallback when Ballistix is absent. Charge sparks accelerate at the locked target point and are also played near the caster so they remain audible at a 48-block cast range; the final pulses add lightning cracks. With Traveloptics installed, detonation also creates a brief Extinction-style white flash. These mod integrations are optional. The release JAR does not contain assets from those mods or modify Iron's Spells.

## Copied-server checks

The full copied pack started on Forge 47.4.10 with this addon. A disposable harness reported:

| Check | Result |
| --- | --- |
| Current mana deducted; nearby entity damaged | Pass |
| Zero-mana cast fizzled | Pass |
| Earlier terrain-off setting preserved a stone block | Pass (earlier default) |
| Staff +30% Fire Spell Power and +10% Cooldown Reduction in main hand only | Pass |
| Staff recipe and Iron's staff tag loaded | Pass |
| Earlier 24-block crater removed stone while preserving chest and bedrock | Pass (earlier design) |
| Canceled block-break event preserved protected stone | Pass |
| Earlier 24-block crater queued 29,919 candidates and finished without a tick-overrun warning | Pass (earlier design) |
| Four armor pieces, smithing recipes, and Fire Spell Power armor attribute | Pass |
| Earlier full-set damage bonus, 32-block entity radius, and caster exclusion | Pass (earlier design) |

The disposable harness is under `src/test` and is excluded from the release JAR. The staff pose, mana-consuming cast, colored charge, and Ballistix detonation were checked in the GPT Prism copy with Solas enabled and disabled. The shaders-off rerun showed the large orange-red blast and gray mushroom cloud; Solas was restored afterward. A later actual staff cast displayed Ballistix's “Nuclear bomb detonates” subtitle after the server sound-delivery fix, and the player confirmed the sound was right. The revised Cinderstar armor was visually checked in the GPT client from the front and back; its face is visible below the hat brim. The earlier full-set damage and radius checks passed on the copied server; the latest 60/80-block damage radius change compiled but has not had a separate gameplay check. The PvP harness confirmed that Grand Explosion damages another ordinary ServerPlayer, leaves its caster unharmed, and spares the other player when `pvpDamage=false`; the harness clears the synthetic players' initial spawn protection before measuring damage. Pressing E in the GPT creative-mode client caused a separate Fetzi's Displays creative-tab exception (`The stack count must be 1`), so inventory inspection in that copy remains blocked by that mod. The client particle-quality option controls the addon's charge and fallback particles; Ballistix controls the density of its own nuclear effect.

## Thundercrash (0.3.0)

`irons_ultimate_explosion:thundercrash` is a craftable Legendary Lightning spell with five levels. Choose Lightning and Thundercrash in Iron's Scroll Forge. With the verified pack defaults, one Legendary Ink, one Paper and one Lightning Bottle produce Thundercrash I; the native server forge test verified the output and ingredient consumption. Inscribe the scroll into a spellbook normally; the native Inscription Table menu test verified that path too. Existing Iron's per-spell overrides can still disable crafting or change the school.

Hold the normal cast control for the brief charge, then aim with the camera during flight. The activation commits to a four-tick upward launch followed by up to 80 guided ticks at 0.85 blocks/tick. Colliding with terrain or an eligible living target discharges within six blocks. Damage starts at 40 and increases by 10 per spell level before Iron's spell-power calculation; it falls to 25% at the edge. The default cost is 150 mana and the cooldown is 120 seconds, with normal Iron's handling. Thundercrash leaves terrain intact. Timeout, external teleports, death, logout, fluids, and unsafe boundaries end propulsion without a discharge. Airborne completion protects the first landing for up to 100 server ticks. Other spell casting is blocked during committed flight.

Server tuning is in the `[thundercrash]` section of `world/serverconfig/irons_ultimate_explosion-server.toml`; client particle and sound tuning is in `config/irons_ultimate_explosion-client.toml`. `baseSpellPower` and `spellPowerPerLevel` adjust progression before Iron's attributes and power multiplier; `edgeDamageFraction` adjusts radial falloff. `guidedFlight=false` locks the activation direction and ends at the default 40-block distance cap. `pvpDamage` also respects the server PvP setting and Iron's friendly-fire rules. Grand Explosion's existing config keys and content IDs are preserved.

Movement is server-owned, with bounded client prediction and scoped mixins for travel, look-input transmission, incoming vanilla position packets, floating checks, and teleport handoff. State is sent every two server ticks plus phase changes and tracking snapshots. The networking protocol is now **3**: replace the previous addon JAR on the server and every client, and remove the old copy. Install only the release JAR; `grand-explosion-test.jar` is a disposable-world harness.

The aura directly calls Ascension's public electricity helper. Distance-sampled electrical trails, short Iron's zap arcs, a forward cluster, and a cyan blastwave build on its particles without bundling upstream textures. Particle budgets prioritize nearby casters and impact effects. The original playerAnimator posture does not enable a spin attack or change the camera mode.

Audio currently uses registered Thundercrash events that reference Iron's existing Shockwave preparation, lightning woosh, and Shockwave cast sounds. Custom recordings have not been supplied. To replace them, place mono Ogg **Vorbis** files under `assets/irons_ultimate_explosion/sounds/thundercrash/` as `thundercrash_cast.ogg`, `thundercrash_flight.ogg`, and `thundercrash_impact.ogg`. Replace each `sounds.json` entry with a file reference such as `{"name":"irons_ultimate_explosion:thundercrash/thundercrash_flight","stream":false}`. Keep the flight file seamless; playback code supplies looping. A resource pack can override these assets. The flight sound follows the caster and is explicitly stopped on termination, tracking loss, unload, disconnect, reload, or a stale snapshot.

See [THUNDERCRASH-TESTING.md](THUNDERCRASH-TESTING.md) for the gameplay checklist and automated validation limits.

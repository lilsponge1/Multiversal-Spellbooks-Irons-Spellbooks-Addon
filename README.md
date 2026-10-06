# Multiversal Spellbooks addon

Forge Minecraft 1.20.1 addon for Iron's Spells 'n Spellbooks. This update combines Grand Explosion **0.3.3**, Crimson Susanoo **0.1.1**, Ignis Armor Compatibility **0.1.0**, and Omega Rush / Omega Form **0.2.2** into one JAR while retaining separate mod IDs, packages, resources and configs.

## Latest changes

- Omega Form: a rare Flowery Scarf in the charm slot unlocks sustained rainbow transformation, hover flight, eight absorption hearts, Resistance III, Strength I, and +25% Nature power. Boss drops are 5%; generated chest loot is one in 500. See [Omega Form](OMEGA-FORM.md).
- Omega Form flight now toggles with a quick double-tap of jump; single jumps remain normal. Form and Rush have dedicated flower and rainbow explosion HUD icons.
- Omega Rush now requires active Omega Form by default and has a 10-second base cooldown during Form. Form flight uses Rush's forward-arm posture while retaining the double-tap hover controls.
- Omega Rush: guided rainbow flight, a broad horizontal explosion trail, forward-arm flying posture, charge-only car-drive audio, and about six bomb sounds per second. Craft its Legendary Nature scroll with Legendary Ink, Paper, and a Poisonous Potato.
- Thundercrash: new lightning-gauntlet icon and charge/flight/impact audio synchronized to actual flight and collision.
- Cuirass of the Thunderstar: refined ivory/gold/blue armor, one equipped spell slot, normal Travel Optics Riptide material attributes and a unique 20% Thundercrash impact bonus.
- Crimson Susanoo: remote walking synchronization, smooth pose interpolation, corrected right-hand katana/grip, half outgoing direct damage by default, co-op damage/aggro protection and a second-cast recall with no extra mana.
- Ignis Mage armor: normal enchanting-table and anvil support, including survival XP/lapis costs and ordinary compatibility rules.

See [Omega Form and its review checkpoint](OMEGA-FORM.md), [earlier release notes](RELEASE-0.3.3.md), [validation](INTEGRATION.md), [Thunderstar](THUNDERSTAR-CUIRASS.md), [Susanoo](modules/crimson-susanoo/README.md) and [Ignis compatibility](modules/ignis-armor-compat/README.md).

## Install

Download [Omega Form 0.2.2 — single combined JAR](https://github.com/lilsponge1/Multiversal-Spellbooks-Irons-Spellbooks-Addon/releases/tag/v0.3.3-omega-form-0.2.2). See [release notes](RELEASE-OMEGA-FORM.md) for the Flowery Scarf, double-tap flight, Form flying pose, and 10-second transformed Rush cooldown.

`multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.2.jar`

Release SHA-256: `08ACF74C03472305CCD5E7292F19769FD6E6BB6907925F4821F619DEFE225805`

Stop Minecraft and the server, back up the world/configs, and replace the older addon JAR with this **same file on the server and every client**. Remove separate Grand Explosion, Crimson Susanoo, Ignis compatibility, and Omega Rush JARs if present; their contents are already included. Keep normal modpack dependencies beside it. This build requires Travel Optics 6.3.0+ and Cataclysm: Spellbooks 1.2.9 (below 1.3), in addition to the existing Iron's/Cataclysm/GeckoLib dependencies. Testing used Forge 47.4.10 and Iron's Spells 3.16.3. Omit diagnostic JVM flags in normal play.

Omega Rush keeps the spell ID `irons_omega_rush:omega_rush` and its existing configuration files. For worlds upgrading from the first standalone build, set `damageRadius = 4.05` in the world's `serverconfig/irons_omega_rush-server.toml`. See [Omega Rush](modules/omega-rush/README.md) and [integration validation](OMEGA-INTEGRATION.md).

Existing item/spell IDs and config files remain valid. The new Susanoo `damageMultiplier=0.5` applies to saved older damage bases; keep `friendlyFire=false` for co-op protection. The server owners handle production installation and the final two-player visual check. See [installation checklist](docs/INSTALL-COMBINED.txt).

## Build and collaborate

Root `src/` contains Grand Explosion, Thundercrash, Thunderstar and Cinderstar. Independent modules live under `modules/crimson-susanoo/`, `modules/ignis-armor-compat/`, and `modules/omega-rush/`. See [CONTRIBUTING.md](CONTRIBUTING.md).

Use Java 17. Build root `src/` with `build.ps1`; its defaults reference an existing development workspace, so supply `-JdkBin`, `-LibraryDir`, `-ModDir`, `-MinecraftJar`, `-MinecraftServerJar` and `-SrgMinecraftJar` for another machine. The script needs the matching Forge runtime and SRG Minecraft JARs plus the pack libraries. Root output defaults to `build/grand-explosion-0.3.3.jar`. `build-test.ps1` creates a separate disposable-world harness, excluded from production.

Build each module with its own Gradle wrapper (see its README), then assemble:

```powershell
./build-combined.ps1 -GrandExplosionJar ./build/grand-explosion-0.3.3.jar
```

Build Omega Rush separately with `modules/omega-rush/build.ps1` before assembly, or pass `-BuildOmega`. To preserve all existing bytes in an accepted combined release and add only Omega Rush:

```powershell
./build-combined.ps1 -CombinedBaseJar ./path/to/accepted-multiversal.jar -BuildOmega
```

Python 3.11+ is required for the packager; pass `-Python <python.exe>` if needed. `-BuildCrimson` and `-BuildIgnisArmor` build the modules first; `-Offline` requires cached Gradle dependencies. `-CrimsonJar`, `-IgnisArmorJar`, `-OmegaJar`, and `-OutputJar` override inputs/output. In the module-input mode, passing `-IgnisArmorJar ''` or `-OmegaJar ''` omits that optional module. Default output is `build/release/multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.2.jar`, with checksum and input/payload report. Existing output files are never overwritten. Only shared metadata is merged; third-party dependencies are never bundled.

The published review artifact retains its tested filename and bytes. A fresh build can differ in archive metadata; validate and test a rebuilt release before deploying it.

## Grand Explosion and Cinderstar

Grand Explosion's spell ID is `irons_ultimate_explosion:grand_explosion`. The craftable Cinderstar Staff comes imbued with Grand Explosion V. Cinderstar Regalia contains `cinderstar_hat`, `cinderstar_robe`, `cinderstar_leggings` and `cinderstar_boots`, each upgraded from matching Pyromancer armor using Cinder Essence and a Nether Star. Grand Explosion's damage radius is 60 blocks, or 80 with all four armor pieces; its full-set center bonus applies before the 320 cap. The crater radius is unchanged by the set.

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

## Thundercrash

`irons_ultimate_explosion:thundercrash` is a craftable Legendary Lightning spell with five levels. Place a Lightning Bottle in Iron's Scroll Forge focus slot to show Lightning spells, then select Thundercrash. With the verified pack defaults, one Legendary Ink, one Paper and one Lightning Bottle produce Thundercrash I; the native server forge test verified the output and ingredient consumption. Inscribe the scroll into a spellbook normally; the native Inscription Table menu test verified that path too. Existing Iron's per-spell overrides can still disable crafting or change the school. Grand Explosion remains unavailable for Scroll Forge crafting.

Thundercrash now has the item ID `irons_ultimate_explosion:thundercrash_scroll`, which appears in `/give` autocomplete and Iron's scroll creative tab. `/give unclecrenando irons_ultimate_explosion:thundercrash_scroll` gives a ready-to-inscribe Thundercrash I scroll. The Scroll Forge produces this same named item and preserves the native spell level. Existing generic Iron's Thundercrash scrolls and `/createScroll` still work.

Hold the normal cast control for the brief charge, then aim with the camera during flight. The activation commits to a four-tick upward launch followed by up to 80 guided ticks at 0.85 blocks/tick. Colliding with terrain or an eligible living target discharges within six blocks. Damage starts at 40 and increases by 10 per spell level before Iron's spell-power calculation; it falls to 25% at the edge. The default cost is 150 mana and the cooldown is 120 seconds, with normal Iron's handling. Thundercrash leaves terrain intact. Timeout, external teleports, death, logout, fluids, and unsafe boundaries end propulsion without a discharge. Airborne completion protects the first landing for up to 100 server ticks. Other spell casting is blocked during committed flight.

Server tuning is in the `[thundercrash]` section of `world/serverconfig/irons_ultimate_explosion-server.toml`; client particle and sound tuning is in `config/irons_ultimate_explosion-client.toml`. `baseSpellPower` and `spellPowerPerLevel` adjust progression before Iron's attributes and power multiplier; `edgeDamageFraction` adjusts radial falloff. `guidedFlight=false` locks the activation direction and ends at the default 40-block distance cap. `pvpDamage` also respects the server PvP setting and Iron's friendly-fire rules. Grand Explosion's existing config keys and content IDs are preserved.

Movement is server-owned, with bounded client prediction and scoped mixins for travel, look-input transmission, incoming vanilla position packets, floating checks, and teleport handoff. State is sent every two server ticks plus phase changes and tracking snapshots. The networking protocol is now **3**: replace the previous addon JAR on the server and every client, and remove the old copy. Install only the release JAR; `grand-explosion-test.jar` is a disposable-world harness.

The aura directly calls Ascension's public electricity helper. Distance-sampled electrical trails, short Iron's zap arcs, a forward cluster, and a cyan blastwave build on its particles without bundling upstream textures. Particle budgets prioritize nearby casters and impact effects. The original playerAnimator posture does not enable a spin attack or change the camera mode.

Thundercrash uses recorded casting, looping flight and impact audio. The charge stops on authoritative launch; flight follows the caster until the actual server collision; impact stops charge/flight before playing the crash. Timeout, cancellation, tracking loss, unload, disconnect and stale snapshots stop playback. The mono 48 kHz Vorbis clips last approximately 0.80 / 3.79 / 4.39 seconds; the flight clip loops instead of scheduling impact at a fixed time. See [THUNDERCRASH-POLISH.md](THUNDERCRASH-POLISH.md).

See [THUNDERCRASH-TESTING.md](THUNDERCRASH-TESTING.md) for the gameplay checklist and automated validation limits.

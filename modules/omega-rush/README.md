# Omega Rush and Omega Form

Omega Rush module for the single-JAR Multiversal Spellbooks add-on, using Minecraft 1.20.1, Forge 47.4.10, and Iron's Spells 'n Spellbooks 3.16.3.

Omega Rush gathers rainbow energy during a short charge, launches the player into camera-guided flight, and leaves a broad cone of rainbow explosions along the curved path. Flight uses a horizontal posture with arms extended forward and legs trailing behind. All three supplied explosion animations play left to right and appear intermixed.

The visual cone is wider than it is tall: its lateral spread extends another three blocks to each side, while its height stays at the previously approved size. The added width stays horizontal when aiming up or down. The default damage radius remains 4.05 blocks. Flight and burst counts remain the same.

The supplied car-drive sound plays only during charging, follows the caster, and is audible to nearby players. It stops on launch or cancellation. The supplied bomb sound plays at the trail's explosion positions about six times per second per caster at normal TPS and default flight settings, 50% more often than before. Playback speed and pitch are unchanged. Fewer trail nodes or several simultaneous casters can reduce the audible rate. Both files are mono Ogg Vorbis, with distance fading over 32 blocks. The client `soundVolume` setting and Minecraft's Players volume control still apply.

See [Omega Form and the Flowery Scarf](../../OMEGA-FORM.md) for the new equipment, transformation, hover controls, drop rates, and Rush prerequisite.

## Install

Install the combined Multiversal Spellbooks JAR on the server and every matching client as described in the [repository installation guide](../../README.md). Remove the previous combined JAR and the standalone Omega Rush JAR: both are now included in the replacement file. Omega Rush retains its existing mod ID and configuration names inside that one file.

Iron's existing required dependencies must already be installed. Do not install `omega-rush-test.jar`; it is a disposable-world development harness. The live pack has not been changed automatically.

## Craft and cast

1. Put **Legendary Ink, Paper, and a Poisonous Potato** into Iron's Scroll Forge. The potato selects the Nature school. Select Omega Rush.
2. Inscribe the resulting scroll into a normal spellbook with the Inscription Table.
3. Hold the usual cast control through the brief charge. After launch, aim with the camera to steer. Releasing the cast control does not stop flight. Sneak to end it deliberately.

For an operator preview, use `/createScroll irons_omega_rush:omega_rush 1`, then inscribe that scroll normally. The spell ID is `irons_omega_rush:omega_rush`.

## Defaults

| Setting | Value |
|---|---|
| School, rarity, levels | Nature, Legendary, I–V |
| Mana, cooldown | 200 mana, 120 seconds |
| Charge | 16 ticks / 0.8 seconds before casting-speed modifiers |
| Flight | 140 ticks / 7 seconds at normal TPS |
| Speed | 0.8 blocks per tick / 16 blocks per second |
| Steering response | 0.18, matching Thundercrash |
| Trail spacing and delay | 2 blocks, then 5 ticks before detonation |
| Damage radius | 4.05 blocks per trail node |
| Base damage progression | 12 damage points at I, plus 3 per additional level |
| Repeat hits | At most three successful hits per target per cast, at least 10 ticks apart |

Iron's Nature spell-power and target-resistance rules apply. The caster is excluded, and native protection/friendly-fire/PvP rules are respected. Terrain remains intact. The wider cone is the cosmetic presentation; the damage radius remains the configured radius around each traveled-path node.

When upgrading an existing world, set `damageRadius = 4.05` in `world/serverconfig/irons_omega_rush-server.toml` (using your world's folder name), then restart the server. Forge preserves existing configuration values, so replacing the JAR alone does not override a saved radius of 3.0.

Terrain contact ends propulsion without an impact blast. Timeout, deliberate cancellation, or ordinary terrain contact allow already placed nodes to finish. Death, disconnect, dimension changes, teleports, fluids, and unsafe boundaries clear the outstanding spell state. The first landing is protected for up to five seconds after an ordinary flight ending.

## Configuration

Server settings are in `world/serverconfig/irons_omega_rush-server.toml`, under `[omegaRush]`. `flightTicks` accepts 100–200 ticks (5–10 seconds at normal TPS). Other keys tune mana, cooldown, charge length, speed, steering, spacing, damage radius, base spell power, level progression, and PvP damage.

Client settings are in `config/irons_omega_rush-client.toml`, under `[presentation]`:

- `particleQuality`: `FULL`, `REDUCED`, or `MINIMAL`. All modes retain the wider cone; full quality includes more bursts and sparks.
- `rainbowPlayerOverlay`: enables the translucent color-cycling player tint.
- `brightFlashes`: enables the short white-core accents.
- `soundVolume`: scales charge, launch, flight, and trail sounds.

The spell does not force a camera mode or enable a spin attack. Flight posture is a rendering animation; normal movement and the collision box are retained.

## Build and verification

Run `build.ps1` from PowerShell. Its parameters point to the existing local Java 17, Forge, Minecraft, and Iron's development runtimes; adjust them for another computer. Extracted assets are already included in the source resources. `tools/extract-assets.py` documents their derivation from the supplied sprite sheet.

The module output is `build/omega-rush-0.2.1.jar`. Assemble it with the root `build-combined.ps1`; users install the combined output. Build parameters also accept matching Minecraft client/server and SRG JAR paths for another development environment.

`build-test.ps1` builds the separate test harness. Run `omega_test` only in a disposable world: the harness creates synthetic players, targets, and test blocks. `omega_latency` and `omega_actor` are development-only commands for local latency and graphics checks.

See `TESTING.md` for the final verification record, scope, and evidence. A SHA-256 checksum accompanies the release JAR.

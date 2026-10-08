# Saiyan Ascension

Minecraft 1.20.1 / Forge 47.4.10 / Iron's Spells 3.16.3 add-on feature. Iron's dependency JAR is unchanged. One registered spell, `irons_ultimate_explosion:saiyan_ascension`, owns Base, Super Saiyan, Super Saiyan II and Super Saiyan III internally. The current combined candidate is 0.3.5. See [SAIYAN-SSJ3.md](SAIYAN-SSJ3.md) for the new sequence and validation limits.

## Obtain and use

`/give @s irons_ultimate_explosion:saiyan_wig`

Craft the Saiyan Wig with five Black Wool, a Lightning Bottle, Nether Star, Golden Apple and Blaze Powder. Equip it in **Curios: Head**, then select Saiyan Ascension in Iron's spell wheel and use the selected-spell cast key or matching quick-cast key. The wig contains one locked spell and no additional slots; it leaves the armor head slot and normal spellbook accessory free. Holding it alone grants no transformation. The former Book of Saiyan Ascension remains registered for existing stacks, but its crafting recipe and creative entry are replaced by the wig. Scroll Forge crafting is disabled by default for this spell.

The spell has a dark player silhouette inside a yellow aura, with two blue-white lightning accents. The transparent 32x32 icon is prepared from the built-in imagegen master in `art/saiyan`; the exact generation prompt and an enlarged pixel preview are saved beside it. Iron's native icon resolver requires `textures/gui/spell_icons/saiyan_ascension.png`. The earlier `gui/spells` path caused the missing-texture checkerboard and is corrected in the `saiyan-visuals` review JAR. Its native spell guide is concise, with separate tooltip lines for activation costs, drain and controls.

Use Iron's selected-spell cast key or matching quick-cast key while the wig is equipped. Existing legacy books also retain their previous held-book activation:

- From Base, activation starts a 2.5-second charge and pays 50 mana.
- In Super Saiyan, release before one second to start the three-second ascension and pay 75 additional mana.
- In Super Saiyan II, tap again to begin the 15-second SSJ3 charge. SSJ2 remains active until completion.
- In any transformed form, or during the SSJ3 charge, hold continuously for three seconds to return to Base for free.
- Releasing between one and three seconds cancels that input. A short activation in Super Saiyan III does nothing.
- Each hold is measured by the server. Heartbeats confirm continued input; missing heartbeats or closing a screen cancels the hold. A charge already committed continues after release.

## Balance

| Modifier | Super Saiyan | Super Saiyan II | Super Saiyan III |
| --- | ---: | ---: | ---: |
| All spell power | +20% | +35% | +100% |
| Melee damage | +25% | +40% | +100% |
| Movement speed | +15% | +25% | +40% |
| Attack speed | +15% | +25% | +50% |
| Damage resistance | 15% | 25% | 35% |
| Knockback resistance | +0.50 | +0.80 | +1.00, capped at 1 |
| Mana drain each second | 10 | 17 | 30 |
| Remaining mana regeneration | 50% | 25% | 25%, with an additional native-regeneration cap |
| Maximum health | unchanged | unchanged | +40% |
| Critical damage | unchanged | unchanged | +50% where supported |
| Jump strength | unchanged | unchanged | +35% |

Each form replaces the previous modifier set. Fixed UUIDs and transient attribute modifiers prevent stacking and persistence. Spell power, melee, movement, attack speed and mana regeneration multiply the existing attribute totals. Knockback resistance adds percentage points because vanilla players start at zero. Damage resistance multiplies incoming damage once at the Forge hurt event, respecting damage types that bypass resistance. No vanilla potion effects are added.

SSJ1 and SSJ2 drain is charged every ten server ticks: five mana in SSJ1 or 8.5 in SSJ2. Regeneration continues through Iron's native system with its own attribute penalty. SSJ1 remains active, including its drain and buffs, during SSJ2 charging. When 8.5 mana cannot be paid, the controller attempts SSJ1 and charges five mana instead; if that also cannot be paid, it returns to Base. Mana cannot become negative through this controller. With normal regeneration of seven mana per second, the net default losses are about 6.5 and 15.25 mana per second. Exceptionally high regeneration can still cover the configured drain; tune the exposed values for such builds.

SSJ3 charges 1.5 mana every server tick after completion. Zero or insufficient mana returns directly to Base. Its native-regeneration cap preserves at least half the configured drain as a net loss under the installed Iron's regeneration schedule. Instant mana restoration remains usable.

## Visuals and sounds

Curved golden flame sheets surround the body with a larger outline in SSJ2. The camera-facing sheets are much fainter so the player's skin and clothing remain visible. SSJ2 adds small native Iron's electricity particles and intermittent, thin branching blue-white arcs. The charge uses a lowered, wide-legged playerAnimator posture with raised fists and an 85% movement penalty. The charging FOV is held steady to prevent the movement penalty from zooming the camera into the player. Ground rings build during charging; the finish has a brief expanding gold shell and shockwave rings, with a larger, longer finish in SSJ2. Power-down calms the aura and collapses it over twelve client ticks.

The aura uses Minecraft's native particle vertex format and shader, full-bright lightmap coordinates, translucent blending and depth testing with color-only writes. It draws after translucent terrain, after Solas has reconstructed the sky. Earlier draws either disappeared against sky or wrote pale panel silhouettes into its depth buffer. Solas V3.7b squares particle transparency in `shaders/programs/gbuffers_textured.glsl`; gold opacity is tuned for that path. Nominal aura height is 3.10 blocks in SSJ1 and 3.55 in SSJ2, with individually pulsing heights, outward flaring and traveling curls. Hair follows the supplied wig reference with sixteen curved locks, a tall asymmetric peak, layered side and rear spikes, and separate swept bangs. The complete mesh stays outside the outer skin layer; armor equipment adds top and side clearance for standard helmets. It follows the animated head and becomes gold in SSJ1, with a taller crown in SSJ2. It does not replace helmet equipment.

SSJ1 charges with Iron's black-hole energy; SSJ2 uses its electrocute loop, then an explosion and lightning crack. The former optional Cataclysm short grunt has been replaced by full-length voice samples from the user's supplied transformation video, sustained excerpt 8.85-10.65 seconds. SSJ1 uses a 2.5-second version; SSJ2 uses a three-second version at unchanged playback pitch. The source is normalized before local UVR vocal separation, then lightly denoised; the resulting 1.8-second voice master is stretched to each duration without changing pitch. The files are mono Vorbis at 44.1 kHz with short fades and normalized levels. `tools/isolate_saiyan_voice.py` rebuilds the voice master from the supplied video, and `tools/generate_saiyan_audio.py` encodes that master, and `art/saiyan/audio-source.json` records provenance and decoded duration checks. All charging audio follows the caster and stops at the authoritative phase boundary, reset, unload or logout. Sound files remain replaceable through a resource pack.

Camera shake is limited to charging and the final burst, fades with distance and can be disabled. Persistent SSJ2 does not shake the camera. The caster's first-person aura geometry is hidden to preserve visibility; other players can see it. Emissive rendering does not provide dynamic world illumination or shader distortion.

## Configuration and safety

Server values live under `[saiyanAscension]`, `[saiyanAscension.ssj1]`, `[saiyanAscension.ssj2]` and `[saiyanAscension.ssj3]` in `world/serverconfig/irons_ultimate_explosion-server.toml`. These expose activation costs, durations, drain, regeneration multipliers, each combat modifier, hold/short-release thresholds, shake radius and sound volume. Client values in `config/irons_ultimate_explosion-client.toml` expose particle density, a shared per-tick budget (128 by default), head spikes, camera shake and the form indicator. Reloading particle quality also respects the add-on's existing quality setting. Restart after changing combat modifiers on a running server so active sessions receive the new values.

State is server-owned and synchronized to the caster and entity-tracking players on changes, every half-second and when tracking begins. Cosmetics have a 64-block distance limit; details thin out beyond 32 blocks. Lightning is generated intermittently and particles share a global budget, with nearer players prioritized. State snapshots expire if the server stops sending them. Tracking loss, world unload, logout and resource reload remove client sounds/animation layers.

Death, clone, respawn, logout, server shutdown, dimension change, lost book/spell access, burnout and forced reset remove modifiers. Sessions are never saved. The public `SaiyanManager.reset(player, SaiyanStatePacket.RESET)` entry point supports administrative integrations. A canceled Iron's pre-cast event or mana-change event prevents a free transformation.

Networking protocol is **5**. The server and every client need the same combined candidate. The candidate is a local review build; it is not installed in the live pack or published. Use a disposable world for first testing. Existing Susanoo and Ignis module payloads are retained by the combined-JAR packager.

## Current SSJ3 verification

The revised candidate is `build/release/multiversal-spellbooks-0.3.5-saiyan-ssj3-hair4.jar`. The mane roots remain attached to the crown and follow head turns, including looking straight up/down, while the lower locks bend along the back. The 15-second charge, Goku voice and ten-second completion music are unchanged. [SAIYAN-SSJ3.md](SAIYAN-SSJ3.md) and `docs/SSJ3-VALIDATION.json` record 40 hidden native Solas checks with no failures, thirteen reviewed captures and the full-angle attachment checks. User acceptance remains pending. Earlier artifacts are preserved.

## Historical SSJ1/SSJ2 verification

The previous accepted SSJ1/SSJ2 artifact is `build/release/multiversal-spellbooks-0.3.4-saiyan-visuals-wig12.jar`, SHA-256 `291CD5984ABA57152F29CD252D4A7990EA4B6B9B962137835A8B99D00327DA72`. Production and separate test sources compile against the installed pack. The package verifier confirms the native icon, Curios Head wig, crafting recipe, charge animation, sound references and bundled yell files, and excludes test helpers and dependency classes. Eleven generated visual/JSON assets match the source. Other combined-mod payloads are preserved by the packager.

The full-pack server harness reported **155 passed, 0 failed** for wig1. It covers Head-only access, locked spell, held-wig rejection, empty-hand casting, helmet preservation, exact 60-tick SSJ2 charge, mana, buffs, hold timing and cleanup. The tested server controller and wig classes are byte-identical in the current artifact. That regression run saved all dimensions, but non-daemon Ballistix blast workers required timeout termination after the server thread exited.

The previous native gameplay review reported **39 passed, 0 failed**. The mana tuning changes only the configuration class and is now under live manual combat review with Prism/Solas; attribute totals and actual payments are recorded in `.tools/saiyan-prism-15`, with incoming damage coverage tracked separately. It covers resources, input, charge timing, both forms, sound instance lifetime and power-down. Native captures show the aura above the hair, a visible body, consistent gold across the horizon and changing flare shapes in six successive SSJ2 frames. The user accepted the aura; the revised SSJ1/SSJ2 hair and subsequent combat build were accepted by the user. SSJ3 has a separate pending native review.

The wig10 hair fix changes only `SaiyanWigRenderer.class`; the other 170 payload entries are byte-identical to wig9. `tools/check_saiyan_hair_geometry.py --jdk-bin <path>` verifies 132 actual cross sections across all three forms against the outer skin and standard helmet bounds. Fresh native Solas captures expose the unarmored face in Base, SSJ1 and SSJ2, include a tilted-head forehead view and a diamond-helmet view, and show no hair crossing the face.

The aura optimization evaluates shared rows once and emits vertex positions directly, preserving the native-reviewed wig8 shape, UVs, colors, transparency and order. `tools/check_saiyan_aura_geometry.py --jdk-bin <path>` compares extracted Java geometry against the frozen wig8 fixture: 90 cases and 116,640 vertices matched bit for bit. Trigonometric and power calls in the sampled flame geometry fell by 96.53%, and temporary per-vertex position objects were removed. The two aura classes compile, the package verifier passes, and the other 169 payload entries remain byte-identical to wig8. Wig9 also passed the current 34-check native Solas run. Its one-caster SSJ2 CPU geometry and draw submission averaged 0.1574 ms (p95 0.2682 ms), versus 0.4346 ms (p95 0.6994 ms) in the preceding wig8 run. These are draw-call CPU measurements, not FPS or GPU measurements.

Previous native reviews are preserved: wig3 passed 31 checks but its appearance and short grunt were rejected; wig5 passed 34 checks but its aura stopped at the horizon; wig7 passed 34 checks at 1280x720 but retained a pale sky band. Native memory failures and an unrelated Eureka registry initialization race are recorded in `docs/SAIYAN-VALIDATION.json`.

The cached disposable Prism instance is `Saiyan Ascension Solas Review 2`, using a separate localhost server at `127.0.0.1:25579` under `.tools/saiyan-prism-10/server`. Solas V3.7b and Oculus 1.8.0 remain enabled. The test helper requests 1280x720 and disables the saved fullscreen option through Minecraft's native API; shadow distance is capped at two chunks. Only the disposable server skips the Penumbra intro and initializes mods with one FML loading thread. Test helpers are excluded from the release. The runner and its ownership-checked exit watcher stop the owned server when the review client closes. The live pack is unchanged.

Audio isolation uses the local [UVR vocal model](https://github.com/TRvlvr/model_repo/releases/tag/all_public_uvr_models) and [official model settings](https://github.com/Anjok07/ultimatevocalremovergui). Neither model nor ONNX runtime is packaged in the mod, and source audio is not uploaded. `tools/isolate_saiyan_voice.py` rebuilds the master; `tools/generate_saiyan_audio.py --ffmpeg <path>` encodes both sounds. Decoded durations are exactly 2.5 and 3 seconds, with no silent interior 100-ms windows. The user accepted the raw yell; listener acceptance of the cleaned version remains pending.

CPU measurements cover only aura geometry and render submission for one caster. GPU time, whole-game FPS and simultaneous casters have not been measured. A two-player visual review remains pending.

The earlier wig8 client exhausted native memory during a late live-heap cleanup attempt, after its completed review. Those processes were stopped. The user then authorized wig9 testing in the same disposable server world; native captures and logs for the new run are preserved under `.tools/saiyan-prism-11`. The current client and server remain open for manual testing, with an ownership-checked exit watcher to stop the server when its client closes. No live-heap cleanup was attempted in this run. Future desktop/server launches require user permission. The sampled lossless motion preview under `art/saiyan/review/ssj2_motion.png` is from the preserved wig8 capture; current wig9 captures are in the new fixture.

# Omega Rush verification

## Omega Form 0.2.2 candidate

The exact combined artifact passed **135 Form assertions plus 114 legacy Rush/compatibility assertions**, for **249 server checks** in the disposable copied pack. SHA-256: `08ACF74C03472305CCD5E7292F19769FD6E6BB6907925F4821F619DEFE225805`. The archive audit verified 240 preserved payload entries. Packager code is unchanged and its seven unit tests passed in the preceding revision.

The new native cast check verifies 200 mana charged once and a 200-tick Rush cooldown during Form. Other cases cover normal outside-Form cooldown, equipment reduction, unchanged Form cooldown, configurable transformed cooldown, zero native cooldown, and no cooldown rewrite when Form ends. Native pose-resource decoding and the full existing movement, equipment, buffs, loot and compatibility cases pass.

The first added zero-cooldown assertion assumed Iron's removed its entry immediately. Its native code stores a zero-duration entry until its next cooldown tick; the fixture was corrected to inspect the duration. Production code was unchanged by that correction. Final log: `build/omega-form-flight-pose-validation.log`.

The user accepted 0.2.1 in game. Form now uses the unchanged Rush pose resource during enabled hover and shares its layer with Rush. The new hover presentation still needs visual review; background checks do not verify how it looks on a real client. No desktop control was used. GitHub publication remains pending. See [the review checkpoint](../../OMEGA-FORM.md).

The remaining sections record earlier released Omega Rush development history.

Validation date: October 5, 2026. All worlds and graphics profiles used here are disposable workspace copies. The live pack, Thundercrash source, Multiversal source/distribution, and GitHub were not modified.

Subsequent repository integration packages this completed module in the single Multiversal Spellbooks JAR. The combined release passed 114 copied-pack server assertions. The exact-health fixture now suppresses Apothic Attributes critical chance only on its synthetic caster and verifies the recorded hit count, preserving production critical hits. See [combined integration validation](../../OMEGA-INTEGRATION.md). The standalone development history below describes earlier work before the authorized GitHub integration.

Latest sound refinement: car-drive playback is restricted to the charge phase and stops immediately when a launch state arrives. Tick handling also prevents restarting it during flight. The bomb cadence now uses fractional deadlines targeting six sounds per second per caster, keeping pitch and volume unchanged and retaining the two-sounds-per-tick global cap. Deadlines do not replay a burst of missed sounds after a pause. This client sound adjustment was rebuilt and audited; the server suite was not rerun because server behavior and resources are unchanged. The user reviewed the preceding sound implementation in game and accepted the sounds and volume, requesting these two timing changes. The latest timing changes have not been reviewed in game.

The preceding sound revision converted the supplied WAVs to mono 44.1 kHz Ogg Vorbis, preserving duration and source files. Both converted files were decoded successfully and checked for finite, nonzero samples. Both sound definitions specify 32-block attenuation. The minimal dedicated-server run passed **109 assertions**, including actual registration of both custom sound events. The final packaged-resource audit checks the embedded sound definitions and mono Vorbis files.

Latest visual revision: the cone's lateral spread extends another three blocks to each side, with its vertical spread unchanged. Its lateral basis stays horizontal during climbs and dives, with a finite fallback for vertical flight. The release was rebuilt and its packaged resources and reference preservation rechecked. Live graphics were not rerun for this cosmetic change; the preview image shows the earlier, narrower cone.

The preceding damage revision increased the default damage radius by 35%, to 4.05 blocks around each path node. The minimal Forge/Iron's server passed **108 assertions**, including a target beyond the old radius taking damage, a target beyond 4.05 blocks remaining unharmed, and a diagonal bounding-box corner staying outside the spherical damage area. Existing saved configuration values are preserved; upgrading worlds must set `damageRadius = 4.05` to enable the increased damage area.

## Automated acceptance

The preceding release passed **106 assertions** on the minimal Forge 47.4.10 / Iron's 3.16.3 server and **111 assertions** in the copied Multiversal/Thundercrash pack. The five additional copied-pack checks exercise the installed Thundercrash implementation and mutual casting exclusion. The updated minimal-server run passed 108 assertions as described above.

Coverage includes:

- Nature school, Legendary rarity, five levels, configured mana/cooldown/duration/speed, and Nature-power capture.
- Exact steering arithmetic compared with the installed Thundercrash binary, yaw wrapping, upward/downward turns, bounded prediction, acknowledgement and reset behavior.
- Arc-length trail sampling through corners and climbs; wider cone geometry, vertical-flight orientation, and finite coordinates.
- Full-body collision sweeps, walls, floor, ceiling, fluids, unloaded chunks, and unsafe build height.
- Five-tick detonation delay, spherical damage radius, caster exclusion, native protection events, PvP-off behavior, ten-tick repeat-hit spacing, and the three-hit maximum.
- Interrupted charge, timeout, deliberate cancellation, gravity restoration/recovery, one-landing protection and expiry, teleport, logout, death, and dimension event cleanup.
- Eight simultaneous server sessions; strict input payload decoding and rejection of malformed input.
- Native playerAnimator decoding and enabled renderer channels for both arms and both legs.
- Actual Scroll Forge ingredient slots and consumption, forged spell/level, and native Inscription Table handling.

The damage fixture uses a cow rather than a mule, because horse-family targets can regenerate health and make exact health assertions nondeterministic. No production damage logic was weakened to accommodate that fixture.

Logs: `build/validation-final-minimal-106.log` and `build/validation-final-compat-111.log`. The final packaged-resource audit is `build/release-verification.json`; it verifies compiled class bytes, mod identity/dependencies, Mixin metadata, exclusion of test code, all three transparent animation sequences, pose resources, and unchanged reference source/distribution hashes.

## Live networking and graphics

- A real Survival-mode client, with server `allow-flight=false`, completed a 140-tick flight at 0.8 blocks/tick under **125 ms of artificial delay in each direction (250 ms round trip)**. The server recorded approximately 112 blocks traveled in 7.002 seconds. The client recorded stopped flight audio, no remaining flight state, and restored gravity.
- Real caster and observer clients received the same flight session and cleared it after completion. Later observer tests confirmed the same session reappears when leaving and returning to tracking range.
- First-person forward visibility, the rainbow charge, contracting ring, launch presentation, and ordinary post-flight movement were inspected in game.
- The actual client sneak input ended a Survival flight after eight ticks (6.4 blocks), restoring gravity and stopping its loop. A resource reload during another flight completed without stranded flight state or audio.
- Third-person pose and the broader cone were reviewed with Solas Shader V3.7b and shader rendering disabled. The final renderer uses the actual `rightArm`, `leftArm`, `rightLeg`, and `leftLeg` animation channels. A close-up caught and corrected a feet-first orientation before delivery.
- The supplied sheet's A/B/C rows are separate left-to-right animations (8/5/7 frames). All three appear intermixed in the visual cone. They do not generate vanilla block explosions.

The graphics harness includes a stationary render-only player fixture to inspect posture and effects without losing the subject during a seven-second dash. Its zero speed and extended lifetime exist only in the test harness; they are not release settings. Timed flight acceptance uses ordinary moving sessions separately.

Some secondary test-client attempts crashed in native GLFW event processing. The final review used one disposable graphics client and a server-provided remote-player fixture. This is recorded as a test-environment limitation, not proof of a mod failure or proof that every graphics driver is supported.

Client configuration files are applied reliably after restarting the client. Full, reduced, and minimal visual modes are checked independently; reduced/minimal preserve a broad cone while decreasing burst and spark counts. Bright-flash and player-overlay toggles are also exercised.

Effective settings are recorded by the test client: `quality=MINIMAL flash=false overlay=true` and `quality=REDUCED flash=false overlay=true`. The reduced-mode run was also inspected with shader rendering disabled. The final preview in `build/release/omega-rush-preview.png` shows the full cone and forward-arm posture; its render fixture is explicitly cosmetic and stationary.

## Practical limits

The copied-pack checks prove startup, native crafting/casting, and coexistence with the installed Multiversal/Thundercrash binary. They do not certify arbitrary future versions of those mods. The latency case is a deterministic local 250 ms round-trip simulation, not a claim about every network condition. Moving ships, additional shader packs, and arbitrary armor renderers are outside this acceptance run.

Install only the release JAR. The `omega-rush-test.jar`, `omega_actor`, `omega_pose`, and `omega_latency` commands belong exclusively to development validation.

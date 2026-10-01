# Thundercrash 0.3.0 gameplay checks

Replace the older Grand Explosion JAR with `build/grand-explosion-0.3.0.jar` on the server and all clients. Do not install `grand-explosion-test.jar` in a normal world. Minecraft 1.20.1, Forge 47.4.10, and Iron's Spells 3.16.3 are the verified target.

## Quick first run

1. Open Iron's Scroll Forge and select Lightning. Confirm Thundercrash appears and craft its scroll using the forge's displayed ingredients. It starts at Legendary rarity and has five levels. Inscribe the scroll into a spellbook normally.
2. For a quicker operator test, run `/createScroll irons_ultimate_explosion:thundercrash 5` in player chat, then inscribe it. This is Iron's installed native command. Start in a clear area with enough mana; the default cost is 150.
3. Cast, look horizontally, then turn sharply left/right and aim upward/downward. Try first and third person. Expect a brief lift, committed camera steering, and an electrical aura and trail.
4. Hit a wall, floor, ceiling, fence, slab edge, then a mob. Expect one discharge, damage to valid nearby targets, and intact blocks. Cast into an immediately obstructed launch too.
5. Fly into open space until timeout. Expect propulsion to stop without a discharge, the first landing to avoid fall damage, and ordinary movement afterward. The landing protection expires after five seconds at normal TPS.
6. Confirm the flight sound stops after impact and timeout. During another flight, test resource reload, death, logout, and an operator teleport. Check that movement, gravity, animation, and sound recover.

A short video showing a turn, an impact, and an airborne timeout is the most useful first report. If something fails, include what happened, whether it was single-player or a dedicated server, and the matching `logs/latest.log` or crash report. Screenshots help with icon, aura, posture, and Scroll Forge appearance.

## Further acceptance

Compare Thundercrash directly with Ascension with shaders disabled and with the normal shader pack. Test all particle-quality settings. Have another player watch: the body, aura, trail, and discharge should line up, including when they start tracking a flight midway through it. Check for rubber-banding and flying kicks with server `allow-flight=false`; repeat with 100–250 ms latency and low TPS.

Check PvP enabled/disabled, teammates, and summons. Test survival and Creative, mounting, Elytra, water/lava entry, dimension changes, world-border/unloaded boundaries, and repeated casts. Lava still deals normal damage. Ordinary spell casts are blocked during flight. Run one, four, and eight simultaneous casters and watch frame/tick behavior.

For Valkyrien Skies, test both stationary and moving ship hulls. Controller movement retains `Entity.move` and damage visibility retains `Level.clip`; an unexpected ship collision uses the safe world movement endpoint. The server harness loads Valkyrien Skies, but it does not prove moving-ship contact accuracy.

## Automated validation

The disposable server is under `build/thundercrash-server`, with its own world and localhost port 25594. It uses the copied full modpack and the exact Forge 47.4.10 runtime. Common movement mixins loaded successfully. The harness uses synthetic players and invokes the server controller explicitly; it does not simulate real client latency or certify visual/audio quality.

Build using `build.ps1`, then `build-test.ps1`. In this disposable server console only, run `thundercrash_test`, `grand_explosion_test`, `grand_explosion_regalia_test`, and `grand_explosion_pvp_test`. The harness deliberately places/removes test blocks, so use a disposable world. Flight animation JSON was decoded successfully using the installed playerAnimator parser.

Final automated results and remaining acceptance gates are recorded below after the validation run.
### Final run: September 30, 2026

- Release and disposable test harness compiled with Java 17. Packaged resources, JSON, Mixin manifest, and exclusion of test classes were verified.
- All **42 Thundercrash assertions passed** on the full copied Forge 47.4.10 server. They cover Lightning school and Scroll Forge craftability, level scaling, full-body sweeps, steering, input rejection, one-shot damage, canceled damage events, PvP filters and damage, timeout and charge completion, aborts, gravity recovery, landing protection/expiry, safe boundaries, a strict 40-block directional fallback, and eight simultaneous sessions.
- Eight synthetic casters averaged **0.445 ms per controller tick**, measured over 30 explicitly invoked ticks in the final scene. This measures the controller and its state sends, not total server/frame time or real-client packet traffic.
- Grand Explosion's paid/empty-mana casts, terrain-off behavior, staff attributes/recipe/tag, Regalia recipes/attributes/damage/range/caster exclusion, and enabled/disabled PvP checks passed.
- The isolated server shut down and saved cleanly. Its final log is copied to `build/thundercrash-validation.log`.

Gameplay acceptance remains pending for real-client prediction, observer alignment, lag/flying checks, shader appearance, audio-loop quality/reload behavior, and stationary/moving ship contact. Custom audio is still represented by the agreed Iron's placeholders. No running remote server or existing Prism client was updated.

### Audited rebuild: September 30, 2026, 17:00 local time

This rebuild supersedes the earlier 42-assertion run. All **61 Thundercrash assertions passed** on the copied Forge 47.4.10 server, followed by the Grand Explosion, Regalia, and PvP regression checks. The server saved and shut down cleanly. The latest evidence is `build/thundercrash-validation.log`; the release checksum is `build/grand-explosion-0.3.0.sha256`.

The audit fixed the prediction budget so acknowledged/replayed steps cannot permit extra movement ahead of the server. Large corrections reset replay velocity and trail history, and an old impact cannot terminate a replacement session. It also added configurable edge damage, captured that setting at activation, moved visibility checks to the safe side of the impact, and limited interrupted-charge cleanup to pending charges. Additional checks cover prediction freeze/acknowledgement, native mana and cooldown handling, Lightning power capture, and late charge interruption.

Eight synthetic casters averaged **0.335 ms per controller tick** over 30 ticks. This remains a controller measurement rather than total server/frame time or real-client networking. The rebuilt release passed resource/JSON/Mixin manifest checks and contains no test harness classes. It is ready for user gameplay testing; the remaining acceptance gates above still apply.

### User gameplay evidence and separate candidate: September 30, 2026

The user installed the stable 0.3.0 test JAR in GPT; its SHA256 matches the 17:00 audited build. They supplied `C:/Users/jraym/Videos/Screen Recordings/Screen Recording 2026-09-30 171318.mp4` and reported: “i was able to steer and manuever exactly as i imagined. The feel of it was great!” Core guided steering is accepted by the user. The 26.54-second recording was sampled into 27 frames under `build/video-review-20260930`; the frames show charge/flight electricity, a dense curved wake with bolts, repeated flight termination, fading effects and ordinary movement afterward. The recorded terrain remains visibly intact. This sample does not prove real observer alignment, survival fall protection, damage values, audio stopping, shader comparisons, or every collision case.

The user is testing Lightning Scroll Forge crafting and an airborne timeout/first landing in Survival. Keep their current JAR unchanged while they do those checks.

The source audit added captured spell level, configurable base power/progression with unchanged defaults, and the planned occasional short bolt from the forward cluster to the body. Those changes are isolated in `build/grand-explosion-0.3.0-candidate.jar`. All **79 assertions passed** on Forge 47.4.10; additional cases cover configuration scaling, level capture, canceled damage without knockback, teams with friendly fire disabled/enabled, owned Iron's summons, Lightning resistance, outward/resistance-aware knockback, blocked line of sight, spherical radius and caster exclusion. Grand Explosion, staff, Regalia and PvP regressions passed afterward, and the server saved cleanly. Evidence is `build/thundercrash-candidate-validation.log`.

Measured synthetic controller means over 30 ticks were **0.033 ms / 0.119 ms / 0.247 ms** for **1 / 4 / 8** casters. These do not measure real-client frame time, server tick overhead or wire traffic.

An earlier candidate test assumed that Iron's helper universally excludes an owned vanilla wolf. That assumption was contradicted by the installed helper and runtime; the test was removed because the approved contract is Iron's friendly-fire rules. The implementation was not changed to create a different targeting policy. Native Iron's summon ownership and native team protection both passed their damage cases. The earlier diagnostic run is retained as `build/thundercrash-candidate-first-run.log`.

The subsequent Survival report was fall damage after a drop estimated at about 100 blocks. The user accepted the intended capped protection and explicitly waived repeating that high-drop check (“Just assume it works as intended”). No fall-protection tuning was changed. Record this as user acceptance of the cap, not runtime proof of the cause or a verified high-drop landing. The native/synthetic expiry and one-landing checks remain the existing evidence; real first-landing behavior in that report is unresolved.

### Expanded candidate validation: September 30, 2026, 17:41–17:45 local time

All **108 Thundercrash assertions passed**, followed by Grand Explosion, staff, Regalia and PvP regression checks. The disposable server saved and exited cleanly. Latest evidence is `build/thundercrash-candidate-validation.log`; the prior 79-check log is retained separately. The candidate checksum is `0DC4071B00A68A54C027A5EB3A087DD6BAA1C1AAC6B43B0CEFFEF3E80B7310D1`. The stable release and GPT-installed JAR remain unchanged.

The new checks use native slab and fence collision shapes, a ceiling, a diagonal corner, an obstructed launch, and an equal-distance block/entity tie. Lifecycle checks cover mounted, Elytra, spectator and sleeping cast rejection; Creative ability preservation; water/lava aborts; retained ordinary lava damage; and death/logout/dimension event-handler cleanup. Posting those events verifies handlers, not actual client disconnections or dimension transitions.

The input decoder now requires exactly 20 bytes. Codec roundtrip and rejection of extra or truncated payloads passed. The installed playerAnimator parser verifies the flight animation's looping metadata and enabled body/head/arm/leg pitch keyframes; this is data validation rather than proof of the rendered posture.

An actual Scroll Forge block and its bound native menu were tested in the disposable world. Thundercrash appears among Lightning-school spells. **One Legendary Ink + one Paper + one Lightning Bottle produces Thundercrash I**, and taking the output consumes each ingredient. Graphical forge UI feedback and actual spellbook inscription remain separate checks.

Synthetic controller means over 30 ticks were **0.066 / 0.234 / 0.184 ms** for **1 / 4 / 8** casters. These measure explicitly invoked controller work, not real network traffic, total server tick overhead or client frame time. Real observers, latency, ship contact and the full shader/audio matrix remain unverified.

### Launch configuration invariant

The movement audit found that a custom `launchVelocity` greater than `flightSpeed` bypassed the speed cap during launch. The shared server/client movement calculation now caps launch magnitude too. The default `(0, 0.35, 0.125)` horizontal-look launch remains exactly unchanged. Invalid speed, steering and launch lift inputs stop movement safely. Verification includes high-lift/low-speed configuration, unchanged default launch, nonfinite lift, invalid speed, zero look and initial momentum through a 180-degree turn.

The rebuilt candidate passed **114 assertions** at 17:50 local time, followed by the Grand Explosion regressions and clean shutdown. Candidate SHA256 is `727AB84EDF3BE082B387DD746F9DD687CFF2BB88FDA2EFE6BDD7350354481E7A`. Latest evidence remains `build/thundercrash-candidate-validation.log`; the prior 108-check run is retained as `build/thundercrash-candidate-108-validation.log`. Synthetic 1/4/8-caster controller means were 0.042/0.141/0.131 ms over 30 ticks. The installed GPT build is unchanged.

The user chose “Leave those unverified for now” for dedicated-server observer and stationary/moving ship checks. Those tests are deferred at the user's request, with no claim of successful verification. The remaining graphical/audio/latency matrix likewise has no independent runtime evidence beyond the accepted single-player clip.

### Native inscription and distribution handoff: September 30, 2026, 17:55 local time

All **118 assertions passed** after adding the native Inscription Table menu test. It takes the actual forged Thundercrash I scroll, accepts it with an ordinary Iron spellbook, processes the menu's normal selection/inscription buttons, preserves the spell and level, consumes the scroll and preserves the spellbook with one active spell. This closes native scroll-to-book integration; it does not assert graphical menu rendering.

The addon binary is unchanged from the 114-check run, which also passed Grand Explosion regressions. That earlier log is preserved as `build/thundercrash-candidate-114-validation.log`. The latest run and clean world save are in `build/thundercrash-candidate-validation.log`; the test server exited normally. No further regression rerun was needed for a harness-only change.

The distribution copy is `build/release/grand-explosion-0.3.0.jar`, with checksum file beside it. It is byte-identical to the candidate (SHA256 `727AB84EDF3BE082B387DD746F9DD687CFF2BB88FDA2EFE6BDD7350354481E7A`). The earlier root-level build and the GPT-installed build remain untouched. Install the distribution JAR in place of the old addon on matching clients/server; keep only one addon JAR. No test harness belongs in a normal pack.

### Final user acceptance: September 30, 2026

The user reported that the three requested gameplay checks all passed: visuals including the Ascension/shader comparison; sound after impact, timeout and resource reload; and ordinary movement/gravity/effect cleanup after teleport, death and leaving/rejoining. Their response was that tests one and two were “perfect,” number three had “no issues whatsoever,” and everything was working as expected. They explicitly asked to consider the mod finished and wrap it up.

These are user-reported gameplay passes on the installed test JAR, not new independently reviewed recordings or a claim that the later distribution was already installed. Guided steering had already been accepted. That distribution remains the preserved 118-check binary; its default movement matches the accepted tuning. Multiplayer observers, ship contact and broader latency/performance measurements remain deferred, and the high-drop landing retest remains waived. No further tests were required for that user-approved handoff.

### Named scroll and supplied recordings: 0.3.1

The Thundercrash-only follow-up passed **130 assertions**, plus Grand Explosion/staff/Regalia/PvP regressions, on the existing disposable Forge 47.4.10 server. The server saved and shut down cleanly. Evidence is `build/thundercrash-scroll-validation.log`.

- `/give @s irons_ultimate` suggestions include `irons_ultimate_explosion:thundercrash_scroll`.
- Native command item construction contains Thundercrash I immediately; save/load retains it and preserves an existing level V.
- Lightning Bottle focus resolves Thundercrash in the forge's enabled, craftable Lightning list.
- Legendary Ink + Paper + Lightning Bottle yields the named Thundercrash I scroll and consumes one of each.
- Both given and forged named scrolls pass native Inscription Table acceptance and button handling, preserving the spellbook, spell and level while consuming the scroll.
- Grand Explosion remains crafting-disabled.

The three user-supplied recordings decode with Minecraft's STB Vorbis library as mono 48 kHz with no clipped decoded samples. Their original bytes are preserved. Evidence is `build/thundercrash-audio-validation.log`; cast/flight/impact durations are approximately 1.086/2.641/5.880 seconds. These are file checks, not a new in-game listening or graphical UI check. Earlier user-accepted gameplay and deferred multiplayer/ship tests remain unchanged in scope.

Current distribution: `build/release/grand-explosion-0.3.1.jar`, SHA256 `D5A2557662FF1ADE4719181D6C091109988A221C2078A64227F670C3F871BDCA`. The previously installed GPT build remains untouched.

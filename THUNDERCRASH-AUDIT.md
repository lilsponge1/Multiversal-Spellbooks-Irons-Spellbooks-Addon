# Thundercrash completion audit

Thundercrash is finished and accepted by the user on September 30, 2026. Guided steering was accepted from gameplay and a supplied clip. The user subsequently reported that all three requested visual, sound and cleanup checks passed and explicitly asked to consider the mod finished and wrap it up. Native Scroll Forge crafting and spellbook inscription passed on the disposable server. The planned landing cap is accepted. Multiplayer/ship tests and broader latency/performance measurements remain deferred rather than claimed verified.

## Evidence matrix before final user acceptance

The matrix below records the earlier audit's evidence limits. Pending gameplay presentation/cleanup items were subsequently accepted by the user's final report, recorded at the end of this document. Broader unmeasured cases remain documented limits of the finished release, not open requirements after the user's wrap-up instruction.

| Plan requirements | Current evidence | Acceptance status |
| --- | --- | --- |
| §1–2 Extend Grand Explosion; exact target and mod ID | Shared ModSpells/GrandExplosionMod; copied Forge 47.4.10 startup; Iron's 3.16.3 dependencies; release metadata | Implemented and build/server verified |
| Lightning, Legendary five levels, Scroll Forge crafting, ordinary scroll/book integration | Native Scroll Forge produces Thundercrash I and consumes ingredients; native Inscription Table menu inscribes that output into an Iron spellbook, preserving spell/level and consuming the scroll | Native crafting and inscription verified; graphical menus not independently tested |
| §3 Ascension helper/assets, no applied Ascension effect | ThundercrashVisuals calls AscensionEffect.ambientParticles; references ParticleHelper; no effect applied | Source verified; recognition and density in-game pending |
| §4 Charge, launch, guided flight, impact, timeout, abort | Spell hooks; native mana/cooldown, interruption, flight and timeout assertions; user's charged casts and guided flight recording | Covered server cases verified; real-client charge and steering accepted; full gameplay lifecycle pending |
| Activation power/level progression, original gravity, dimensions, unique sessions | State captures spell level, computed power, gravity, vectors, settings and identity; level progression and Lightning power/capture assertions | Server capture verified, including level V through the spell hook |
| Initial tuning defaults | Spell and ThundercrashConfig values match stated defaults; base/progression and falloff are configurable; power/falloff captured per activation | Default/config progression verified; user accepts current steering feel; broader gameplay balancing pending |
| §5 Shared steering, capped prediction, no WASD acceleration, scoped movement ownership | Movement and Prediction classes; travel/local/connection mixins; replay/freeze budget assertions; common mixins loaded on exact server runtime | Arithmetic and server hooks verified; real-client travel/movement packet and latency behavior pending |
| Gravity restore, first landing protection, Creative abilities unchanged, original posture/free camera/no spin attack | Cleanup/recovery tests; Creative ability flags unchanged through controller flight; installed animation parser verifies enabled pitch keyframes; user accepts landing cap | Server state and animation data verified; full client posture and landing behavior not independently proved |
| §6 Swept body collision; earliest block/entity contact; safe bounds; no terrain change | Thin-wall/shoulder/floor/entity/wall/boundary assertions plus native slab, fence, ceiling, diagonal corner, obstructed launch and block/entity tie cases | Covered server geometry verified; broader gameplay geometry and ships pending |
| Entity.move/Level.clip/ship world endpoint, no chunk loading | Movement and visibility calls preserved; truncated movement uses actual endpoint; safe query checks loaded chunks | Source verified; stationary/moving ship contact accuracy pending |
| §7 Aura, trail spacing/history, arcs, forward cluster, discharge/ring/upward burst/cloud/shake; global/local budgets | Visuals implementation and packaged assets; Client sample queue, session/correction cleanup; user's clip shows body electricity, a dense wake, connecting bolts and cessation after flight | Initial appearance accepted by user; direct Ascension comparison, shader/quality matrix, first-person coverage and observer alignment pending |
| §8 Events/subtitles/loop lifetime/reload; placeholder substitution | ModSounds, sounds.json, FlightSound and client lifecycle paths; installed placeholder keys verified | Source/assets verified; audible attenuation, encoded loop, actual stop/reload behavior pending |
| Custom audio when supplied | No custom audio supplied; agreed Iron's placeholders retained; replacement instructions in README | Conditional asset replacement pending supply; placeholders implement the agreed initial strategy |
| §9 Protocol 3; ID 0 preserved; IDs 1–3; tracking/self/late tracking/nearby impact; validation/main threads | Fixed 20-byte input decoder; roundtrip, extra/truncated payload rejection and invalid-input assertions; tracking and main-thread source paths | Server input and codec verified; real observers, late tracking and packet traffic pending |
| §10 Lightning damage/falloff/LOS, friendly-fire/PvP, one pass, resistance-aware knockback, no explosion/fire | DamageSources, captured power/falloff, LOS from discharge, living knockback, terminal-before-callback; damage/PvP/cancellation/single-pass assertions plus native team, Iron's summon, Lightning resistance, occlusion, spherical radius and knockback tests | Added server damage cases verified; broad multiplayer gameplay and edge balancing remain pending |
| One cleanup path, recovery marker, shutdown, death/logout/dimension/teleport/fluids/mount/Elytra/Creative | Native mounted/Elytra/spectator/sleeping rejection; water/lava abort and retained lava damage; death/logout/dimension event-handler cleanup; gravity/recovery and clean shutdown | Covered cases verified; actual network transitions and saved marker across forced shutdown/login pending |
| §11–12 Java/resources/build/docs/integration/order | Main classes, mixin manifest/config, icon, animation, sounds/localization, build scripts and docs packaged; harness excluded | Build/package verified; no custom recordings supplied |
| §13 All single-player/dedicated observer/latency/ship/VFX/audio/performance gates | Synthetic server harness and regression logs; 1/4/8-session controller means measured; installed client JAR hash matches stable test build; clip and steering feedback | Core single-player steering accepted; dedicated observers, latency, ships, frame time, real tick overhead, packet traffic and full visuals/audio matrix remain incomplete |
| Grand Explosion regressions including sound/network/staff/armor | Existing automated damage/mana/PvP/staff/Regalia checks rerun | Covered server checks verified; actual audio/network gameplay pending |
| §14 Guided-first and directional fallback | Guided default true; fixed direction, strict 40-block total travel cap, no timeout discharge, same client presentation | Server fallback verified; guided latency quality and presentation acceptance pending |

## Current testing handoff

The user installed `build/grand-explosion-0.3.0.jar` in GPT, verified by matching SHA256 `4BE42DC1F66CC99E8D52991DC21B1A0E1254ABD8315B8D410377CF2E5E978CFB`. That test build remains unchanged. The later source/config additions, short forward-to-body arc, strict input decoder and launch-speed cap are packaged as `build/release/grand-explosion-0.3.0.jar`, byte-identical to `build/grand-explosion-0.3.0-candidate.jar`. **118 assertions passed** after the inscription check; Grand Explosion regressions passed against the same addon bytes in the prior 114-check run. Release SHA256 is `727AB84EDF3BE082B387DD746F9DD687CFF2BB88FDA2EFE6BDD7350354481E7A`. Latest evidence is `build/thundercrash-candidate-validation.log`; regression evidence is retained in `build/thundercrash-candidate-114-validation.log`. Do not silently replace the user's current test build.

The user explicitly chose to leave dedicated-server observer and stationary/moving ship tests unverified for now. These remain documented gaps; do not request those tests again unless the user reopens them. The high-drop landing retest was also waived. Neither decision converts missing runtime evidence into a passing test.

See THUNDERCRASH-TESTING.md for gameplay evidence and remaining checks. A green synthetic harness is not evidence of graphical or real-network acceptance.

## Implementation audit and earlier validation dependency

The post-handoff audit rechecked the packaged distribution checksum, successful native crafting/inscription log, clean shutdown, spell defaults, particle budgets and trail discontinuity guards, sound-event references, scoped movement hooks and network registration. No further implementation defect was established. The latest distribution remains the 118-check binary identified above; additional server reruns would repeat existing evidence.

Before final user acceptance, the remaining validation dependency was real-client evidence for the shader/particle-quality comparison with Ascension, sound-loop stop/reload behavior and attenuation, latency and frame/tick/wire measurements, actual network lifecycle transitions and saved-state recovery. Observer and ship checks had explicitly been deferred by the user, and the high-drop retest was waived. Further synthetic assertions could not establish those remaining runtime claims.

The goal was previously blocked on that validation dependency. The user's final report and wrap-up instruction now close the requested implementation at the accepted verification scope. Do not reopen deferred tests or change the accepted default flight feel on automatic continuation.

## Final acceptance and release

The user reported: “test number one and two that you wanted are perfect” and “number three has no issues whatsoever,” then explicitly instructed: “consider the mod finished and wrap it up.” These refer to the requested Ascension/shader visual comparison, impact/timeout/resource-reload sound checks, and teleport/death/logout/rejoin cleanup checks. Record those as user-reported passes; no additional independent recording was supplied.

The user's installed GPT JAR still has SHA256 `4BE42DC1F66CC99E8D52991DC21B1A0E1254ABD8315B8D410377CF2E5E978CFB`. The distribution includes later source-audit changes and remains SHA256 `727AB84EDF3BE082B387DD746F9DD687CFF2BB88FDA2EFE6BDD7350354481E7A`, with 118 passing server assertions and same-binary Grand Explosion regressions. User gameplay acceptance covers the installed test build; the later changes retain its default movement and add configuration/validation refinements plus a short forward arc. Do not represent the distribution as already installed or independently gameplay-tested in GPT.

No requested implementation or packaging work remained at the 0.3.0 handoff. Custom audio used the agreed Iron's placeholder strategy until recordings were supplied. The deferred multiplayer, ship, latency and full performance matrix was not certified by that release. Its artifact remains preserved at `build/release/grand-explosion-0.3.0.jar`.

## Follow-up: named scroll and supplied audio, 0.3.1

The user requested `/give` discovery and Scroll Forge access for Thundercrash only, then supplied three `.ogg` files through the active goal. `ThundercrashScrollItem` extends Iron's native Scroll and implements its public `IPresetSpellContainer` API, so `/give` constructs a populated Thundercrash I item immediately. The existing Scroll Forge recipe remains authoritative; a narrow menu mixin converts only valid Thundercrash output into the named item, preserving its NBT and level. A second narrow mixin admits that item to Iron's otherwise exact-item-only inscription slot. Grand Explosion remains crafting-disabled.

All **130 Thundercrash assertions passed** on the disposable Forge 47.4.10 server, followed by Grand Explosion, staff, Regalia and PvP regressions and a clean save/shutdown. Evidence: `build/thundercrash-scroll-validation.log`. New assertions cover real Brigadier `/give` suggestions, native ItemInput construction, immediate spell data, save/load, higher-level preservation, Lightning focus filtering, named forge output, and inscription of both given and forged scrolls.

The user's cast, flight and impact Vorbis recordings are bundled unchanged at the documented sound paths. Minecraft's bundled STB decoder successfully decoded all three as mono 48 kHz, with no clipped decoded samples. Durations are 1.086 / 2.641 / 5.880 seconds; evidence: `build/thundercrash-audio-validation.log`. This establishes file usability; perceived volume and the supplied flight-loop seam have not had a new in-game listening check. Accepted movement, damage and visual behavior was not retuned.

Distribution: `build/release/grand-explosion-0.3.1.jar`, SHA256 `D5A2557662FF1ADE4719181D6C091109988A221C2078A64227F670C3F871BDCA`. The older releases and GPT installation remain unchanged. Install matching 0.3.1 copies on server and clients, replacing the old addon. Earlier deferred tests remain deferred.


## Corrected impact recording: 0.3.2

The user replaced the source Thundercrash_impact.ogg with a corrected recording. Its SHA256 is 1305FD250BB0F6BC592AB3F6387A2715E4270410E8F26D866ACD6767861A7BA2. It was copied unchanged and decoded with Minecraft STB Vorbis as mono 48 kHz, 2.174 seconds, with no clipped decoded samples. Evidence: build/thundercrash-impact-0.3.2-validation.log.

A full comparison of release entries against 0.3.1 confirmed that only the impact audio and META-INF/mods.toml version differ. All classes, cast/flight sounds, other assets and mixin configuration remain byte-identical. Existing 130-assertion gameplay and Grand Explosion regression evidence applies to the unchanged code; this resource-only update did not rerun the server harness. Previous deferred tests remain deferred.

Current distribution: build/release/grand-explosion-0.3.2.jar; SHA256 4F66CD1DEB2DA0376C77760A77578B84B3F48DD79A6C7602145460147BE65698. The prior releases and GPT installation remain preserved. Replace the old addon JAR on matching clients and server.

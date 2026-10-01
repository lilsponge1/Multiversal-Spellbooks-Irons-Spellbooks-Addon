# Thundercrash implementation plan

## 1. Summary and confirmed decisions

Add `irons_ultimate_explosion:thundercrash` to the existing Grand Explosion addon.

The spell will use a brief electrical charge and upward launch, followed by committed, camera-guided flight. Colliding with terrain or a valid living target will discharge a large Lightning-school attack. The presentation will combine **Ascension’s actual electricity particles and aura helper**, a continuous interpolated trail, concentrated forward energy, and Iron’s existing lightning and blastwave effects.

Confirmed preferences:

- Extend Grand Explosion rather than create another addon.
- Enable PvP, subject to server PvP settings and Iron’s friendly-fire rules.
- Leave terrain intact.
- On flight timeout, end without detonating and provide temporary landing protection.
- Use the verified `irons_spellbooks:ascension` as the primary visual reference.
- Implement guided flight first; retain a directional-charge fallback.

This is a plan only. No implementation or project edits have been made.

## 2. Relevant code, versions, and available infrastructure

### Verified environment

| Component | Available version / finding |
|---|---|
| Minecraft | **1.20.1** |
| Loader | **Forge 47.4.10**, confirmed in Prism metadata and copied-server libraries |
| Iron’s Spells ’n Spellbooks | **1.20.1-3.16.3**, confirmed from JAR metadata |
| Separate Lightning Spellbook addon | Not found in the current server archive or either Prism instance |
| Ascension provider | **Iron’s Spells itself** |
| Iron’s Library | **1.20.1-2.1.0** |
| GeckoLib | **Forge 1.20.1-4.8.4** |
| playerAnimator | **1.0.2-rc1+1.20** |
| Traveloptics | **6.3.0-1.20.1** |
| Valkyrien Skies / Eureka | **2.4.11 / 1.6.3** in the copied test pack |
| Existing custom addon | `irons_ultimate_explosion`, currently packaged as Grand Explosion **0.2.0** |
| Build tools | Bundled Java 17, local dependencies, PowerShell build script |
| Particle implementation | Iron’s native particle registry and custom particle classes; no new particle library is needed |

The available files establish the local target. They do not independently confirm the contents of a remote running server.

### Existing custom project

The implementation belongs in the existing [Grand Explosion source directory](</C:/Users/jraym/Documents/ChatGPT/Debugging of server crashing/irons-ultimate-explosion/src/main/java/local/ironsultimateexplosion>).

Useful infrastructure already exists:

- `ModSpells`: registers `AbstractSpell` instances through Iron’s spell registry.
- `GrandExplosionMod`: registers spell, config, networking, and client systems.
- `ExplosionConfig`: separate server and client configuration specifications.
- `ExplosionNetwork` / `ExplosionPacket`: Forge `SimpleChannel`, currently protocol `"2"`.
- `ClientEffects`: client tick processing, effect expiry, distance-based particle density.
- `GrandExplosionSpell`: working examples of Iron’s damage source, spell power, casting hooks, and multiplayer effects.
- `src/test`: disposable server harness excluded from the release JAR.

Thundercrash will share this infrastructure while keeping its state, effects, and damage logic separate from Grand Explosion’s crater and nuclear presentation.

### Inspected reference classes

Classes below are in the installed [Iron’s Spells 3.16.3 JAR](</C:/Users/jraym/Documents/ChatGPT/Debugging of server crashing/analysis/magic-compat/inputs/irons_spellbooks-1.20.1-3.16.3.jar>). Their relevant methods were inspected through bytecode.

| Reference | Relevant methods / behavior | Thundercrash use |
|---|---|---|
| `spells.lightning.AscensionSpell` | `onCast`, `onClientCast`, `getEmptyCastData` | Launch and impulse synchronization reference |
| `effect.AscensionEffect` | `ambientParticles`, `clientTick`, per-tick effect application | Primary aura reference |
| `particle.ElectricityParticle` | Constructor, tick, sprite animation, fullbright rendering | Existing electrical appearance |
| `util.ParticleHelper` | `ELECTRICITY`, `ELECTRIC_SPARKS` | Reusable particle options |
| `player.ClientPlayerEvents` | `onClientEntityTick` | Executes `ISyncedMobEffect.clientTick` for client-side living entities |
| `mixin.LivingEntityMixin` | Effect-added, updated, and removed hooks | Synchronizes marked effects to tracking clients |
| `spells.fire.BurningDashSpell` | `onCast`, `onClientCast`, `ImpulseCastData` | Propulsion reference; not a sustained flight controller |
| `spells.lightning.ElectrocuteSpell` | `CastType.CONTINUOUS`, persistent projectile cast data | Demonstrates sustained casting; unsuitable as Thundercrash’s complete lifecycle |
| `spells.lightning.ThunderStepSpell` | Recast state and `onRecastFinished` | Existing optional recast mechanism |
| `spells.lightning.ShockwaveSpell` | `onCast`, damage, target filtering, particles | Lightning impact reference |
| `particle.ZapParticleOption` | Public destination-based constructor | Actual electrical bolts between two positions |
| `particle.BlastwaveParticleOptions` | Public color-and-scale constructors | Expanding impact discharge |
| `api.util.RaycastBuilder` | `start`, `end`, `checkForBlocks`, `bbInflation`, `filter` | Supporting raycasts; not sufficient alone for sweeping the player’s full body |
| `damage.DamageSources` | `applyDamage`, `isFriendlyFireBetween` | Spell damage events and friendly-fire handling |
| `DeadKingAmbienceSoundInstance` | Tickable looping sound and explicit stopping | Audio architecture reference |
| playerAnimator APIs | `PlayerAnimationAccess`, `PlayerAnimationRegistry`, `ModifierLayer`, `KeyframeAnimationPlayer` | Original flight posture without enabling a damaging spin attack |

## 3. Detailed Ascension findings and reuse analysis

### What Ascension actually does

`AscensionSpell` is an instant Lightning spell. Its installed implementation:

1. Applies `MobEffectRegistry.ASCENSION` for **80 ticks**.
2. Creates a visual-only vanilla lightning bolt with damage set to zero.
3. Applies a nearby Lightning-school attack.
4. Computes its launch impulse from the normalized horizontal look direction:

   ```text
   impulse = (horizontalLook + (0, 5, 0)) × 0.125
   ```

   This produces approximately **0.125 horizontal** and **0.625 upward** impulse.

5. Adds that impulse to server velocity and marks an impulse.
6. Stores `ImpulseCastData` for the client cast callback.

The client callback applies the synchronized horizontal components and preserves an existing upward velocity when it exceeds the supplied vertical component. This is useful for a launch, but it does not implement guided flight.

`AscensionEffect`:

- Resets fall distance every effect tick.
- Implements `ISyncedMobEffect`.
- Calls its public static `ambientParticles(ClientLevel, LivingEntity)` helper from `clientTick`.

The aura helper emits **two `ParticleHelper.ELECTRICITY` particles per invocation**:

- Random positions around the entity, using its width and height.
- Horizontal spread parameter approximately `0.4`.
- Random initial velocity on each axis in approximately `[-0.04, +0.04]`.
- Client-local generation.
- No look-direction or velocity-oriented placement.
- No attachment of existing particles to the caster after spawning.

`ElectricityParticle` supplies the recognizable appearance:

- Four electricity sprites.
- Random sprite changes and small positional jitter.
- Zero particle gravity.
- Fullbright lighting.
- Approximately **5–19 ticks** lifetime.
- Velocity friction approximately `0.77`.

**Ascension’s inspected aura uses animated electricity particles, not destination-based lightning arcs.** Its dramatic initial bolt is separate. Thundercrash’s connecting arcs will therefore come from Iron’s Shockwave particle family.

### Reuse classification

| Category | Concrete decision |
|---|---|
| **Directly reusable** | Call `AscensionEffect.ambientParticles(...)`; use `ParticleHelper.ELECTRICITY`; reference the existing electricity sprites and particle provider through Iron’s registry. |
| **Copy/adapt behavior** | Reproduce Ascension’s entity-relative random placement and low random particle velocity in an addon-owned sampler that accepts arbitrary positions, density, and orientation. This supports trail samples and the forward cluster. |
| **Must recreate** | Sustained movement, steering, session state, collision sweeping, timeout, recovery, and movement prediction. Ascension provides none of these as a reusable controller. |
| **Thundercrash-specific additions** | Interpolated trail, occasional connecting bolts, forward energy cluster, stronger impact discharge, flight posture, moving audio loop, and bounded multiplayer state packets. |

Use public APIs and reference upstream assets in place. There is no need to bundle copies of Iron’s textures or transplant entire upstream classes.

Do not apply the actual Ascension effect merely to obtain its visuals: that would couple Thundercrash to an independently expiring effect and complicate cleanup or simultaneous Ascension use.

## 4. Recommended lifecycle and spell architecture

Use an **ordinary charged Iron’s spell plus an independent, short-lived server flight session**.

A continuous spell would tie the charge to holding the cast control and Iron’s channel lifecycle. The requested behavior is an activated ability that continues while the player aims.

### Lifecycle

```text
Charge → Successful cast → Upward launch → Guided flight
                                       → Collision → Detonation → Cleanup
                                       → Timeout   → Safe landing
                                       → Abort     → Cleanup
```

- **Charge:** `CastType.LONG`, default 10 ticks. Ascension aura intensifies, charge sound plays, and `PREPARE_CROSS_ARMS` provides the existing preparation animation.
- **Successful cast:** Iron’s normal mana/cooldown handling completes once. Create the flight session.
- **Launch:** Four ticks of upward movement, with collision checks active.
- **Guided flight:** Up to 80 ticks, using camera direction and a shared movement model.
- **Collision:** Stop at the earliest valid contact, detonate once, then clean up.
- **Timeout:** Stop propulsion without impact damage or impact sound; restore ordinary movement and protect the initial landing.
- **Abort:** Death, disconnect, dimension transition, external teleport, invalid movement state, or unsafe world boundary ends the session without damage.

Use `onServerPreCast`, `onServerCastTick`, `onCast`, and `onServerCastComplete`. A successful `onServerCastComplete` must not remove a flight session just created by `onCast`; interruption removes only the pending charge.

### Session data

`ThundercrashState` will hold:

- Caster UUID, dimension, and unique session ID.
- Phase and remaining server ticks.
- Spell level and damage power captured at activation.
- Authoritative position, velocity, and recent accepted path samples.
- Latest validated look input and input sequence.
- Original gravity state.
- Last safe position.
- Terminal/detonated flag.

`ThundercrashManager` owns active sessions and one idempotent termination method. Persistent capabilities and projectile entities are unnecessary for this short-lived state.

### Initial tuning defaults

These are prototype values, exposed for later adjustment:

| Setting | Default |
|---|---:|
| Rarity / maximum level | Legendary / 5 |
| Charge / launch | 10 / 4 ticks |
| Maximum guided flight | 80 ticks |
| Cruise speed | 0.85 blocks per tick |
| Upward launch speed | 0.35 blocks per tick |
| Steering interpolation | 0.18 per tick |
| Impact radius | 6 blocks |
| Base spell power / per level | 40 / 10 |
| Mana cost | 150 |
| Cooldown | 120 seconds |
| Knockback strength | 1.5 |
| Trail spacing | 0.5 blocks |
| Landing protection | First landing, capped at 100 ticks |

At 20 TPS, cruise speed is approximately **17 blocks/second**, with roughly **68 blocks** of theoretical straight flight before acceleration and collision. Practical engagements should commonly cover 20–50 blocks.

Use normal Iron’s scroll/spellbook integration. No Thundercrash staff or armor set is included in this change.

## 5. Movement and multiplayer synchronization

### Movement model

Maintain momentum while turning toward the look vector:

```text
desired = normalizedLook × configuredSpeed
velocity = lerp(previousVelocity, desired, steeringResponsiveness)
```

Cap velocity magnitude at the configured speed. Prevent invalid or zero-length inputs from producing NaNs. WASD must not add ordinary walking acceleration during flight.

Use a shared `ThundercrashMovement` calculation for the server controller and local prediction.

### Authority and prediction

The server advances the player once per server tick. The controlling client predicts the same movement for responsiveness. Clients send **look input**, never authoritative positions, collision results, or damage.

Nearby clients observe the real player entity through normal entity tracking, supplemented by Thundercrash state for cosmetics.

Repeated server velocity changes alone are insufficient as the complete design: ordinary player positions normally arrive from client movement packets. Explicitly handle that interaction during the active session.

### Narrow movement hooks

The following targets were verified in the Forge 47.4.10 runtime JARs:

- `LivingEntity.travel(Vec3)` — SRG `m_7023_`.
- `LocalPlayer.sendPosition()` — SRG `m_108640_`.
- `ServerGamePacketListenerImpl.handleMovePlayer(...)` — SRG `m_7185_`.

Add scoped mixins:

- **Travel hook:** replace normal travel only for an active Thundercrash player. The server manager performs authoritative movement; the local player performs prediction. Other entities use normal travel.
- **Local position hook:** while movement ownership is active, transmit Thundercrash look input instead of competing ordinary position updates.
- **Server packet hook:** after Minecraft’s main-thread handoff, prevent ordinary movement packets from overwriting the active session’s authoritative position.
- **Connection support:** clear floating counters only for a server-authorized active session; perform a normal teleport acknowledgement handoff when returning to vanilla movement.
- **External teleport hook:** end Thundercrash before unrelated teleports, excluding the controller’s own final handoff.

The relevant floating fields are `clientIsFloating` / `aboveGroundTickCount`, SRG `f_9736_` / `f_9737_`.

Do not globally disable flying or movement checks. Every hook must have a fast inactive path.

Call `Entity.move(MoverType.SELF, displacement)` for actual movement, preserving vanilla and installed-mod collision processing. Update server chunk tracking after controller movement.

### Gravity and presentation

- Temporarily set `noGravity`; capture and restore its prior value.
- Reset fall distance during flight.
- Leave Creative flight permissions, movement attributes, and collision enabled.
- Use an original playerAnimator flight animation with arms forward and legs trailing.
- Keep the camera freely aimable; do not force third person.
- Do not use `startAutoSpinAttack` for posture: it introduces unrelated attack behavior.

## 6. Collision and impact-point determination

Implement `ThundercrashCollision` as a **swept player-body collision query**, not a check at the final position.

For each intended step:

1. Construct the union of the starting and intended player bounding boxes.
2. Verify finite coordinates, loaded chunks, world border, and build-height limits.
3. Sweep the body against block collision shapes.
4. Query living entities within the swept bounds.
5. Compute the earliest block or entity contact.
6. Move only to the last non-overlapping position before contact.
7. Detonate at that contact location.

For block shapes, use continuous AABB-versus-shape-box intersection. For entities, sweep the player’s center against target boxes expanded by the player’s half-extents. This prevents shoulders, feet, thin walls, and small targets from being missed by a center ray.

`Level.clip` and Iron’s `RaycastBuilder` remain useful for supporting contact and visibility queries. They are not the only collision authority.

Rules:

- Ground counts as a block impact.
- Exclude the caster, spectators, dead targets, and friendly targets.
- Player collision follows the configured PvP rules.
- A block wins an equal-distance tie with an entity.
- Initial floor contact is not an impact when the launch is moving away from it.
- An obstructed launch or immediate wall contact terminates correctly.
- Compare requested and actual `Entity.move` displacement; unexpected collision truncation must also terminate the charge.

**Valkyrien Skies:** its installed infrastructure modifies `Level.clip` and entity movement for ships. Preserve these calls, test stationary and moving ship hulls, and never treat shipyard block coordinates as ordinary world impact coordinates. Until transformed contact details are available, use the actual safe world-space movement endpoint for a ship impact.

Do not load chunks to complete a charge. Stop safely at an unloaded boundary.

## 7. VFX implementation

### Ascension-derived aura

- During charge, increase helper calls from one toward four per client tick.
- During guided flight, default to four helper calls: **eight original electricity particles per tick**.
- Add a small number of velocity-biased electricity emissions around the body using the adapted sampler.
- Recompute the emission center every tick from the caster’s current rendered position.
- Generate effects for every visible active caster, including the controlling player.

The original particles remain in world space after emission. Fresh body-centered emissions maintain the aura; lingering particles contribute to the wake. Do not describe these as permanently attached particles.

### Continuous electrical trail

Maintain a short client path history derived from authoritative movement samples.

- Sample each traveled segment at configurable distance intervals.
- Carry the spacing remainder between segments.
- Emit two electricity particles per default 0.5-block sample.
- Add occasional `ELECTRIC_SPARKS`.
- Retain approximately 12 recent path points for short connecting bolts.
- Never draw a trail across a teleport, dimension change, session replacement, or large correction.

At default speed, this adds approximately **3–4 electricity particles per tick**, with no per-particle networking.

### Electrical arcs

Use `new ZapParticleOption(destination)` at a chosen starting position.

- Spawn one short connecting arc approximately every four ticks.
- Connect recent trail points or nearby body positions.
- Bound arc length and history size.
- Treat these as Shockwave-derived additions; Ascension’s aura helper does not generate them.

### Forward cluster

Place denser emissions approximately 0.8 blocks ahead of the torso along the actual flight direction:

- Four electricity particles per tick.
- A few cyan sparks.
- Occasional short bolts back toward the caster.

Avoid covering the controlling player’s entire first-person view.

### Impact discharge

One authoritative impact packet starts:

- A concentrated electricity/spark burst.
- Approximately 8–12 short radial `ZapParticleOption` bolts.
- A cyan-white `BlastwaveParticleOptions` discharge.
- An expanding ring over roughly 10–15 ticks.
- A brief upward electrical burst.
- Small cloud particles and restrained camera shake.

Generate these client-side. Use cosmetic particle bolts rather than damaging vanilla lightning entities, preventing extra damage, fire, transformations, and thunder.

No new particle renderer or texture family is required for the first version. Visual acceptance must compare Thundercrash directly with Ascension, both with shaders disabled and with the pack’s usual shader configuration.

## 8. Audio implementation and required files

Minecraft’s installed audio decoder uses Vorbis. Final assets should be **Ogg Vorbis `.ogg`**, rather than another codec placed in an Ogg container. Sound definitions belong in `assets/<namespace>/sounds.json`, with audio under `sounds/`. [Forge 1.20.1 sound documentation](https://docs.minecraftforge.net/en/1.20.1/gameeffects/sounds/)

### How to provide the audio

Provide three separately named files, preferably in one ZIP:

| Filename | Recommended content |
|---|---|
| `thundercrash_cast.wav` or `.ogg` | Roughly 0.5–0.75 seconds of electrical buildup and launch |
| `thundercrash_flight.wav` or `.ogg` | Seamless 1–2 second sustained electrical loop |
| `thundercrash_impact.wav` or `.ogg` | Strong transient plus approximately 1–3 seconds of discharge; up to 5 seconds with a tail |

Requirements and preparation defaults:

- **Mono** for all three positional effects. Standard OpenAL spatialization applies to mono sources; stereo behaves differently. [OpenAL spatialization reference](https://openal.org/pipermail/openal/2017-May/000633.html)
- Prefer **48 kHz**; 44.1 kHz is also acceptable.
- WAV sources may be 16- or 24-bit PCM and will be converted during asset preparation.
- Prefer peaks around **−3 dBFS**, with no clipping or excessive limiting.
- Roughly −18 to −14 LUFS is a starting mixing target, not a Minecraft requirement.
- Make the flight loop seamless across its actual file boundaries, without leading silence or an impact transient.
- No special loop metadata is required; playback code controls looping.
- Verify the encoded Ogg loop, since conversion can expose boundary clicks or gaps.

### Registration and playback

Add three registered events:

- `irons_ultimate_explosion:thundercrash_cast`
- `irons_ultimate_explosion:thundercrash_flight`
- `irons_ultimate_explosion:thundercrash_impact`

Use a `ModSounds` deferred register, `sounds.json`, and localized subtitles.

- **Cast:** one positional charge event per session.
- **Flight:** `ThundercrashFlightSound extends AbstractTickableSoundInstance`; follows the caster each client tick, loops with zero delay, uses positional attenuation and `SoundSource.PLAYERS`.
- **Impact:** one positional event started by the same packet as impact particles.
- Default volume multipliers: cast `1.0`, flight `0.7`, impact `2.0`, all configurable.

Keep one loop instance per caster/session. Stop it immediately on impact, timeout, abort, tracking loss, world unload, disconnect, and resource reload. Do not repeatedly broadcast sound events to simulate a loop.

Use short, buffered sounds with `stream: false` initially. The existing Dead King loop confirms that tickable looping sounds are feasible.

### Placeholder and replacement strategy

Development can proceed without custom audio:

- Cast placeholder: `irons_spellbooks:spell.shockwave.prepare`.
- Flight placeholder: `irons_spellbooks:lightning_woosh_01`.
- Impact placeholder: `irons_spellbooks:spell.shockwave.cast`.

Initially, logical Thundercrash events reference these existing events through `sounds.json`. Later, map them to:

```text
sounds/thundercrash/thundercrash_cast.ogg
sounds/thundercrash/thundercrash_flight.ogg
sounds/thundercrash/thundercrash_impact.ogg
```

This initial asset substitution needs no Java changes. Subsequent replacements retain those filenames. A client resource pack can override the same assets.

## 9. Networking and responsibility boundaries

Extend the existing `ExplosionNetwork` channel to protocol **`"3"`**. Preserve Grand Explosion’s packet ID 0 and add:

| ID | Packet | Responsibility |
|---|---|---|
| 1 | `ThundercrashInputPacket` | Caster → server: session, sequence, finite yaw/pitch |
| 2 | `ThundercrashStatePacket` | Server → clients: identity, dimension, phase, tick, position, velocity, accepted input, remaining duration, recent path samples |
| 3 | `ThundercrashImpactPacket` | Server → nearby clients: unique impact/session, contact position, radius, presentation seed |

Defaults:

- Input: once per controlling-client tick while active.
- State: every two server ticks, plus immediate phase transitions.
- Include the last two server movement samples to preserve the traveled path through turns.
- Send state to tracking players and self.
- Send a current snapshot when another player starts tracking.
- Send impact to nearby clients even if they did not previously track the caster.

Validate session ownership, sequence, finite values, packet size, direction, and rate. Apply changes on the correct main thread. These rules match Forge’s packet-handling guidance. [Forge 1.20.1 networking documentation](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/)

The client predicts only a bounded interval ahead of the latest server snapshot. Authoritative collision and termination always override prediction. During a prolonged server stall, freeze prediction instead of continuing to fly through the world.

Particle density, audio, and animation remain client responsibilities. Damage and impact location remain exclusively server responsibilities.

## 10. Damage, cleanup, and edge cases

### Damage

At successful activation, capture:

```text
power = getSpellPower(spellLevel, caster)
```

Iron’s implementation already includes spell-level progression, general Spell Power, Lightning school power, and its configured power multiplier. Do not multiply those attributes again.

On collision:

- Query living entities within the radius.
- Apply spherical distance filtering and block line of sight from the discharge.
- Exclude the caster and friendly targets.
- Respect server PvP settings.
- Apply damage through:

  ```text
  DamageSources.applyDamage(target, damage, spell.getDamageSource(caster))
  ```

- Use configurable radial falloff, initially full damage at the center and 25% at the edge.
- Apply radial knockback only when damage succeeds, using the living entity knockback API so resistance remains relevant.
- Ensure the direct collision target is included once.
- Do not create a vanilla explosion, crater, fire, or additional damaging lightning entity.

### Cleanup

One termination method must:

1. Mark the session terminal before damage or callbacks.
2. Remove active movement ownership.
3. Restore the captured gravity state.
4. Clear excessive residual velocity and reset fall distance.
5. Send the terminal state and perform the vanilla position handoff.
6. Stop client aura, trail generation, animation, and flight audio.
7. Clear session buffers and references.

For airborne completion, provide addon-owned first-landing protection, capped at 100 ticks. It cancels fall damage and clears on landing or expiry; it grants no general invulnerability.

### Edge-case decisions

- Reject casts while mounted, sleeping, spectating, or already Thundercrashing.
- Reject active Elytra flight and conflicting propulsion effects.
- Permit Creative casting but leave flight permissions untouched.
- Prevent other Iron’s movement casts during active Thundercrash.
- Death, logout, dimension change, and external teleport abort without detonation.
- Water or lava entry ends propulsion safely; lava retains ordinary damage.
- World border, unloaded chunks, and unsafe height boundaries abort.
- Use server ticks for duration; low TPS must not trigger catch-up movement.
- Handle simultaneous casters independently, with one discharge per session.
- Omit manual detonation and new keybinds from this version.

Before setting gravity, save a small original-state recovery marker in player persistent data. Restore and clear it on login if an unclean shutdown left a saved player with Thundercrash-owned gravity state. Normal shutdown also cleans sessions before player saving.

## 11. Files to add or modify

All proposed Java classes remain under `local.ironsultimateexplosion`.

| File/class | Purpose |
|---|---|
| `ThundercrashSpell.java` | Iron’s registration metadata, casting hooks, tooltip, animations |
| `ThundercrashState.java` | Session data and terminal state |
| `ThundercrashManager.java` | Server ticking, lifecycle, damage, cleanup, recovery |
| `ThundercrashMovement.java` | Shared steering and propulsion calculations |
| `ThundercrashCollision.java` | Swept block/entity collision |
| `ThundercrashClient.java` | Prediction, observer state, interpolation, watchdog |
| `ThundercrashVisuals.java` | Ascension aura, trail, bolts, impact |
| `ThundercrashAnimations.java` | playerAnimator flight layer and cleanup |
| `ThundercrashFlightSound.java` | Moving, explicitly stopped loop |
| `ModSounds.java` | Three sound-event registrations |
| Three packet classes | Input, state, impact |
| `mixin/ThundercrashTravelMixin.java` | Scoped travel replacement |
| `mixin/ThundercrashLocalPlayerMixin.java` | Scoped movement-input transmission |
| `mixin/ThundercrashConnectionMixin.java` | Movement ownership, floating state, teleport cleanup |

Modify:

- `ModSpells`: register Thundercrash.
- `GrandExplosionMod`: register manager, sounds, and client systems.
- `ExplosionConfig`: add separate `thundercrash` server/client sections.
- `ExplosionNetwork`: protocol update and packet registration.
- `build.ps1` / `build-test.ps1`: add Mixin compile dependency and manifest packaging.
- `META-INF/mods.toml`: version **0.3.0** and updated description.
- `en_us.json`: spell name, guide, errors, subtitles, and death messages.
- README and existing plan documentation: installation, controls, settings, audio replacement.

Add resources beneath `assets/irons_ultimate_explosion`:

- `textures/gui/spell_icons/thundercrash.png`
- `player_animation/thundercrash_flight.json`
- `sounds.json`
- Three custom audio assets when supplied.

Add the root Mixin configuration and a separate Thundercrash test harness. Preserve existing Grand Explosion IDs and config keys.

The current build uses an older cached Minecraft/Forge compile artifact alongside Forge 47.4.10 libraries. Movement-hook work must use the verified 47.4.10 client/server signatures and validate packaged mixins against that runtime.

## 12. Implementation order

1. Register the spell, icon, localization, and tuning settings.
2. Add charge/session lifecycle and idempotent cleanup.
3. Prototype authoritative movement and vanilla movement handoff.
4. Add look steering and bounded client prediction.
5. Implement swept collision and safe impact positioning.
6. Add Lightning damage, PvP filtering, and knockback.
7. Integrate the actual Ascension helper.
8. Add interpolated electricity trail.
9. Add front cluster, connecting arcs, and impact presentation.
10. Add modular sound events and moving loop.
11. Complete observer tracking, late tracking, and packet validation.
12. Add landing protection, recovery, and all abort paths.
13. Profile simultaneous casts and tune particle/network budgets.
14. Balance damage, mana, cooldown, animation, and presentation.

Networking starts with the movement prototype; it cannot be postponed until after single-player polish.

## 13. Testing and acceptance criteria

### Movement and collision

Test single-player and a dedicated Forge 47.4.10 server with another observing client:

- Horizontal, upward, downward, and diagonal flight.
- Rapid turns and 180-degree camera reversals.
- Walls, ground, ceilings, corners, slabs, fences, thin walls, and entities.
- Immediate launch obstruction.
- Timeout while airborne.
- Stationary and moving Valkyrien Skies ships.
- 100–250 ms latency and reduced server TPS.

Acceptance: no tunneling, repeated rubber-banding, flying kicks, movement ownership leaks, or duplicate impacts. Observers see the caster, aura, trail, and discharge in the same location.

### Damage and lifecycle

Test:

- Level progression and Lightning power bonuses.
- Lightning resistance and cancellable spell-damage events.
- PvP enabled/disabled, teams, summons, and caster exclusion.
- Unchanged terrain.
- Death, logout, dimension changes, teleports, mounting, fluids, Elytra, and Creative.
- Repeated casting and simultaneous charges.
- Orderly shutdown and recovery from a saved active-state marker.

Acceptance: one damage pass per impact; timeout and abort produce no damage; all temporary state is removed.

### Visuals and audio

Compare against Ascension with shaders on/off and each particle-quality setting.

Acceptance:

- Recognizable Ascension electricity surrounds the moving player.
- No obvious trail gaps at supported speed.
- Corrections do not draw long stray trails.
- Impact clearly exceeds flight intensity.
- Positional sounds follow the caster and impact correctly.
- Flight audio stops within one client tick of processing termination.
- Resource reload, tracking loss, and disconnect leave no looping sound.

### Performance and regression

Test 1, 4, and 8 simultaneous casters. Start with approximately 20–25 regular particle emissions per caster per tick, occasional arcs, and a bounded impact burst. Enforce per-caster and global client budgets, reducing distant effects first.

Measure collision time, packet traffic, frame time, and server tick time. Target under 2 ms additional aggregate server work for eight casters in the test scene, then adjust query and emission budgets if necessary.

Rerun Grand Explosion casting, damage, sound, staff/armor, and network checks. The protocol update requires matching addon versions on server and clients.

## 14. Risks and directional fallback

The inspected APIs establish feasibility, but a prototype must resolve:

- Prediction quality under latency and low TPS.
- Interaction between scoped movement hooks and Valkyrien Skies.
- Moving-ship contact accuracy.
- Whether repeated Ascension particles form a sufficiently dense body aura at cruise speed.
- Shader visibility and bolt overdraw.
- Encoded audio-loop seams and mix balance.
- Conflicts with other mods that independently control player movement.

These are validation gates, not reasons to start with the simpler design.

If guided flight remains unreliable after the dedicated-server prototype, set a configurable `guidedFlight=false` fallback:

- Capture look direction once at launch.
- Keep that direction fixed.
- Default to 0.85 blocks/tick with a 40-block travel cap.
- Retain authoritative movement, swept collision, and cleanup.
- Keep the same Ascension aura, continuous trail, front cluster, custom audio, and impact attack.
- End safely without damage when the travel cap is reached.

The fallback changes steering and duration behavior; it retains the electrical-projectile presentation and collision reliability.

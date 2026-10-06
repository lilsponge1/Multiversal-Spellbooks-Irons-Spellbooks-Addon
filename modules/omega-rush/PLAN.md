# Omega Rush — standalone Iron’s Spells add-on

## Accepted visual revision — October 5, 2026

The player uses a horizontal Superman-style flight posture, with both arms forward and straight legs trailing behind. Explosion A, B, and C are separate left-to-right animation rows and must all appear intermixed. Replace the narrow visual string with a wide cosmetic cone: full quality adds six bursts per node, spread approximately 1–3 blocks behind it, up to three blocks laterally, and several blocks above the path. The cone follows each curved path segment. Steering, sound, duration, mana, cooldown, and the established damage rules remain unchanged.

## Summary

Create a **Legendary Nature spell** that gathers rainbow energy, launches the player into controllable flight for seven seconds, and leaves a cascading wall of rainbow explosions behind them.

**Flight uses Thundercrash’s camera-based steering and smoothing.** Omega Rush retains its own speed, duration, charge-up, visuals, damage, and termination behavior.

Build independently for **Minecraft 1.20.1, Forge 47.4.10, and Iron’s Spells ’n Spellbooks 3.16.3**. Do not modify Thundercrash or Multiversal Spell, install into the live pack, or push to GitHub during this work.

## Spell behavior and defaults

| Setting | Initial default |
|---|---|
| Name | Omega Rush |
| School / rarity | Nature / Legendary |
| Maximum spell level | 5 |
| Mana cost / cooldown | 200 mana / 120 seconds |
| Charge duration | 0.8 seconds |
| Flight duration | 7 seconds; configurable from 5–10 |
| Flight speed | 16 blocks per second |
| Steering | Camera-controlled; Thundercrash’s 0.18 responsiveness |
| Trail node spacing | Every 2 blocks traveled |
| Node detonation delay | 0.25 seconds |
| Damage radius | 4.05 blocks per node (approved wider-trail revision) |
| Base damage | 12 damage points, plus 3 per spell level above I |
| Terrain destruction | None |

- **Charge-up:** Use Iron’s normal cast lifecycle. Rainbow motes spiral inward toward the torso, an outer ring contracts, and the center brightens. Finish with a brief white pulse edged in rainbow colors. Scale the animation to Iron’s effective cast duration. Interrupted charging produces no flight or trail.
- **Launch and aiming:** Launch along the player’s current look direction. During flight, looking left, right, upward, or downward smoothly redirects movement. Releasing the cast control does not stop flight.
- **Steering response:** Match Thundercrash’s existing flight calculation: each tick blends 82% of the previous velocity with 18% of the desired velocity toward the camera direction. Use Omega Rush’s speed of 0.8 blocks per tick, capped at that speed. Preserve the natural temporary slowdown during sharp turns rather than normalizing it away. Do not inherit Thundercrash’s upward launch phase or distance cap.
- **Termination:** End at the duration limit, terrain obstruction, or deliberate sneak cancellation. Use continuous collision checks to prevent passing through walls. Creatures do not trigger an impact attack; touching them causes no direct spell damage.
- **Cleanup:** Restore normal movement immediately. Protect the first landing from fall damage for up to five seconds. Death, logout, dimension changes, external teleports, fluids, and unsafe world boundaries cancel flight and clear outstanding spell state.

## Implementation and presentation

- **Standalone interfaces:** Create a separate `omega-rush` project with mod ID `irons_omega_rush` and spell ID `irons_omega_rush:omega_rush`. Register the spell and server/client settings through Iron’s and Forge’s existing systems. Thundercrash remains a read-only handling reference, with no runtime dependency.
- **Movement synchronization:** The server owns movement, collision, and termination. Send camera aim during active flight; use the same movement arithmetic for bounded client prediction and server reconciliation. Send state every two server ticks and immediately on phase changes. Validate input against the sender’s active flight session and reject stale or invalid updates. Without a fresh aim update, retain the last accepted direction.
- **Curved explosion trail:** Sample the actual traveled path every two blocks, including through turns, ascents, and descents. Carry unused spacing between ticks. Never draw a straight trail between launch and destination. Each node detonates five server ticks after placement, damages once, and animates for approximately 0.6 seconds.
- **Damage:** Apply Iron’s Nature spell-power scaling once. Exclude the caster and respect friendly-fire, protection, and server PvP rules. Limit each target to one successful hit every ten ticks and three successful hits per cast. Preserve vanilla damage immunity.
- **Trail lifecycle:** Existing nodes finish after timeout, deliberate cancellation, or ordinary terrain contact. Clear pending nodes after death, disconnect, dimension change, or external teleport. Do not force-load chunks.
- **Reference assets:** Extract Explosion B/C frames from the supplied sprite sheet for bright cores that expand into thinning rings; occasionally use Explosion A for larger accents. Preserve transparency and exclude labels and borders. Account for the video’s half-speed playback when tuning animation.
- **Rainbow effects:** Combine white cores, cyan/magenta/lime/yellow/violet rings, outward sparks, and short ribbons. After flight and explosions work, add a rainbow aura and optional translucent colored player overlay. Full skin recoloring is outside this release.
- **Client settings and audio:** Provide full/reduced/minimal particle settings plus overlay and bright-flash toggles. Reduced settings retain readable rings. Render cosmetic effects locally from compact trail events with distance culling and particle limits. Use existing Minecraft sounds, limit overlapping bursts, and stop flight audio on every termination path.

## Crafting and delivery sequence

1. Register the spell, configuration, and normal mana/cooldown behavior.
2. Implement charge-up, guided flight, prediction, collision, cancellation, and cleanup.
3. Implement curved trail placement, delayed damage, hit limits, and multiplayer synchronization.
4. Add sprite animations, rainbow presentation, audio, and client quality controls.
5. **As the final feature stage, enable native Scroll Forge crafting:** Nature school, Poisonous Potato focus, Paper, and the appropriate Legendary Ink. Use Iron’s standard scroll item and level rules.
6. Verify normal Inscription Table handling and casting from ordinary spellbooks.
7. Deliver the standalone release JAR, installation instructions, configuration reference, checksum, and test report. Integration into the other mods remains a separate follow-up after completion.

## Tests and acceptance criteria

- Charging, interruption, mana deduction, and cooldown follow Iron’s normal lifecycle.
- Flight lasts 140 server ticks by default and cruises at 16 blocks per second.
- Camera steering matches Thundercrash’s smoothing at Omega Rush’s speed, including gentle adjustments, sharp turns, upward/downward aiming, and yaw wraparound. Client and server calculations agree.
- Trail spacing stays consistent through curves and climbs; sharp turns produce no missing segments or extra damage from overlapping nodes.
- Walls, ceilings, fluids, unloaded chunks, and world boundaries stop movement safely. Contact produces no impact blast.
- Nodes detonate after five ticks, leave terrain intact, spare the caster, and enforce per-target hit limits.
- Every termination path leaves no stuck movement, sound, overlay, or spell state.
- Dedicated-server startup succeeds without client rendering classes. Multiplayer tests cover latency, corrections, tracking changes, and simultaneous casters.
- Visual review covers first/third person, shaders enabled/disabled, and all particle settings; forward visibility remains usable.
- Scroll Forge selection, ingredient consumption, output spell/level, and inscription pass in the installed Iron’s version.
- Coexistence checks with Thundercrash and Multiversal Spell require no edits to either project and prevent two movement spells from controlling the same player.
- Completion requires a working standalone build, automated behavior checks, multiplayer checks, and an in-game visual review. Record unverified scenarios explicitly.

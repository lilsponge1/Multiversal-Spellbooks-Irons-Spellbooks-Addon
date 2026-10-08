# Super Saiyan III

The 0.3.5 review candidate extends the existing Saiyan Ascension spell and craftable Curios Head wig. The accepted wig12 release is preserved. It does not add another spell, replace armor equipment or modify the installed Iron's Spells JAR.

## Controls and sequence

Use the existing selected-spell or quick-cast key. Base → SSJ1 → SSJ2 → SSJ3 uses the same short taps. A hold of three seconds returns to Base, including during SSJ3 charging. Intermediate holds cancel that input. SSJ1 and SSJ2 retain their previous charge durations, activation costs and 10/17 mana-per-second drain.

SSJ3 takes **15 seconds (300 server ticks)**, following the latest request instead of the attachment's earlier five-second duration. SSJ2 bonuses, regeneration penalty and upkeep remain in effect during this charge. SSJ3's bonuses and 30 mana-per-second drain begin at completion. Its configurable activation fee defaults to zero and is paid only at completion.

| Time at normal server speed | Sequence |
| --- | --- |
| 0–4.1 seconds | Charging pose, spoken line from the supplied clip, swirling gold particles and an initial ground wave |
| 4.1–6 seconds | Aura enlarges; dust, electrical arcs and camera shake intensify |
| 6–9 seconds | Repeated waves and a faint energy shell build during the sustained yell |
| 9–12 seconds | Long golden hair appears and lengthens; a tall energy pillar rises |
| 12–15 seconds | Climax, full hair, final flash and expanding shockwave |
| 15–25 seconds | Completed SSJ3 aura remains; a vocal-separated instrumental excerpt from the supplied final clip plays for ten seconds |

Server lag extends the wall-clock charge because gameplay uses server ticks. Sounds follow the caster. A late observer hears the yell rather than replaying the introductory sentence. An extended configured charge continues with the yell loop. Power-down, reset, tracking loss, death, resource reload and world changes stop the associated effects and audio.

## Final SSJ3 bonuses

These replace SSJ2's bonuses; they do not stack with them.

| Stat | SSJ3 |
| --- | ---: |
| All spell power | +100% |
| Melee attack damage | +100% |
| Attack speed | +50% |
| Movement speed | +40% |
| Incoming damage reduction | 35% |
| Knockback resistance | +1.00, capped by the native attribute |
| Critical damage | +50% where supported |
| Maximum health | +40% |
| Jump velocity | +35% |
| Natural mana regeneration | 25% of normal, further capped when necessary |
| Mana upkeep | 30 per second, paid as 1.5 each server tick |

The installed pack provides Apothic Attributes' shared `attributeslib:crit_damage`; this affects supported spell hits **and melee criticals**. A spell-specific critical attribute takes priority if present. No critical bonus is applied if neither exists. Jump strength uses Forge's jump event and a server-synchronized value on the controlling client.

Damage reduction runs once and respects native resistance-bypass damage tags. Armor and other mods continue using their own systems. Maximum-health modifiers do not grant free healing; ending SSJ3 clamps current health to the restored maximum.

Zero or insufficient mana cancels SSJ3 directly to Base. It never downgrades to another transformed form. With seven mana per second before transformation, ordinary regeneration becomes 1.75 and the net loss is 28.25 per second. Survival time depends on mana remaining **after** the charge, equipment, spells and potions; there is no fixed mana-pool assumption.

To prevent unusually high natural regeneration from making SSJ3 indefinite, native regeneration can offset at most 50% of upkeep by default. This cap recomputes when relevant attributes change, preserving equipment improvements below the cap. Instant mana restoration remains effective. Existing SSJ1/SSJ2 burnout behavior remains unchanged.

## Hair, aura and combat effects

The wig renders a cached custom 3D hairstyle with a swept-back crown and sixteen overlapping mane locks. All roots overlap the crown and remain fixed to its animated head transform, including standard-helmet offsets. Head rotation blends smoothly into the lower torso-following locks, so the upper mane turns with the head while the ends sway along the back. Collision clearance follows the head's own orientation and tests whole nearby surfaces instead of pushing roots behind the torso. The mane stays connected while looking straight up/down and turning at those angles. Temporary forehead-coloured brow strips avoid editing the skin; unusual skin layouts and modded helmets still require review.

SSJ3 has a larger wavy, flaring gold aura, a pale inner core, frequent blue-white branching arcs, swirling/upward sparks, charge dust, rings, a late pillar and a final flash. The normal aura is about 6.15 blocks high; the late charging pillar rises toward nine blocks. Camera-facing sheets remain faint so the body is visible. It uses the established native particle shader/render stage for Solas. Framebuffer refraction and dynamic world lighting are not added; the compatible glow and traveling waves provide the shimmer.

Melee hits create small cosmetic gold/electric bursts. Accepted spell casts pulse around the caster without altering original spell colours. Sprinting leaves a short gold trail; jumps and landings create sparks. These add no damage. Server cosmetic packets are limited to one per four ticks per caster. Particles share the existing global budget; aura details reduce beyond 32 blocks, stop beyond 64 and render at most sixteen players per pass.

## Configuration

Server settings are under `[saiyanAscension.ssj3]` in the existing server TOML: all combat bonuses, activation fee, charge ticks, upkeep, remaining regeneration, maximum-health bonus, critical bonus, jump strength and maximum native-regeneration offset. Existing shared settings control sound volume, camera-shake range and hold thresholds. Restart after changing balance values for consistent active-session behavior.

Client `[saiyanAscension.ssj3]` exposes aura intensity, particle multiplier, hair sway, cosmetic combat/movement effects and completion music. Shared particle budget, density, shake and HUD settings still apply. Sound assets can be replaced through resource packs.

The fifteen-second charge voice was separated locally from `Recording 2026-10-07 153943 (ssj3).mp4`. The source's leading silence and pause between the spoken line and yell are removed: the line begins at activation and the sustained Goku yell fills the rest of the charge at natural pitch. Short crossfades join the voice excerpts. SSJ3 uses only this voice sequence during charging; the separate electrical charge loop and older yell are excluded. The music uses seconds 18–28 of `Recording 2026-10-07 154142 (ssj3 final).mp4`. Its vocals were separated locally to remove the unwanted yell after the final flash. The additional lightning crack at SSJ3 completion is removed; the gold flash retains its explosion sound. All three encoded assets have decoded duration and peak checks. Audio is not uploaded and no voice-model/runtime dependency is included in the mod. Excerpts and hashes are recorded in `art/saiyan/ssj3-audio-source.json`.

## Build and verification

The revised deliverable is `build/release/multiversal-spellbooks-0.3.5-saiyan-ssj3-hair4.jar`. Server and clients require this same protocol-5 build. It replaces the hair mesh and renderer and adds one deformation helper. The other 177 entries from hair3 remain byte-identical, including all audio, aura, combat, recipes and controls. Prior releases are preserved, and test helpers are excluded. The main server and normal client mod pack are unchanged.

The build passed **40 native client checks, 0 failed**, using Solas V3.7b and the cached Prism mods in a hidden client on the same disposable server/world. Thirteen captures cover charge, front/side/rear, head turning, straight up/down, combined turning/pitching and helmets. The agent reviewed all captures, including the previously missed overhead view. The hidden client closed and the server saved all dimensions. Saved client options were restored byte-for-byte.

The actual Java deformation was checked at **33,965,568 surface samples**, including corners and triangle interiors, over head yaw/pitch through 90 degrees, partial charge reveal, maximum sway and standard helmets. All 746,496 checked root anchors coincide with the crown transform within 0.000004 model pixels. No sampled head/body collisions were found. Older-form geometry remains bit-identical. The mesh adds no faces; the deformation reuses scratch vectors per render rather than allocating per vertex.

The CPU deformation benchmark covers only one mane's math, excluding crown rendering, draw submission, GPU cost and whole-game FPS. Results are recorded in the audit, including timing outliers while the full pack was loading. User hair/audio acceptance and extended multiplayer FPS/GPU measurements remain pending.

Historical server regression on the initial SSJ3 artifact: **410 passed, 0 failed**. Its server combat/config/network/recipe entries remain byte-identical in this revision; those 410 checks were not rerun against this package. That historical run required terminating an unchanged Ballistix worker after saved-world shutdown, so its JVM exit is not reported as clean.

Current SHA-256: `8164908EA961477EEE436BB9E6299F0A58FF55020187A0149574CD0FB36BE7BD`. See `docs/SSJ3-VALIDATION.json` and this artifact's `.ssj3-validation.json` sidecar.

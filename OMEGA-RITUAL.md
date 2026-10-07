# Omega Form transformation ritual — 0.2.3 preview

This local revision extends Form's casting sequence inside the single combined Multiversal Spellbooks JAR. The published 0.2.2 release is unchanged.

## Sequence

The default charge is **6.4 seconds**, 20% shorter than the initial eight-second revision. The player physically rises **3.5 blocks** during its opening fifth, with one thigh lifted and the knee folded, while the arms hold the power-up pose. Gravity and movement ownership belong to the charge controller until success or interruption; walls, ceilings and the existing collision box still apply.

After the rise, six native Minecraft flowers appear **one at a time**: poppy, orange tulip, dandelion, blue orchid, cornflower and allium. They orbit in a tall, tilted ring inspired by the supplied GIF. Each arrival plays the supplied `snd_enemy_appear_quick.wav`, converted to mono 44.1 kHz Ogg Vorbis, with pitches **1.0, 1.18, 1.36, 1.54, 1.72 and 1.9**. At normal TPS, arrivals are separated by six or seven ticks (about 0.32 seconds). The car-drive clip plays once at charge start and does not loop or restart for observers arriving halfway through. These are rendered item models, without entities, drops, hitboxes or gameplay loot.

Once all six are present, the ring continues briefly, then spirals into the player's body. Successful activation emits a bright world pulse and a short screen flash for nearby clients, including the caster. Three colored follow-up pulses occur four, eight and twelve ticks later, with a short rainbow halo. The existing bright-flash preference disables the white screen flash while retaining colored effects.

The 200-mana activation payment remains at success, followed by the existing 30-mana/second upkeep. Interruptions, equipment removal, teleportation or invalid movement clean up the charge and restore original gravity without a Form mana payment. Once the transformation completes, the existing double-tap flight toggle remains off until chosen by the player; landing protection covers the return from the ritual's rise. Form buffs, Rush's 10-second transformed cooldown, and its guided movement remain unchanged.

## Settings and compatibility

- `omegaForm.chargeTicks`: base default 160, range 160–400, scaled by 0.8 for the requested overall speed increase: 160 produces 128 effective ticks (6.4 seconds). Older short charges migrate to the longer sequence. Native casting-speed gear can reduce the effective duration, with a 96-tick minimum (4.8 seconds). All phase times use the same effective clock.
- `omegaForm.chargeLiftBlocks`: default 3.5, configurable from 0 to 8; obstructing ceilings reduce the actual rise.
- Omega network protocol **4** carries the charge origin/height and nearby arrival/absorption events. Server and clients need the same preview build.
- Existing sound volume, particle quality and bright-flash preferences still apply. All six native flower models remain visible across quality modes.

## Current verification

The final combined `omega-ritual-e.jar` passed **165 Form assertions and 114 Rush/compatibility assertions**, for **279 server checks**. Added checks exercise rise before first arrival, fixed position during charge, all six arrival ticks, pitch ordering, orbit motion, complete absorption, exact activation cost, manual-toggle restoration, safe landing, cancellation, teleport, original gravity, low ceiling, native flower registries, sound registration and event round trips.

The archive audit verifies 245 preserved module-input payload entries, all four mod IDs, matching Mixin classes, three mono Vorbis sounds, HUD icons and exclusion of development code. The supplied WAV is unchanged; its converted clip decodes to 86,080 mono frames at 44.1 kHz (1.952 seconds).

A separate real Minecraft test client verified the bent-knee pose, physical rise, six flower models, orbit, inward absorption, world glow and follow-up pulses. The same client was exercised as a caster and as a nearby observer of a synthetic test actor. Its sound engine started exactly one non-looping car source and six flower sources per sequence, at the expected pitches. Both roles received the screen-flash callback, and the foreground white flash was inspected in the first-person observer view. The real caster's charge completed after approximately 6.4 seconds and returned to ground at the original X/Z position. Server `allow-flight=false` stayed enabled without a floating kick. The temporary desktop test window and fixtures are separate from production profiles.

Logs: local `build/ritual-final-server-validation.log` and `build/ritual-final-client-review.log`. These checks cover the requested sequence with shaders disabled; they do not certify arbitrary shaders or stress with several simultaneous real clients.

No GitHub publication of 0.2.3 has been requested or performed.

Local combined preview: `build/release/multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.3.jar`. SHA-256: `CBCD5F55765E2AB60E95C13C852AE23ECDAAFA2BB0F28980F50198A71E188354`.

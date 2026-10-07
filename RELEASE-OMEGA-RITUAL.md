# Multiversal Spellbooks — Omega Form Ritual 0.2.4

Omega Form now has a full transformation sequence, inside the **single combined Multiversal Spellbooks JAR**. Grand Explosion/Thundercrash 0.3.3, Crimson Susanoo 0.1.1 and Ignis Armor Compatibility 0.1.0 remain included.

## Transformation changes

- A **6.4-second default charge** lifts the player 3.5 blocks, with one leg raised and its knee bent. Ceilings and normal collisions still apply.
- Six different Minecraft flowers appear one by one, orbit in a tilted ring, then spiral into the player. Arrivals are roughly 0.32 seconds apart at normal TPS.
- Each flower plays the supplied appearance sound, with pitches rising from 1.0 to 1.9 in steps of 0.18.
- The car-drive sound plays once at the start of Form's charge.
- Successful absorption produces a nearby-player screen flash, world halo and three colored follow-up pulses. The supplied **fire-spell sound** plays once with this finish, audible to the caster and nearby players.
- Existing bright-flash and sound-volume preferences still apply. The flower and completion recordings are mono Ogg Vorbis with 32-block spatial attenuation.

Form retains its 200-mana activation, 30 mana/second upkeep, buffs, free dismissal and double-tap hover controls. Rush retains its 200-mana cost, 10-second transformed cooldown, guided flight, broad explosion trail and recipe. Scarf acquisition and equipment rules remain unchanged.

## Install

1. Stop Minecraft and the server, and back up the world and configs.
2. Replace the previous combined addon with `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.4.jar` on **both the server and every client**.
3. Remove separate Grand Explosion, Crimson Susanoo, Ignis Armor Compatibility and Omega Rush JARs; they are already included.
4. Keep normal dependencies. The package targets Minecraft 1.20.1 / Forge, with validation on Forge 47.4.10, Iron's Spells 3.16.3 and Curios 5.14.1. Existing Travel Optics 6.3.0+, Cataclysm: Spellbooks 1.2.9 (below 1.3), Cataclysm/GeckoLib/animation dependencies remain required.

**Omega protocol 4** replaces protocol 3; update server and clients together. No new standalone mod is needed. The longer ritual replaces older short Form charge settings automatically. `omegaForm.chargeTicks=160` is the base clock and runs at 80% of that value: 128 effective ticks, or 6.4 seconds. Casting-speed gear can reduce the sequence to a 96-tick minimum. `chargeLiftBlocks=3.5` controls its rise.

Use the Flowery Scarf in a functional charm slot to cast Form. Omega Rush's Scroll Forge recipe remains **Legendary Ink + Paper + Poisonous Potato**. See `INSTALL-COMBINED.txt` and `OMEGA-RITUAL.md` for further details.

## Verification

The preceding 0.2.3 ritual build passed **279 server assertions**, plus real-client review as the caster and a nearby observer. The review covered physical rise, pose, six flower arrivals, sound pitches, one-shot car audio, convergence, finish effects, safe landing and server `allow-flight=false`.

The 0.2.4 fire-spell cue addition passed compilation, audio decoding and archive checks. All three prior recordings and other gameplay classes are unchanged. At the user's request, this final sound addition was left for their live review; the 279 gameplay checks were not rerun for it. Arbitrary shaders and multiple simultaneous real-client transformations are outside the recorded review.

SHA-256: `95602ECB3368691F1952DABBEE912A32C79D57DF4E3ED58130507C2025EDB878`

Install only the combined JAR. The checksum, installation text and verification reports are supporting files.

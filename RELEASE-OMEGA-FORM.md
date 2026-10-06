# Multiversal Spellbooks — Omega Form 0.2.2

This release adds Omega Form and the Flowery Scarf to the **single combined Multiversal Spellbooks JAR**, alongside the existing Thundercrash, Crimson Susanoo, and Ignis armor features.

## What's new

- Equip the rare Flowery Scarf in a functional **charm slot** to unlock Omega Form I in Iron's spell wheel. The scarf has a visible neck-and-shoulder model and cannot be crafted or stripped of its bound spell.
- Omega Form charges for 1.5 seconds, costs 200 mana to activate, and drains 30 mana per second. It grants Resistance III equivalent, eight absorption hearts once per transformation, Strength I equivalent, and +25% Nature spell power.
- Double-tap Space to toggle Form flight. Single jumps stay normal; hold Space to rise, sneak to descend, and use movement keys horizontally. Flight uses Rush's arms-forward posture while retaining hover controls. Turning flight off keeps Form buffs and upkeep.
- Rainbow body and hand effects, aura, and fading player after-images accompany the transformation. Dedicated flower and prismatic explosion icons now appear on the HUD and spell wheel.
- Omega Rush requires active Form by default and has a **10-second base cooldown during Form**, with normal gear reductions. Its 200-mana cost, steering, seven-second flight, broad explosion trail, sound timing, and Scroll Forge recipe remain intact.
- The scarf has a 5% drop chance from eligible player-defeated Iron's and non-fire Cataclysm bosses, and a 0.2% chance in generated exploration chest loot. Server configuration exposes drop rates, mana, buffs, flight speeds, prerequisite, and cooldown settings.

## Install

1. Stop Minecraft and the server, and back up your world and configs.
2. Replace the older combined addon with `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.2.jar` on **both the server and every client**.
3. Remove separate copies of Grand Explosion, Crimson Susanoo, Ignis Armor Compatibility, or Omega Rush. These modules are already included in this one JAR.
4. Keep the normal modpack dependencies. This release targets Minecraft 1.20.1 / Forge, tested with Forge 47.4.10, Iron's Spells 3.16.3 and Curios 5.14.1. Existing Travel Optics 6.3.0+, Cataclysm: Spellbooks 1.2.9 (below 1.3), Iron's/Cataclysm and animation dependencies remain required.

Existing registry IDs and configuration values are retained. The new setting `omegaRush.omegaFormCooldownSeconds = 10` is added automatically. Worlds upgrading from the earliest Rush build should set `damageRadius = 4.05` to use the wider damage area. Network protocol 3 requires the matching combined addon on server and clients.

The Flowery Scarf is `irons_omega_rush:flowery_scarf`. Omega Rush remains craftable at the Scroll Forge with **Legendary Ink + Paper + Poisonous Potato**. Recast Form to dismiss it for free; its 120-second cooldown starts when Form ends.

## Verification

The exact release JAR passed **249 server assertions**: 135 Form checks and 114 Rush/combined compatibility checks. Its archive audit confirms 240 preserved input payload entries, all four mod IDs, sound resources, correct HUD icons, and exclusion of test code. The preceding unchanged packager passed seven unit tests.

The user accepted the preceding Form, icons and flight-toggle revision in game. The new Form flying pose reuses the accepted Rush animation; its application during hover has not received a separate desktop visual check. Broader shader/body/latency and multiple-client Form presentation remain outside these server checks.

SHA-256: `08ACF74C03472305CCD5E7292F19769FD6E6BB6907925F4821F619DEFE225805`

Only the combined JAR belongs in the mods folder. The checksum, installation text, and verification reports are supporting files.

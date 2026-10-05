# Multiversal Spellbooks 0.3.3 combined update

Review build from `codex/latest-addon-updates`: Grand Explosion 0.3.3 + Crimson Susanoo 0.1.1 + Ignis Armor Compatibility 0.1.0.

## Changes

- New Thundercrash spell icon and synchronized charge, looping flight and actual-impact audio.
- Refined Cuirass of the Thunderstar with one spell slot, Riptide material stats and a unique 20% Thundercrash impact perk. Its optional recipe example remains inactive.
- Susanoo remote walking synchronization, smooth pose/keyframe interpolation, right-hand katana with corrected blade roll and grip, half direct damage by default, friendly player/pet/summon protection and second-cast recall at no extra mana cost.
- Ignis Mage enchanting tables/anvils support with normal XP/lapis costs and compatibility rules.
- Separate source modules, one combined JAR, updated build/packaging tools and validation documentation.

## Validation

The exact release JAR passed full-pack Forge 47.4.10 startup and 278 guardian diagnostics with zero failures. The owner confirmed live second-cast recall and cooldown. Earlier unchanged armor payloads passed 63 Thunderstar checks and 499 Ignis enchanting checks. Player-driven Thundercrash comparison measured 40.00 vs 55.44 damage, matching the normal attribute bonuses plus one 20% perk. Numerical animation/footing/blade checks, asset checks, audio decoding/lifecycle checks and packaging tests also passed.

Two-human-client visual synchronization and production latency/performance remain checks for the server owners. See `INTEGRATION.md` for detailed evidence and limits.

## Installation

Install the same attached combined JAR on the stopped server and every matching client. Remove older combined/separate addon copies and retain existing dependencies. This update requires Travel Optics 6.3.0+ and Cataclysm: Spellbooks 1.2.9 (below 1.3); tested with Minecraft 1.20.1, Forge 47.4.10 and Iron's Spells 3.16.3. Existing IDs/configs remain valid. Keep Susanoo `friendlyFire=false` and omit diagnostic flags in normal play. Source is on the update branch for review; publication does not merge into main or install on a production server.

Artifact: `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-thunderstar-recall3.jar`

SHA-256: `D74BFA26C913D2E3C540D81ED8A468D7F8A32817A169D9516543E275DCCBE4F0`

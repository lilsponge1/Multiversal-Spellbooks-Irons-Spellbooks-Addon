# Grand Explosion addon plan

## Verified pack target

- Minecraft 1.20.1, Forge 47.4.10, Iron's Spells 'n Spellbooks 3.16.3.
- The latest `Modded Server Current.zip` contains the same Iron's JAR, Oculus 1.8.0, and Embeddium 0.3.31. The earlier client export contains Solas Shader V3.7b.
- Ship one standalone addon JAR to both server and clients. Do not edit Iron's JAR. Keep the disposable test harness out of the release.

## Grand Explosion

- Register `irons_ultimate_explosion:grand_explosion` as a five-level Legendary Fire spell. Admins can distribute it in spellbooks, and the Cinderstar Staff comes imbued with level V. Keep the spell out of random loot, crafting, scrolls, and other weapons.
- Use a six-second cast, with a minimum 80 ticks after cast-speed bonuses, a 120-second base cooldown, and a server raycast up to 48 blocks.
- Require 80 mana to start. At detonation, set the Iron's `SpellOnCastEvent` cost to the current mana. Base the bonus on the amount actually deducted. A survival cast that spends none fizzles. Cap the mana contribution at 1,600 and center damage at 160.
- Damage living targets within 60 blocks through Iron's damage source, with a 512-target cap and bounded knockback. PvP defaults on for other players; the caster is always excluded.
- Lock the impact point when the cast starts. Draw neon magenta, green, cyan, and red energy inward around a shrinking charge ring. At detonation, call the installed Ballistix mod's nuclear particle presentation on the client for its native shockwave and mushroom cloud. Send dimension-aware charge, detonation, and cancellation packets within a 128-block default visual radius. Keep the addon in control of spell damage; use Ballistix's terrain raycast only for the crater.
- Apply four seconds of Slowness II after detonation by default; the server setting can disable it.
- At the locked target and near the caster, play accelerating sparks, adding lightning cracks at the end. At detonation, use Ballistix's nuclear explosion sound, volume, pitch, and nearby-player reinforcement; use Alex's Caves and vanilla sounds only as the fallback. With Traveloptics installed, show its brief white screen flash. Keep the mod integrations optional and leave spell damage with this addon.

## Cinderstar Regalia

- Four original voxel armor pieces: wide witch hat, cream-shouldered crimson robe with a dark front panel and gold trim, asymmetric light and dark leggings, and bright orange boots. The hat leaves the wearer's face visible; no armor piece adds hair or changes the face.
- Use Iron's Pyromancer material and caster armor attributes. Smith each piece from its Pyromancer counterpart with Cinder Essence and a Nether Star.
- Only a complete four-piece set boosts Grand Explosion: double its calculated center damage, capped at 320, and extend entity damage radius by one third (60 to 80 blocks). Terrain damage radius is unchanged.

## Cinderstar Staff

- Register `irons_ultimate_explosion:cinderstar_staff` as an Iron's `StaffItem` that automatically carries Grand Explosion V. Casting from this staff uses mana and cooldown; the same spell also works from an equipped spellbook.
- Main hand only: **+30% Fire Spell Power** and **+10% Cooldown Reduction**, both `MULTIPLY_BASE`. Iron's soft cap determines the final cooldown. The staff boosts all Fire spells, including Grand Explosion, without bypassing that spell's mana and damage limits.
- Use an original voxel model inspired by the reference's long shaft and red focal point: charred grip, pale shaft with gold bands, asymmetric blackened-bronze prongs, and an ember-red faceted crystal. Keep the geometry connected and use a more upright first- and third-person held pose. Use the standard item renderer; no custom shader or copied anime asset.
- Craft it as a late-game upgrade from a Pyrium Staff, two Fire Runes, Cinder Essence, and a Nether Star. Add the Iron's `staff` item tag, language, tooltip text, and Better Combat attributes.

## Terrain and safety

- Default terrain changes **on**, still requiring Iron's `spellGriefing` switch. Existing world configs must opt in explicitly if they saved the older `terrainDamage=false` value.
- With Ballistix installed, use its nuclear raycast with the pack's configured size (45) and energy (120), its 2-tick block-processing pace, block explosion hooks, occasional surface fire, and block-break debris. Keep its radiation source and irradiated-block pass out. Without Ballistix, use the configurable downward-bowl fallback.
- Server settings bound damage multiplier, PvP, visual radius, crater radius, terrain, and post-cast exhaustion (15 seconds by default). The client setting controls particle quality.

## Validation

1. Build the release JAR from the exact local dependency set and verify registration on a copied Forge server.
2. In a disposable world, check mana drain, low-mana fizzle, entity damage, recipe and tag loading, main-hand staff modifiers, terrain-off behavior, protected blocks, block entities, and unbreakable blocks.
3. In the GPT Prism copy, inspect inventory, first-person, and third-person staff placement; cast from the staff with the spellbook unequipped; inspect the charge ring at the locked impact point, detonation, and mushroom cloud aftermath with Solas V3.7b and shaders off; compare particle density and frame time at several distances.
4. In multiplayer, test player-to-player damage with two real clients and caster immunity, creative mode, cast interruption, unusually high mana, claim protection, chunk edges, concurrent casts, and server tick time before enabling terrain on the primary server.

The copied server checks in steps 1 and 2 pass, including the revised 24-block crater: 29,919 candidates completed, target stone was removed, and a chest and bedrock were preserved. A canceled block-break event also preserved protected stone. The server log showed no tick-overrun warning during the large crater job. The GPT client confirmed a full-mana spellbook cast and a mana-consuming cast from the imbued staff with Solas enabled. The more upright pose was inspected in first and third person. Live GPT casts displayed the multicolored charge particles and Ballistix's large orange blast and persistent mushroom cloud with Solas enabled and disabled. The shaders-off rerun showed the orange-red blast and gray cloud again, and Solas was restored afterward. Full-set armor behavior passed a copied-server harness: the center damage calculation doubled, entities beyond the base radius were hit only with the set, and the caster remained safe. The revised hair-free outfit and all four armor pieces rendered on the GPT client, from both sides, with the face visible. The multiplayer client joined after matching the copied server's Warium and Copycats variants, and stayed connected through a regalia detonation test. A copied-server PvP harness using ordinary ServerPlayer targets passed after clearing their normal spawn-protection period: other-player damage on, caster safe, and other-player damage off when configured. The previous FakePlayer result was invalid because Forge FakePlayer is always invulnerable. Pressing E in the creative-mode GPT copy caused Fetzi's Displays to submit an invalid stack while building its creative tab, so inventory inspection there did not complete.

## Thundercrash implementation

Version 0.3.0 extends this addon with `irons_ultimate_explosion:thundercrash`, a five-level Legendary Lightning spell enabled for Iron's Scroll Forge crafting. It uses a brief charge, upward launch, server-authoritative guided flight, swept full-body collision, a Lightning damage pass without terrain destruction, and one cleanup path. A fixed-direction, distance-limited fallback is configurable.

The implementation reuses AscensionEffect.ambientParticles and Iron's electricity, zap, and blastwave particles. It adds bounded client prediction, tracking snapshots, an interpolated trail, a forward energy cluster, an original flight posture, and a moving sound loop. Networking uses protocol 3 while preserving Grand Explosion packet ID 0. Three logical sound events use upstream placeholders until the user supplies recordings.

Cleanup restores captured gravity, hands movement back through vanilla teleport acknowledgement, resets fall distance, and protects the first landing for a bounded interval. Persistent original-gravity recovery is checked on login. Death, logout, dimension changes, external teleports, conflicting movement states, fluids, and unsafe world boundaries abort without damage.

Build and validation details are maintained in README.md and THUNDERCRASH-TESTING.md. Client feel, shader appearance, latency behavior, and stationary/moving ship impacts require gameplay acceptance; a successful dedicated-server harness alone does not establish those results.
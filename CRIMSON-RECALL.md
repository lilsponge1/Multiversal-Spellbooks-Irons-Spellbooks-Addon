# Crimson Susanoo recall

After manifestation completes, press cast again with Crimson Susanoo selected to dismiss the active guardian. Recall has zero cast time and no additional mana cost, including with insufficient mana for another summon. The existing 30-tick fade and normal post-summon cooldown remain. Initial manifestation, upkeep, combat, damage and animation assets are unchanged by this addition.

## Implementation

Source: `modules/crimson-susanoo/`. Iron's Spells 3.16.3 `PlayerRecasts` / `RecastInstance` API registers one synchronized recall only after successful summoning. Its total cast count is two: initial summon plus recall. Marker cleanup removes the native recast through `removeRecast(instance, RecastResult.USER_CANCEL)` on dismissal, expiry, death, logout and dimension change. Recast completion uses the existing dismissal lifecycle. Active lookup verifies the stored owner UUID; a cooldown guard rejects delayed completion after expiry/death. Initial consumable scroll use and `/crimson_susanoo dismiss` remain supported.

## Verification

Java 17 build/reobfuscation and asset/release checks passed. The final module passed **278 diagnostics, zero failures** on a fresh Forge 47.4.10 required-dependency server, with normal exit 0. Checks include instant initiation with one mana, dismissal of the same guardian, unchanged mana, recast cleanup, cooldown activation and rejection of late resummoning.

The **exact final combined JAR** also started in the copied full pack and passed all 278 guardian checks on October 2, 2026. Matching client/server hashes were verified. The owner tested normal summoning followed by a second cast and confirmed: "Yes, it recalls and shows cooldown." This is user-observed live evidence, not an assistant-recorded video.

Artifact: `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-thunderstar-recall3.jar`. SHA-256: `D74BFA26C913D2E3C540D81ED8A468D7F8A32817A169D9516543E275DCCBE4F0`. See [INTEGRATION.md](INTEGRATION.md).

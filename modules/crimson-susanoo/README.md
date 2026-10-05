# Crimson Susanoo module

An owner-bound Fire summon for Iron's Spells 'n Spellbooks. The guardian includes articulated walking and combat, planted feet, terrain navigation, a katana, a solid fiery neck mantle, chest furnace, emissive armor cracks, manifestation, Crescent, Guard and dismissal effects. Its scroll is craftable in Iron's Scroll Forge and shows compact damage information.

After manifestation completes, cast the selected Crimson Susanoo spell again to dismiss your guardian instantly with no additional mana cost. It uses the existing fade-out animation and starts the normal cooldown. Initial summoning still requires its full cast and mana cost. The recall uses Iron's synchronized recast state and clears when the summon ends; `/crimson_susanoo dismiss` also remains available.

## Source ownership

Crimson lives entirely in this directory, with Java package `com.crimson_susanoo`, resource namespace and mod ID `crimson_susanoo`. The root project's Grand Explosion/Thundercrash source remains independent. Both compile to production names before the root packager combines them.

## Build

Use Java 17, set `JAVA_HOME`, and place these legally obtained modpack dependencies in `libs/` (they are ignored by Git):

- `irons_spellbooks-1.20.1-3.16.3.jar`
- `L_Enders_Cataclysm-3.31.jar`
- `geckolib-forge-1.20.1-4.8.4.jar`
- `irons_lib-1.20.1-2.1.0.jar`
- `lionfishapi-3.0.jar`
- `player-animation-lib-forge-1.0.2-rc1+1.20.jar`
- `curios-forge-5.14.1+1.20.1.jar`

`scripts/extract-dependencies.ps1 -PackZip <pack.zip>` can extract these from a pack ZIP whose mods are under `minecraft/mods/`.

Run `./gradlew.bat --no-daemon build`, then `./scripts/verify-assets.ps1` and `./scripts/verify-release.ps1`. The reobfuscated standalone JAR is copied into `dist/`. The first build requires network access for ForgeGradle; `--offline` works after dependencies are cached. On Windows, compiler output uses a module-specific temporary directory to avoid OneDrive file locks. Do not build two copies of this module simultaneously.

## Validation scope

The accepted standalone version passed full-pack startup and 246 disposable-server assertions, including owner protection, target selection, movement, lifecycle, crafting and combat. Asset, pose, foot planting, gait cadence and blade clearance checks also passed. Solas walking and combat recordings were accepted by the owner, including stairs, slabs and uneven-ground sword contact. A two-human-client multiplayer check remains a deployment check for the server owners.

Crimson 0.1.1 adds synchronized ground/movement flags for remote clients, continuous cubic interpolation of the existing poses, stable client animation clocks, a right-hand katana with a vertical cutting plane and a clear guard, and a default 0.5 outgoing damage multiplier. The new config key also applies to worlds retaining the earlier damage bases. Default effective direct damage is 11 / 14 / 20 before Fire Spell Power; the scroll uses the same calculation.

The final combined 0.3.3 + Crimson 0.1.1 + Ignis 0.1.0 JAR passed copied-pack startup and 278 guardian diagnostics, including recall, with zero failures. The owner confirmed that pressing cast again recalls the guardian and shows the cooldown. See [TESTING-0.1.1.md](TESTING-0.1.1.md) and the root [integration report](../../INTEGRATION.md) for interpolation/footing/clearance evidence and remaining remote visual checks. Update the server and every client together. Test diagnostics are explicitly opt-in; keep them off on production servers. Dependencies and personal recordings/worlds are excluded from this repository.

Use Windows PowerShell 5.1 for the System.Drawing asset generator. The build extracts GeckoLib's existing embedded mclib JAR for compilation only; GeckoLib supplies it at runtime.

## Co-op protection

Leave `friendlyFire=false` in `world/serverconfig/crimson_susanoo-server.toml` to protect all players and their tamed pets/summons, including friends without scoreboard teams. Attributed friendly hits are ignored before health or retaliation changes; the guardian's attacks and Crescent also exclude them. `friendlyFire=true` explicitly opts into player combat, subject to normal server/team PvP rules. The guardian always ignores its owner's own hits.

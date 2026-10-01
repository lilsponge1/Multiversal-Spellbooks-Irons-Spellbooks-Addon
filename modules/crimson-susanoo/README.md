# Crimson Susanoo module

An owner-bound Fire summon for Iron's Spells 'n Spellbooks. The guardian includes articulated walking and combat, planted feet, terrain navigation, a katana, a solid fiery neck mantle, chest furnace, emissive armor cracks, manifestation, Crescent, Guard and dismissal effects. Its scroll is craftable in Iron's Scroll Forge and shows compact damage information.

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

The combined JAR needs its own startup validation because it shares metadata and loads Thundercrash's mixins. See the root integration report. Test diagnostics are explicitly opt-in; keep them off on production servers. Neither this module's dependencies nor personal recordings/worlds are distributed in this repository.

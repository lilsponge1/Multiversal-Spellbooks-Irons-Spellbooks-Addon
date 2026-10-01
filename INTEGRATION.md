# Crimson Susanoo integration candidate

## Contents and source boundaries

- Base: published Grand Explosion 0.3.2, including Thundercrash and Cinderstar.
- Added module: accepted Crimson Susanoo 0.1.0.
- Root gameplay source, mixins, assets and original build script are unchanged by this branch.
- Crimson gameplay source and assets are imported unchanged. Its build directory and project name are module-specific; wrapper portability and a packaging documentation reference are adjusted.
- One production JAR registers `irons_ultimate_explosion` and `crimson_susanoo`. Both keep their own versions, configs, resources and network/registry namespaces.

## Build and packaging evidence

Validated October 1, 2026 with Java 17 and the existing ForgeGradle cache.

| Check | Result |
| --- | --- |
| Crimson module offline build and reobfuscation | Passed |
| All 32 imported gameplay source/resource files vs accepted project | Identical |
| Rebuilt Crimson vs accepted standalone EBE64D | Only build manifest differs; all classes/assets identical |
| Asset and release verification | Passed: 22 bones, 118 cubes, 11 animations |
| Published Grand Explosion input SHA-256 | Matches published release checksum |
| Combined payload verification | 115 entries preserved byte for byte; only three shared metadata files merged |
| Forge metadata | Both mod/dependency sections preserved; common loader/license compatible |
| Thundercrash manifest mixin registration | Preserved with referenced config present |
| Assembly failure tests | Passed: collisions, missing mixins, signed inputs, dependency classes and output overwrites rejected |
| Combined dedicated-server runtime | Incomplete: both addons detected; Windows paging-file limit stopped JVM before startup completed |

### Artifact identities

```text
Published Grand Explosion 0.3.2:
4F66CD1DEB2DA0376C77760A77578B84B3F48DD79A6C7602145460147BE65698

Accepted standalone Crimson Susanoo:
EBE64DE513896AA5031FE9235CF510153788B1B163C245F119098E8D8E328FC3

Rebuilt isolated Crimson module:
3D6A2F3DC12AA08375D6593C890404A2739A2BD57912237264EA0543AD166F40

Combined candidate:
1A4481744FBC53BAF676F0EFEE35E6BEDF7556B38A4B104AC24B8C08FD10514F
```

Full local build logs, server logs and per-entry hashes are retained in ignored build/test directories. The candidate's `.integration.json` sidecar records both inputs and all preserved payload hashes.

### Runtime limitation

The separate Forge 47.4.10 full-pack fixture detected both addon versions from this JAR and reached world resource loading without an addon-specific exception. The JVM then failed a native memory allocation with Windows error 1455 (`The paging file is too small`). No `Done` startup marker or combined-runtime diagnostic summary was reached, so the standalone's 246 passed assertions are **not** claimed as a combined-JAR test result. A smaller-heap retry could not proceed because Forge libraries in the disposable installation were missing; that installation is being repaired separately. The original test world's 124 files and all 97 mod JARs were checked against saved hashes and remain unchanged.

The copied world also reported its old `mod:crimson_susanoo` datapack entry as missing. Forge exposes the shared file's resources through the root `irons_ultimate_explosion` pack entry. A world which previously used the standalone Crimson resource pack may report that transition on its first combined startup; all feature resources are present in the shared file. Preserve the root pack entry and verify crafting after the transition.

## Deployment checks

Use the exact same combined JAR on matching clients and the server. Remove the two separate addon JARs to avoid duplicate IDs; retain normal pack dependencies. Preserve world/config backups and run without diagnostic JVM flags in normal play. Existing configs and content IDs remain valid.

The accepted Crimson standalone already passed Solas walking/combat recordings, uneven-ground contact and 246 server assertions. This integration preserves that gameplay and presentation, but does not establish new client-side Thundercrash rendering evidence, two-human-client synchronization or production latency/performance guarantees. Server owners will perform the multiplayer check independently. Grand Explosion retains its original configurable terrain/PvP behavior; the packager does not alter either feature's balance.

The candidate is provided on an integration branch for review. Merge and production-server installation are separate steps for the repository/server owners.

# Working together

## Independent Ki build

Use `build-ki.ps1` for the deployable Ki School addon. It reuses the reviewed Saiyan implementation with independent package, registry, config and network wiring under `src/standalone`. Do not use the historical combined build as the Ki download. Build `tools/build_ki_test.py` separately for disposable diagnostics; its helper is excluded from releases.

Keep this work on `codex/ki-school`. Add its JAR beside the existing addon/Omega JARs and test that arrangement. The instructions below describe the shared legacy source retained in this branch; they do not authorize changing your friend's Omega branches or merging the projects.

## Legacy shared source

1. Accept the repository collaborator invitation and sign in to GitHub on your development PC.
2. Continue Ki/Saiyan work on `codex/ki-school`. Keep Omega Rush/Form work on its own Omega branches. Do not merge the two features or assemble them into one release without an explicit request.
3. Keep Grand Explosion, Thundercrash, Thunderstar and Cinderstar source under root `src/`. Keep Crimson Susanoo under `modules/crimson-susanoo/` and Ignis enchanting compatibility under `modules/ignis-armor-compat/`. Use unique Java packages, resource paths, registry IDs and network channel names for future features.
4. Build each changed feature independently, then assemble the combined release with `build-combined.ps1`. Do not compile the source trees together: the root project uses SRG names and the modules use ForgeGradle official mappings.
5. Test the resulting same combined JAR on a disposable server and matching clients, then open a pull request with the changes and results. The repository owner reviews and merges it into `main`.

A branch keeps unfinished work separate. The directory/package split keeps source ownership separate after merging. Only changes merged into the release branch appear in its combined JAR.

Do not commit third-party dependency JARs, local caches, worlds, logs, credentials or recordings. Publish built releases as GitHub release assets rather than adding binaries to source history. Keep normal dependency mods installed beside the combined JAR.

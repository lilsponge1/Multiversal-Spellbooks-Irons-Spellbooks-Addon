# Working together

1. Accept the repository collaborator invitation and sign in to GitHub on your development PC.
2. Create a feature branch from current `main`; use separate branches for each author's work.
3. Keep Grand Explosion, Thundercrash, Thunderstar and Cinderstar source under root `src/`. Keep Crimson Susanoo under `modules/crimson-susanoo/` and Ignis enchanting compatibility under `modules/ignis-armor-compat/`. Use unique Java packages, resource paths, registry IDs and network channel names for future features.
4. Build each changed feature independently, then assemble the combined release with `build-combined.ps1`. Do not compile the source trees together: the root project uses SRG names and the modules use ForgeGradle official mappings.
5. Test the resulting same combined JAR on a disposable server and matching clients, then open a pull request with the changes and results. The repository owner reviews and merges it into `main`.

A branch keeps unfinished work separate. The directory/package split keeps source ownership separate after merging. Only changes merged into the release branch appear in its combined JAR.

Do not commit third-party dependency JARs, local caches, worlds, logs, credentials or recordings. Publish built releases as GitHub release assets rather than adding binaries to source history. Keep normal dependency mods installed beside the combined JAR.

# Omega Form 0.2.5 deployment

Requested changes: supplied Flower Man music while transformed with 20-block positional audio; twice the double-tap Form flight speed; five less mana per second of passive upkeep. The release is available at [Omega Form 0.2.5](https://github.com/lilsponge1/Multiversal-Spellbooks-Irons-Spellbooks-Addon/releases/tag/v0.3.3-omega-form-0.2.5), with source in [PR #8](https://github.com/lilsponge1/Multiversal-Spellbooks-Irons-Spellbooks-Addon/pull/8).

Combined JAR SHA-256: `09309f6f6941414889bb4b9d1c9924d95012810bd951c2428c3949a5a6b5c7fc`.

The exact production JAR passed 166 Form checks and 114 Rush/compatibility checks on a disposable Forge server (280 total), then shut down normally. Package checks verified the mono Vorbis stream, sound registration, 20-block attenuation, metadata and exclusion of the diagnostic harness. All 135 non-shared payloads belonging to the other combined modules remain byte-identical. A fresh multiplayer listening session has not been performed.

## Hosted server

The server had zero connected players before a normal clean stop. The previous combined JAR was downloaded and verified against the accepted 0.2.4 hash, then retained remotely as `.jar.disabled`. The new upload was downloaded back and matched the tested 0.2.5 artifact exactly.

The active world was read from `server.properties`. Only three saved Omega Form values changed: `manaPerSecond` 30 to 25, `horizontalBlocksPerSecond` 6 to 12, and `verticalBlocksPerSecond` 4 to 8. The editor was reloaded and its complete saved content compared with the reviewed changes before restart. Initial mana cost, Rush dash speed and the other settings were preserved.

The new hosted startup reached `Done` at **2026-10-26 01:14:10.281 UTC** (October 25 in America/Chicago), with `irons_omega_rush 0.2.5` in the loaded-mod list. The server was subsequently observed online. Existing unrelated pack recipe errors remain outside this update.

## Friends' updater

Published version: `2026.10.25-omega-music-1`. It replaces the previous combined 0.2.4 JAR, removes its old managed filename, and retains all other 119 managed payloads byte-for-byte. No configs, saves, shaders or diagnostic mods are added. Every client must apply the update and restart Minecraft before joining the updated server.

Descriptor: 34,761 bytes, SHA-256 `246f1d9e6c8d86481d34191525f632846de0d34b9b34945a26004af7a13285c8`.

Publication complete: the production friend downloader anonymously retrieved and checked the descriptor and **all 120 payloads (677 MiB)** before emitting the ready pointer. The prior live feed still matched the recorded baseline immediately before publication. The existing Drive pointer was updated through its normal version controls, preserving its identity, permissions and prior revisions. Anonymous readback then matched the verified ready pointer exactly and confirmed `2026.10.25-omega-music-1` is live. The same tested JAR is now on GitHub, the server and the updater. The owner's local Prism instance was not directly changed.

Local raw evidence, downloaded server logs, distribution metadata and disposable fixtures remain under `../analysis/omega-music-2026-10-25/`, outside source commits.

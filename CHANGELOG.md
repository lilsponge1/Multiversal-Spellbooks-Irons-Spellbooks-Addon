# Changelog

## 0.3.1

- Added `irons_ultimate_explosion:thundercrash_scroll` to `/give` suggestions and Iron's scroll creative tab. New stacks contain Thundercrash I through Iron's public preset spell-container API.
- Lightning Scroll Forge crafting produces the named scroll using the existing Legendary Ink + Paper + Lightning Bottle recipe. Native validation, ingredient consumption and spell level are preserved.
- Allowed the named scroll in Iron's Inscription Table. Existing generic scrolls still work; Grand Explosion's crafting settings are unchanged.
- Replaced Thundercrash's placeholder sounds with the user's mono 48 kHz Vorbis cast, flight and impact recordings, preserving their bytes and existing playback/cleanup behavior.
- Passed 130 Thundercrash assertions on Forge 47.4.10, plus Grand Explosion, staff, Regalia and PvP regressions. Audio decoding passed with Minecraft's STB Vorbis library.

Replace the old addon JAR on clients and server with this version. Install only one addon JAR; do not install the test harness. New audio has not had an in-game listening check. Earlier deferred observer, ship and latency tests remain deferred.

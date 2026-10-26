# Omega Form 0.2.5

Omega Form now plays the supplied **Flower Man** recording after the transformation ritual succeeds. The song follows the caster, fades over 20 blocks, and loops while Form remains active. Ending Form stops it immediately, including dismissal, exhausted mana, scarf removal, death, dimension changes and disconnect. Rush and hover toggles preserve the song. A player tracking an already transformed caster also receives music; playback begins locally when that caster is first tracked. Music uses the Jukebox/Note Blocks sound slider and Omega's presentation volume.

The double-tap Space flight now defaults to **12 blocks/second horizontally and 8 vertically**, twice the previous 6/4 values. Passive upkeep is **25 mana/second**, reduced from 30. The initial 200-mana activation cost and Rush's guided dash speed remain unchanged.

Install the same combined JAR on the server and all clients, replacing the older combined addon:

`multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.5.jar`

SHA-256: `09309f6f6941414889bb4b9d1c9924d95012810bd951c2428c3949a5a6b5c7fc`

Existing worlds retain saved config values. In the active world's `serverconfig/irons_omega_rush-server.toml`, set `[omegaForm]` to:

```toml
manaPerSecond = 25.0
horizontalBlocksPerSecond = 12.0
verticalBlocksPerSecond = 8.0
```

The music is a mono 44.1 kHz streaming Vorbis conversion of the user-supplied MP3, approximately 192.2 seconds long. No dependency JARs or test harnesses are included. The other combined modules retain all 135 non-shared payloads byte-for-byte. The existing flight animation differs only in JSON whitespace from 0.2.4.

Validation: production compilation and combined-package/resource audit passed; the exact release JAR passed **166 Form assertions and 114 Rush/compatibility assertions** on a disposable Forge server (280 total), then shut down normally. These checks verify mana consumption, doubled configured speeds, double-tap controls, movement, Form/Rush handoff and cleanup. The audio resource, streaming flag and 20-block attenuation passed package checks. A fresh two-player listening session remains unperformed.

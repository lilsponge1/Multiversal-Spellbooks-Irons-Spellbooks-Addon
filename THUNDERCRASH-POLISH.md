# Thundercrash 0.3.3 polish

## Icon and audio

The original blue lightning-gauntlet icon is saved at `art/thundercrash-icon.png` and fitted to a transparent 128 × 128 spell texture. `generate-art.ps1` preserves it; `art/thundercrash-prompt.txt` records its generation prompt.

The supplied recorded cast/flight/impact sequence is separated into three mono 48 kHz Vorbis assets. Charge stops at authoritative launch, impact or cancellation. Flight starts at launch, loops continuously, follows the caster and stops when the actual server collision occurs. Impact stops charge/flight and queues the crash before presentation cleanup. Duplicate impact packets and later stale flight snapshots cannot restart it. Timeout, cancellation, tracking loss, death, unload, disconnect and stale snapshots stop playback without inventing an impact.

Clip durations are approximately 0.80 / 3.79 / 4.39 seconds. Flight contains only pre-impact audio and uses a 120 ms loop crossfade; its duration does not schedule collision. Preparation is reproducible with `tools/prepare_thundercrash_audio.py` using the original recording and an installed PyAV library; source video and generated previews are not distributed. Relative levels are preserved with gain/headroom; any background audio in the supplied recording remains part of the clip.

## Validation

Java 17 compilation, icon/resource checks and Minecraft STB Vorbis decoding passed. Ten headless checks exercise production cast/flight sound lifecycle: continuous looping, caster movement, immediate early-impact stop, repeat cleanup, one-shot charge, launch stop, death and entity removal. These establish lifecycle behavior rather than perceived loudness.

The owner subsequently played the disposable build and reported that Thundercrash was doing great. The final combined release preserves that root gameplay/audio payload. See [INTEGRATION.md](INTEGRATION.md) for the exact artifact and verification limits.

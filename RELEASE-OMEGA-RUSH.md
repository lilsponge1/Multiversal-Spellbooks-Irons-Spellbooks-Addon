# Multiversal Spellbooks with Omega Rush

One JAR now includes Omega Rush, Thundercrash, Grand Explosion, Thunderstar, Cinderstar, Crimson Susanoo, and Ignis armor compatibility.

Omega Rush charges rainbow energy and launches into seven seconds of smoothly guided flight, leaving a wide horizontal cone of delayed rainbow explosions. It includes a forward-arm flight posture, 4.05-block damage radius, charge-only car-drive sound, and approximately six bomb sounds per second without changing playback speed. Its Legendary Nature scroll crafts with Legendary Ink, Paper, and Poisonous Potato at the Scroll Forge.

Replace the older combined addon and standalone Omega Rush JAR with the same new combined JAR on every client and the server. Remove other standalone copies of the included modules. Normal dependency mods stay installed. Existing spell/item IDs and configs are preserved; worlds with the old Omega Rush radius must set `damageRadius = 4.05` in `serverconfig/irons_omega_rush-server.toml`.

Existing module payloads are unchanged; the packager verifies 205 preserved entries, all four mod IDs, and all three Mixin configurations. Seven packaging tests and 114 copied-pack server assertions passed, including crafting and Thundercrash coexistence. See [integration validation](OMEGA-INTEGRATION.md) for runtime evidence and limits.

File: `multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.1.0.jar`

SHA-256: `0B698D1635ED84618BFE297CF0AC752A761B163A9DF88DCD866AFF88E4006DBB`

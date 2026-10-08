# Ki School

Development branch: `codex/ki-school`.

This branch publishes the current Saiyan Ascension feature and prepares a place for a future **Ki** spell school. The spell currently uses **Lightning**; a new school is not registered in this change.

## Included feature

- One transformation spell with Super Saiyan I, II and III, granted by a craftable wig worn in Curios: Head.
- Mana drain defaults of 10 / 17 / 30 per second; SSJ3 charges for 15 seconds. Tap to ascend and hold three seconds to return to Base.
- Translucent waving golden aura, synchronized lightning, charge poses and transformation sounds. SSJ3 starts its spoken line immediately, continues yelling during the charge and plays ten seconds of completion music with vocals removed.
- Sculpted hair for all forms. The SSJ3 mane stays connected to the crown through head turns and full up/down pitch, while the lower locks bend along the back.
- Configurable combat bonuses, server-controlled mana and state, cleanup on equipment/lifecycle changes, and test harnesses kept separate from the production mod.

See [full controls, stats and recipe](SAIYAN-ASCENSION.md), [damage-testing instructions](SAIYAN-DAMAGE-TEST.md) and [SSJ3 details](SAIYAN-SSJ3.md).

## Validation and integration status

The recorded hair4 candidate passed 40 native client checks with Solas V3.7b on the disposable server, with 13 reviewed captures. Its deformation test checked 33,965,568 surface samples and 746,496 root anchors over full head yaw/pitch, partial reveal, maximum sway and standard helmets. The initial SSJ3 candidate passed 410 server regressions; those historical checks were not rerun for hair4. [The audit](docs/SSJ3-VALIDATION.json) distinguishes the tested artifacts and remaining acceptance/performance limits.

This branch starts from current `main` and preserves its Omega modules and packaging support. The historical Saiyan review artifact predates that integration; a newly assembled Saiyan + Omega combined JAR still needs disposable-world testing before release. The documented existing release hashes are not hashes of a new combined build.

Publication checks compiled all 61 root production sources and seven root test sources against the installed pack APIs, validated the resource/audit JSON, and confirmed the copied Saiyan sources/audio match the reviewed workspace. The portable attachment check was rerun successfully with the same sample counts and no sampled head/body collisions.

Run the pure-Java attachment check independently of Minecraft:

```powershell
python tools/check_saiyan_attached_hair.py --jdk-bin C:/path/to/jdk-17/bin --joml C:/path/to/joml-1.10.5.jar
```

Build root `src/` with `build.ps1` and the matching pack/SRG dependencies, then build each independent module and assemble with `build-combined.ps1`; see [contributing](CONTRIBUTING.md). Use matching protocol-5 addon builds on server and clients. Built JARs, worlds, dependencies, local caches and raw reference recordings are excluded from source history.

## Future Ki school

Follow-up work will define and register the Ki school, then connect Saiyan Ascension and future Ki spells to its icon, attributes, items and progression. That school and progression work remains pending.

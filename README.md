# Ki School — Saiyan Ascension

Development branch: **`codex/ki-school`**. Ki School is an independent Minecraft **1.20.1 / Forge** addon with the craftable **Saiyan Wig**, granting Super Saiyan I, II and III.

## Download

**[Download ki-school-1.0.0.jar](https://github.com/lilsponge1/Multiversal-Spellbooks-Irons-Spellbooks-Addon/releases/download/ki-school-v1.0.0/ki-school-1.0.0.jar)**

[Release notes, installation instructions and checksum](https://github.com/lilsponge1/Multiversal-Spellbooks-Irons-Spellbooks-Addon/releases/tag/ki-school-v1.0.0)

## Install on the server

1. Stop the server and Minecraft clients.
2. Add **`ki-school-1.0.0.jar`** to the server's `mods` folder and every client's `mods` folder.
3. Keep the existing addon and Omega JAR installed. Ki is an additional addon with its own **`ki_school`** mod ID.
4. Start the server and clients with their existing modpack dependencies.

Do not also add the older `multiversal-spellbooks-0.3.5-saiyan-ssj3-hair4.jar` review package. That older file bundles base addon IDs and is superseded by this independent Ki download.

Requires Forge **47.4.10+**, Iron's Spells 'n Spellbooks **3.16.3**, Curios **5.x**, and Player Animator **1.x**, plus those mods' normal dependencies. The same Ki file is required on the server and every client. Omega development stays on your friend's existing branches; this package does not include or change it.

## Use

Search **Saiyan Wig** in JEI, craft it, and equip it in **Curios: Head**. Use the existing spell cast key: tap to ascend and hold for three seconds to return to Base.

| Form | Mana per second |
| --- | ---: |
| Super Saiyan I | 10 |
| Super Saiyan II | 17 |
| Super Saiyan III | 30 |

SSJ3 has a fifteen-second charge, sustained transformation voice, ten seconds of completion music, gold aura/lightning and long hair that follows head movement. The reviewed hair geometry, audio and spell balance are retained.

Operator command: `/give @p ki_school:saiyan_wig`.

The dedicated Ki magic school is planned follow-up work. Saiyan Ascension currently uses Iron's **Lightning** school.

## Build

Use Java 17 and Python 3.11+. Supply a JSON array of paths to the matching Forge/SRG Minecraft and mod API JARs:

```powershell
./build-ki.ps1 -JdkBin C:/path/to/jdk-17/bin -ClasspathFile C:/path/to/compile-libraries.json
```

The build derives independent classes from the reviewed `Saiyan*.java` implementation under `src/main`, changing its package, resource namespace, network and config wiring. Standalone registration code is in `src/standalone`. It bundles only Ki classes/resources; neither the base addon nor Omega is included. `build.ps1` and `build-combined.ps1` describe the historical combined addon and are not the independent Ki build entry point.

Separate test sources are under `src/standaloneTest`, and `tools/build_ki_test.py` builds a diagnostics JAR excluded from production. See [Ki release validation](docs/KI-STANDALONE-VALIDATION.json) and [project notes](KI-SCHOOL.md).

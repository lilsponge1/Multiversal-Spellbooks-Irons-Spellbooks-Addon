# Crimson Susanoo 0.1.1 verification

## Requested corrections

Reviewed the main-server walking/combat recording from October 1, 2026 at 01:33 and the two close-up screenshots at 01:41. The accepted base choreography is retained. The complete rig and its rotation/translation tracks are reflected together to put the existing weapon motion on the anatomical right.

- Remote clients receive the server's grounded and actual-moving flags. The controller can select walking even when GeckoLib's local movement flag is false, and the foot solver no longer depends on the client's physics ground flag.
- Pose history uses continuous local entity ticks. Action clocks take the server timestamp's age at receipt, then advance locally, avoiding discontinuities from world-time corrections. Late trackers retain the current phase; ending poses remain bounded until the server clears the state.
- Shape-preserving cubic interpolation runs through GeckoLib's easing API. Neighboring segments share velocity at their keys; reversals have zero tangents. Original poses, damage contact times, durations and stride cadence are preserved.
- The hand holds the katana's wrapped grip. The guard ends seven model pixels ahead of the foremost knuckles. Guard, habaki, steel, glow seam and flame ribbons share a 90-degree longitudinal roll, putting the broad blade dimension vertically.
- A new `damageMultiplier=0.5` multiplies all three direct attacks and Slash splash after base configuration and Fire Spell Power. Existing `22 / 28 / 40` settings therefore yield `11 / 14 / 20` before power. The scroll displays the same calculation. Dependency-owned burn/Brand damage retains its own calculation.
- With `friendlyFire=false`, players, tamed pets and player-owned summons are protected without needing a scoreboard team. Their attributed melee, projectile and spell damage is rejected before hurt animation or retaliation history is written. Target acquisition and every direct/area/Crescent hit use the same protected-target filter. The owner cannot damage its own guardian; enabling friendly combat explicitly still makes player targets obey server/team PvP permissions.

## Checks completed October 1, 2026

| Check | Result |
| --- | --- |
| Java 17 offline ForgeGradle build and reobfuscation | Passed |
| Asset/release verification | Passed: 22 bones, 118 cubes, 128-pixel height, 11 clips; no dependency classes bundled |
| Production easing API and clock tests | 202,319 checks across 249 exported axis tracks; native constant position wrappers, exact keys, bounded values, continuous velocity; 30/60/144 FPS |
| Production foot planting | 630,149 checks; worst planted-corner drift 0.00000034 blocks; penetration within floating-point tolerance |
| Cadence and upper-body counterbalance | Passed at 30/60/144 FPS; no repeated-foot steps in the cadence fixtures |
| Melee contact corridor | Cleave and Slash reach the representative humanoid torso at the existing damage frames, including bounded terrain lowering |
| Full blade/pose pipeline | 17,334 poses / 54 approach, attack, recovery and split-support scenarios; minimum steel clearance 0.165676 blocks |
| Combined packager | Three tests pass; 117 input payload entries preserved with two mod IDs |
| Copied-pack dedicated server, Forge 47.4.10 | Startup `Done`; 273 checks passed, 0 failed; clean process exit after stop |
| Existing saved config upgrade | Old file lacked multiplier; Forge added `damageMultiplier=0.5` while preserving old damage bases |
| Remote-state regression | Grounded/moving fields round-trip through the real entity-data packet codec to two receivers with false local ground flags and zero velocity; controller selects walk with a false GeckoLib movement flag |
| Damage/tooltip regression | All three attacks and Slash splash halve through the production damage helper; tooltip agrees |
| Co-op damage and retaliation | Unteamed player melee, arrows, attributed magic and projectile-owner fallback are ignored; hostile retaliation history is preserved; all three player history sources are filtered; friend's pet and Crimson summon are protected; hostile mob damage and explicit player-combat opt-in still work |

The dedicated server used a separate temporary installation with copies of the 97-mod disposable pack and the exact combined candidate. Its world was disposable. These checks do not establish live rendering, real transport latency or Solas appearance for the changed rig.

The final candidate was also installed in both the existing disposable pack server and Prism test client. The client joined `127.0.0.1:25566` with Solas enabled, and a guardian was summoned successfully. Client and server JAR hashes matched the candidate below; the targeted client-log scan found no Crimson exception/error lines. This startup check does not replace a continuous movement recording or a second-player visual check.

## Earlier October 1 candidate identity

```text
multiversal-spellbooks-0.3.2-crimson-0.1.1.jar
SHA-256: 75DAB58D94D9CEFD7835E74CDFAE33B450225E490D4D58D41BE026E31769D5F0
```

## Remote visual follow-up

Stop Minecraft and the server, replace the previous combined JAR on both, and ensure every client uses this candidate. Do not install the standalone Crimson JAR alongside the combined JAR. Dismiss and resummon after restarting.

Record flat walking, turns, stairs/slabs, and one walking-to-combat pass. Keep both feet visible, then show the right-hand grip from the front and side. Confirm the broad blade faces are vertical, the guard clears the knuckles, and the cutting edge clears terrain. With another client, compare manifestation and attack contact timing. Existing Solas options can remain enabled.

Production JVMs must omit `crimson_susanoo.diagnostics`, `bossEncounter` and pursuit-trace flags. The verification record contains no personal captures or upstream mod assets.

## Owner recall addition — October 2, 2026

Recasting the selected spell after manifestation dismisses the owner's existing guardian with zero cast time and no additional mana. Iron's synchronized recast state supplies one recall (two total casts including the original summon). Existing fade animation, cooldown and initial scroll behavior are retained. Marker cleanup removes the native recast via its public lifecycle API; active lookup verifies the entity's owner UUID. A cooldown guard rejects a delayed recall completion after expiry or death.

Java 17 offline build/reobfuscation and asset/release validation passed. Final module SHA256: `8E87D88E84859F1AC4AC6109FD957C0146CD7FFF2D39674D20C9A5615A714F21`. An isolated Forge 47.4.10 server with the exact required dependencies from the disposable pack passed 278 checks, zero failed, and exited 0 after normal shutdown. New checks cover native instant recall, initiation with one mana, dismissal of the same entity, unchanged mana, removal of recast state, cooldown activation and prevention of a late resummon. The tested payload is preserved in the final combined artifact documented in [the root recall report](../../CRIMSON-RECALL.md). That exact final combined JAR subsequently passed full-pack startup and all 278 guardian checks with zero failures on October 2. Matching client/server hashes were verified, and the owner confirmed that a second cast recalls the guardian and shows the cooldown. See [the root integration report](../../INTEGRATION.md) for the current artifact, checksum and deployment limits.

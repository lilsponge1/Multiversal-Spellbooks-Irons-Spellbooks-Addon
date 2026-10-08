# Disposable combat review

Use the cached **Saiyan Ascension Solas Review 2** Prism instance and `127.0.0.1:25579`. This reuses the review world and modpack with wig12. The server-only monitor lives in the disposable test helper; the release JAR is unchanged.

The wig is equipped in Curios Head. The test fixture gives 1,000 maximum mana as a buffer, but leaves damage, regeneration, speed and resistance bonuses at their configured values. `/saiyantest refill` refills mana without suppressing drain. Casting and camera control are manual.

1. Stay in survival with the same equipment. Run `/saiyantest hit 10` in Base.
2. Activate SSJ1 with **V**, then repeat the command.
3. Briefly press/release **V** to start the three-second SSJ2 charge, then repeat.
4. Hit the stationary cow target with the supplied iron sword in each form. Stay grounded, wait for a full attack cooldown, and use the same sword.
5. Use `/saiyantest stats` to inspect current attributes. Hold **V** for three seconds to power down and verify the bonuses clear.

| Expected behavior | Base | SSJ1 | SSJ2 |
|---|---:|---:|---:|
| Raw 10-HP hit after Saiyan reduction, before armor | 10 | 8.5 | 7.5 |
| Melee multiplier | 1.00 | 1.25 | 1.40 |
| Spell-power multiplier | 1.00 | 1.20 | 1.35 |
| Movement multiplier | 1.00 | 1.15 | 1.25 |
| Attack-speed multiplier | 1.00 | 1.15 | 1.25 |
| Knockback resistance added | 0 | 0.50 | 0.80 |
| Mana regeneration multiplier | 1.00 | 0.50 | 0.25 |
| Gross mana drain per second | 0 | 10 | 17 |

Armor, other defenses, critical hits and attack cooldown can affect final health loss. The monitor reports the addon's own reduction separately from final damage. Damage types that bypass resistance are exempt. Each self-hit command heals first; amounts are capped at 10 HP. Additional commands: `/saiyantest heal`, `/saiyantest dummy`.

Live results are saved in `.tools/saiyan-prism-15/stats.jsonl` and `stats-summary.json`, alongside the server console log. Attribute checks include each actual modifier and the effective totals, accounting for other equipped bonuses and the charge slowdown. Mana checks compare the real before/after values at the controller's payment boundary. Damage checks compare the values at its resistance boundary. Unobserved forms or damage cases remain unverified.

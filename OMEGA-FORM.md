# Omega Form and the Flowery Scarf

Omega Rush 0.2.2 adds a reusable Flowery Scarf and a sustained Omega Form transformation to the single Multiversal Spellbooks JAR. The other combined modules retain their existing payloads and versions.

## Obtain and equip

Flowery Scarf: `irons_omega_rush:flowery_scarf`. Equip it in a **functional charm slot**. It is unstackable, has no durability or recipe, and supplies the locked Omega Form I spell through Iron's normal spell wheel. A cosmetic slot, inventory storage, or holding it does not unlock Form. Its supplied pixel-art icon is preserved, and its gold collar and folded tails render on the player's upper body. Curios' render toggle hides the model without disabling its gameplay role.

The scarf cannot be placed in Iron's Inscription Table to extract or replace its spell. Omega Form is neither craftable nor eligible for random scroll loot. Operator testing can obtain the item with `/give @s irons_omega_rush:flowery_scarf`; casting still requires a functional charm slot.

Boss drops have a **5% chance**, once per player-attributed boss defeat, unaffected by Looting. Eligible bosses are Iron's Dead King and Echo of Tyros, including their ominous variants, and Cataclysm's Ender Guardian, Harbinger, Leviathan, Ancient Remnant, Maledictus, and Scylla. Ignis and Netherite Monstrosity are excluded by default. The drop respects `doMobLoot`, cancelled drop events, and existing loot. Several participating players do not create extra rolls.

Naturally generated chest loot has a **0.2% chance (one in 500)**. This includes vanilla and modded structure loot rather than only temples. A Forge global loot modifier appends the scarf without replacing tables, and each loot context rolls once even through nested tables. Entity rewards and other loot parameter types are excluded. Unopened loot containers can receive the new reward; reopening an already generated container does not reroll it.

## Activate and control

Select Omega Form in the normal spell wheel and cast it. The 1.5-second charge gathers colored energy while the player raises their arms into a bent-elbow flex. Car-drive audio plays only during this charge. Successful activation costs **200 mana**, and upkeep costs **30 mana per second** at normal TPS, whether grounded, hovering, or Rushing. Ordinary mana regeneration continues. There is no fixed Form duration.

| Benefit | Default |
|---|---|
| Damage resistance | Resistance III equivalent; stronger normal Resistance wins |
| Absorption | Eight extra hearts, granted once per transformation |
| Melee strength | Strength I equivalent; stronger normal Strength wins |
| Nature spell power | Attribute multiplier ×1.25 |
| Hover speed | Six blocks/second horizontally; four vertically |
| Cooldown | 120 seconds starting when Form ends |

Flight starts **off**. Double-tap the jump key (normally Space) within seven ticks, approximately 0.35 seconds, to toggle flight on or off. A single tap jumps normally; holding jump does not toggle repeatedly. While flight is on, hold jump to rise, sneak to descend, and use ordinary movement keys horizontally. Releasing movement settles into a hover. Toggling flight off restores normal gravity with short landing protection; the transformation, buffs, and upkeep continue. Riding, swimming, and climbing temporarily use normal movement. Hovering does not change game mode, grant creative flight permissions, or bypass collision. Ordinary spellcasting remains available; incompatible flight controllers are blocked or end Form.

Cast Form again to dismiss it immediately for no additional mana. Removing the scarf, insufficient upkeep mana, death, logout, dimension changes, or clearing the Form status effect also ends it. Remaining Form-owned absorption and modifiers are removed while preserving unrelated effects. The shield does not refill through repeated updates or scarf swapping. Same-dimension teleports reset hover prediction and after-image history rather than ending the transformation.

## Omega Rush interaction

Enabled Form flight uses Rush's horizontal, arms-forward animation. This changes rendering only; hover speeds, collision, aiming, and the double-tap toggle retain their current behavior. Disabling flight restores the normal pose. The shared animation stays active while either Form hover or Rush owns movement and clears on cleanup or resource reload.

Omega Form is required to begin a new Omega Rush by default. Rush uses a **10-second base cooldown while Form is active**, with normal cooldown-reduction attributes and cast-source modifiers still applied. Outside Form, the existing 120-second cooldown remains (when standalone Rush is allowed). It keeps its 200-mana cost, seven-second flight, guided steering, 4.05-block explosion radius, horizontal spread, and approximately six bomb sounds per second. Its scroll recipe remains Legendary Ink + Paper + Poisonous Potato.

Hover yields movement ownership to Rush, which captures Nature power once at launch. If Form runs out or the scarf is removed after launch, the committed Rush finishes with that captured power, while Form's other buffs end. If Form ends during Rush's charge, the charge is cancelled before its mana payment. Rush preserves the selected flight toggle: it returns to hovering only when Form remains active and flight was enabled; otherwise normal gravity and the existing first-landing protection apply. Jump taps during committed Rush do not toggle Form flight.

## Presentation and settings

Form adds a cycling rainbow body tint, first-person hand tint, restrained aura, and fading snapshots of the posed player body and clothing. Full quality retains at most five snapshots per nearby caster, sampled every three ticks, fading within 0.75 seconds. Reduced quality uses two; minimal quality keeps the aura and tint without model echoes. Snapshots do not create entities, hitboxes, held items, armor renderers, shadows, or name tags. Invisibility, teleport, tracking changes, logout, and resource reload clear the relevant presentation state.

The new `[omegaForm]` and `[floweryScarf]` sections of the existing server config expose mana, upkeep, cooldown, hover speeds, defensive values, Nature bonus, drop chances, and eligible boss IDs. `omegaRush.requireOmegaForm=true` enables the prerequisite; set it false to retain standalone Rush access. `omegaRush.omegaFormCooldownSeconds=10` controls the shorter Rush cooldown independently of the normal `cooldownSeconds`. Client `presentation.afterImages` complements the existing particle quality, rainbow overlay, flashes, and sound settings.

Network protocol **3** requires the matching combined JAR on every client and the server. Replace the older combined addon and remove any separate Omega Rush JAR; the module remains inside the one combined file. Existing Rush IDs and settings remain valid.

## Development and verification

Build the Omega module independently, then assemble it with the root packager. When using `-CombinedBaseJar`, use the accepted three-module base **without an older Omega module**; the packager rejects duplicate payloads. The normal separately built module-input workflow includes the current Omega module automatically.

The disposable harness provides `omega_test` for legacy Rush behavior and `omega_form_test` for equipment, casting, buffs, hover transitions, networking validation, and actual loot generation. Never install `omega-rush-test.jar` on the production server or clients.

## Review checkpoint

The local 0.2.2 candidate is `build/release/multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.2.jar`, SHA-256 `08ACF74C03472305CCD5E7292F19769FD6E6BB6907925F4821F619DEFE225805`. The exact artifact passed **135 Form assertions plus 114 Rush/compatibility assertions**, for **249 server checks**. The added cases exercise a native paid Rush with a 200-tick cooldown in Form, the unchanged cooldown outside Form, native cooldown reduction, unchanged Form cooldown, configurable transformed cooldown, zero base cooldown, and preservation of an already committed cooldown when Form ends. Existing mana, equipment, movement, double-tap, loot, cleanup, and pose-resource checks pass.

The archive audit confirms 240 preserved input payload entries, four mod IDs, three Mixin configurations, unchanged other module payloads, correct HUD icon paths, sound assets, and exclusion of development code. Packager code is unchanged; its seven unit checks passed in the preceding revision.

The user reviewed 0.2.1 and reported that it works perfectly. The new Form flight uses the existing accepted Rush animation, but its application during hover still needs an in-game visual check. This revision used background tests and took no desktop control. Broader Form latency, shader/body/quality combinations and multiple-real-client visual checks remain unverified.

The same JAR should be installed on the server and clients. Network protocol remains 3 because packet layouts did not change. The new server setting is automatically added without changing existing normal cooldown settings.

**GitHub publication is pending final review.** Ask before desktop tests and use only the separate test profile. Production profiles and original saves have not been edited.

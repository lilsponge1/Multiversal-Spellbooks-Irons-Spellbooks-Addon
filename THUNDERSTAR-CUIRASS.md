# Cuirass of the Thunderstar

Registry ID: `irons_ultimate_explosion:cuirass_of_the_thunderstar`.

## Behavior

`ThunderstarArmorMaterial` delegates to Travel Optics 6.3.0's installed Riptide Sovereign material. The default chest stats are 11 armor, 4 toughness, 0.05 knockback resistance, 1280 durability and enchantability 15; durability use follows its `armorsShouldBeBreakable` setting. Repair ingredient and equip sound remain delegated. Additional attributes are +150 mana, +5% general spell power, +10% Lightning spell power and +5% Aqua spell power, with no duplicate entries.

`ThunderstarArmorItem` reuses `UnbreakableImbueableArmor`, including one equipped spell slot, normal enchantment/durability rules and template rarity. Riptide Stormline/full-suit effects are not inherited. Travel Optics is a required dependency.

At actual Thundercrash impact, `ThundercrashManager` checks the chest slot and applies **one 1.20 multiplier** after existing spell-power and radial calculations. Holding the item does not qualify. Equipping/removing it before impact changes eligibility; it does not create another damage event or affect Grand Explosion. Normal target defenses still apply.

## Refined appearance

Original geometry reinterprets the supplied Falling Star reference: tapered ivory breastplate and keel, round gold fasteners over beveled hardware, gilded chest markings, small stormstar core, open-throat blue collar, raised layered pauldrons, angled hip plates and long split blue cloth. A 256 × 128 shaded atlas and restrained emissive mask support 16 bones and 83 cuboids. Cloth uses Iron's `armorTorsoExtensionLeftLeg/RightLeg` hierarchy to follow leg rotation. The Forge armor-texture override returns the actual `thunderstar.png` path instead of a nonexistent vanilla layer texture.

Assets are under `src/main/resources/assets/irons_ultimate_explosion/`. Edit `tools/generate_thunderstar_art.ps1` for persistent changes; `build.ps1` regenerates the geometry, textures, animation and 32-pixel inventory icon. Geometry can be imported into Blockbench with the Bedrock/GeckoLib format. The reference image itself is not bundled.

## Acquisition

Available in Iron's equipment creative tab or:

```text
/give @s irons_ultimate_explosion:cuirass_of_the_thunderstar
```

The optional `docs/thunderstar-recipe.example.json` is intentionally **inactive**, with placeholder ingredients pending balancing. Copying it into the addon's recipe datapack enables the smithing upgrade from the Riptide chestpiece and preserves input armor data. This release does not silently enable it.

## Validation

Java 17 compilation and asset validation passed, including hierarchy, UV bounds, textures/glow mask, animation, icon and localization. **63 copied-full-pack checks passed with zero failures**, followed by normal server exit 0. They exercised registered stats, operations/slot UUIDs, durability configurations, enchantability, Protection, spell slots, repair ingredients and actual Thundercrash impacts. Checks cover center/falloff bonus, held/removed armor, equip-before-impact, one hurt event and no Grand Explosion bonus. The harness is under `src/test`, excluded from production, and opt-in for disposable worlds.

The owner hit two identical 1,000-HP zero-armor husks with one level-one Thundercrash each: **40.00 damage without the cuirass; 55.44 with it**. That matches 40 × 1.05 × 1.10 × 1.20: ordinary spell/Lightning attributes plus exactly one unique 20% perk. The owner also confirmed that the refined chestpiece looked better in the disposable client. Detailed cloth motion and all armor poses were not independently recorded.

Later rendering refinements and Susanoo recall do not change the tested armor gameplay. The final combined JAR and hashes are recorded in [INTEGRATION.md](INTEGRATION.md).

# Ignis Armor Compatibility 0.1.0

Forge 1.20.1 compatibility module for **Cataclysm: Spellbooks 1.2.9**, maintained independently under `modules/ignis-armor-compat/`.

## Behavior

These existing items become eligible for normal enchanting-table offers:

- `cataclysm_spellbooks:ignis_helmet`
- `cataclysm_spellbooks:ignis_chestplate`
- `cataclysm_spellbooks:ignis_leggings`
- `cataclysm_spellbooks:ignis_boots`
- `cataclysm_spellbooks:ignis_chestplate_elytra`

The installed Ignis classes override `isDamageable(ItemStack)` to return false. Minecraft's inherited `Item.isEnchantable` uses that value as an eligibility gate, preventing table offers even though the armor material has enchantability 15. This module changes only that gate for the five registry IDs. It retains the registered items, classes, armor material, attributes, durability behavior, spell-container capabilities, renderers and flight behavior.

Normal enchantment categories and armor-slot rules still apply. Enchanting tables require lapis, XP and sufficient bookshelf power; treasure enchantments such as Mending remain book-only. Anvils retain vanilla/Forge compatibility rules, conflicts, repair cost and survival level requirements. Unbreaking and Mending can be stored when the normal enchantment rules allow them, but provide no durability benefit while the armor is unbreakable.

No enchantments are forced onto invalid items. Base Cataclysm Ignitium armor, Cursium/Maledictus equipment and unrelated items are outside this patch.

## Build

Use Java 17:

```powershell
./gradlew.bat --no-daemon --offline build
```

Output: `dist/ignis_armor_compat-0.1.0.jar`. Dependencies are not bundled. The Forge 1.20.1 mixin lists both the official development method and the production SRG method; `require=1` ensures a missing target fails explicitly instead of silently doing nothing.

For the combined addon, the repository assembly tool accepts an optional `--ignis-armor` input alongside the separately built Grand Explosion and Crimson modules. `build-combined.ps1` includes this module by default. It preserves all three modules' original payloads and merges both required mixin registrations. Build each module separately before assembling.

## Installation

Install matching versions on the server and clients with Minecraft stopped. Choose either the combined JAR containing all three modules or the standalone armor addon alongside a combined JAR that does **not** already contain `ignis_armor_compat`. Never install both forms of this module. Existing armor does not need to be recrafted. Nothing changes item registry IDs.

## Diagnostics

`-Dignis_armor_compat.diagnostics=true` enables integration checks on server startup. Use only in a disposable world: it places an enchanting table/bookshelves and uses a fake survival player. Omit the flag in normal play.

Checks exercise actual table offers and clicks, lapis/XP consumption, book results in real anvils, normal survival costs, Protection/Unbreaking/Mending, boot-only Feather Falling, invalid sword enchantments, conflicting protection, modded enchantments and existing item NBT/name preservation. The anvil fixture sends the current display name as the actual client does; leaving it null would intentionally remove a custom name in vanilla.

# Ignis Mage enchanting integration

The independent `modules/ignis-armor-compat/` module is included in the combined build. It fixes enchanting eligibility for Cataclysm: Spellbooks 1.2.9's five Ignis Mage pieces while retaining existing items, attributes, renderers, flight behavior and unbreakable durability.

Java 17 build and reobfuscation passed. The module's verified payload started in a copied 97-mod Forge 47.4.10 pack and passed **499 armor checks, zero failures**, followed by normal exit 0:

- All five pieces produced real table offers, consumed three lapis/levels on the highest offer, received enchantments and retained custom names/nested NBT.
- Actual anvil menus accepted Protection IV, Unbreaking III, Mending and each compatible registered mod enchantment, with normal survival costs/pickup checks.
- Feather Falling remained boots-only; weapon-only and conflicting protection books were rejected.
- Armor remained non-damageable. Ordinary blocks, vanilla armor and base Ignitium are outside the patch.

The final combined release preserves that exact module payload and both required mixin configurations; see [INTEGRATION.md](INTEGRATION.md). The assembler has five regression tests for payload/metadata handling and unsafe inputs.

Tests use production menus and a fake survival player. A live GUI check on previously existing main-server armor and every third-party enchantment's gameplay effect are not established by these checks. Mending/Unbreaking can be stored normally but have no durability benefit while armor is unbreakable. Existing armor does not need recrafting. Diagnostic flags must be omitted in normal play.

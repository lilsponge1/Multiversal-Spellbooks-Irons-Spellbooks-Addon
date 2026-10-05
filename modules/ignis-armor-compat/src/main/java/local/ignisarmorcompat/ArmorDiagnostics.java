package local.ignisarmorcompat;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in integration checks. Run only against a disposable server/world. */
public final class ArmorDiagnostics {
    private int passed;
    private int failed;

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        try {
            var level = event.getServer().overworld();
            FakePlayer player = FakePlayerFactory.get(level,
                    new GameProfile(UUID.randomUUID(), "IgnisArmorTest"));
            player.getAbilities().instabuild = false;
            player.experienceLevel = 100;
            BlockPos table = new BlockPos(0, 80, 0);
            level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
            // Real table logic samples these shelves through Forge's normal hooks.
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                if (Math.abs(x) == 2 || Math.abs(z) == 2)
                    for (int y = 0; y <= 1; y++) level.setBlockAndUpdate(table.offset(x, y, z),
                            Blocks.BOOKSHELF.defaultBlockState());
            for (String path : new String[]{"ignis_helmet", "ignis_chestplate", "ignis_leggings",
                    "ignis_boots", "ignis_chestplate_elytra"}) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cataclysm_spellbooks", path));
                check(item != null && item != Items.AIR, path + " registered");
                if (item == null || item == Items.AIR) continue;
                ItemStack stack = fixture(item);
                check(item instanceof ArmorItem, path + " retains armor class");
                check(IgnisArmorPolicy.appliesTo(item), path + " covered");
                check(!item.isDamageable(stack) && !stack.isDamageableItem(), path + " remains non-damageable");
                check(stack.isEnchantable(), path + " table eligibility restored");
                check(item.getEnchantmentValue(stack) == 15, path + " original enchantability preserved");

                EnchantmentMenu menu = new EnchantmentMenu(0, player.getInventory(),
                        ContainerLevelAccess.create(level, table));
                menu.getSlot(0).set(stack.copy());
                menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
                check(menu.costs[2] > 0 && menu.enchantClue[2] >= 0, path + " real table offers enchantment");
                int before = player.experienceLevel;
                check(menu.clickMenuButton(player, 2), path + " real table button succeeds");
                ItemStack enchanted = menu.getSlot(0).getItem();
                check(enchanted.isEnchanted(), path + " table writes enchantments");
                check(!enchanted.isEnchantable(), path + " no repeat table enchant");
                check(menu.getSlot(1).getItem().isEmpty(), path + " table consumes three lapis");
                check(player.experienceLevel == before - 3, path + " table charges three levels");
                check(preservesFixture(enchanted), path + " table preserves custom data");

                anvil(player, stack, Enchantments.ALL_DAMAGE_PROTECTION, 4, true, path + " Protection IV");
                anvil(player, stack, Enchantments.UNBREAKING, 3, Enchantments.UNBREAKING.canEnchant(stack), path + " Unbreaking III");
                anvil(player, stack, Enchantments.MENDING, 1, Enchantments.MENDING.canEnchant(stack), path + " Mending");
                boolean boots = ((ArmorItem)item).getType() == ArmorItem.Type.BOOTS;
                anvil(player, stack, Enchantments.FALL_PROTECTION, 4, boots, path + " Feather Falling slot rule");
                anvil(player, stack, Enchantments.SHARPNESS, 5, false, path + " rejects weapon-only book");
                ItemStack protectedStack = stack.copy();
                EnchantmentHelper.setEnchantments(Map.of(Enchantments.ALL_DAMAGE_PROTECTION, 4), protectedStack);
                anvil(player, protectedStack, Enchantments.FIRE_PROTECTION, 4, false, path + " preserves protection conflict");
                for (var entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
                    if (!entry.getKey().location().getNamespace().equals("minecraft")) {
                        Enchantment enchantment = entry.getValue();
                        anvil(player, stack, enchantment, 1, enchantment.canEnchant(stack),
                                path + " " + entry.getKey().location());
                    }
                }
            }
            check(!IgnisArmorPolicy.appliesTo(Items.NETHERITE_HELMET), "vanilla armor excluded from patch");
            check(new ItemStack(Items.NETHERITE_HELMET).isEnchantable(), "vanilla armor stays enchantable");
            check(!IgnisArmorPolicy.appliesTo(Items.STONE), "ordinary blocks excluded");
            check(!new ItemStack(Items.STONE).isEnchantable(), "ordinary blocks remain non-enchantable");
            check(!IgnisArmorPolicy.appliesTo(ForgeRegistries.ITEMS.getValue(new ResourceLocation("cataclysm", "ignitium_helmet"))),
                    "base Cataclysm Ignitium excluded");
        } catch (Exception exception) {
            failed++;
            IgnisArmorCompatibility.LOGGER.error("Ignis armor diagnostics aborted", exception);
        }
        IgnisArmorCompatibility.LOGGER.info("Ignis armor diagnostics: {} passed, {} failed", passed, failed);
    }

    private void anvil(FakePlayer player, ItemStack stack, Enchantment enchantment, int rank,
            boolean expected, String label) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(stack.copy());
        // The actual client sends the existing display name on slot changes.
        // A null name in a headless fixture requests removal of a custom name.
        menu.setItemName(stack.getHoverName().getString());
        menu.getSlot(1).set(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, rank)));
        menu.createResult();
        ItemStack result = menu.getSlot(2).getItem();
        check(!result.isEmpty() == expected, label + " real anvil acceptance");
        if (expected && !result.isEmpty()) {
            check(EnchantmentHelper.getItemEnchantmentLevel(enchantment, result) == rank, label + " level preserved");
            check(preservesFixture(result), label + " custom data preserved");
            check(menu.getCost() > 0, label + " charges normal levels");
            check(menu.getSlot(2).mayPickup(player), label + " survival can take result");
        }
    }

    private static ItemStack fixture(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal("Existing Ignis Armor"));
        CompoundTag data = new CompoundTag();
        data.putString("spell", "crimson_susanoo:crimson_susanoo");
        data.putInt("rank", 1);
        stack.getOrCreateTag().put("IgnisCompatFixture", data);
        return stack;
    }

    private static boolean preservesFixture(ItemStack stack) {
        return stack.hasCustomHoverName() && stack.getHoverName().getString().equals("Existing Ignis Armor")
                && stack.hasTag() && stack.getTag().getCompound("IgnisCompatFixture").getInt("rank") == 1
                && stack.getTag().getCompound("IgnisCompatFixture").getString("spell").equals("crimson_susanoo:crimson_susanoo");
    }

    private void check(boolean condition, String label) {
        if (condition) passed++;
        else { failed++; IgnisArmorCompatibility.LOGGER.error("FAIL: {}", label); }
    }
}

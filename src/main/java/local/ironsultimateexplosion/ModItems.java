package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GrandExplosionMod.ID);
    public static final RegistryObject<Item> CINDERSTAR_STAFF = ITEMS.register("cinderstar_staff", () -> new CinderstarStaffItem(
            new Item.Properties().m_41487_(1), 8.0, -2.5,
            Map.of(
                    AttributeRegistry.FIRE_SPELL_POWER.get(), new AttributeModifier(UUID.fromString("8e0a8a25-7dd2-4329-a934-cf4585c24329"), "Cinderstar fire power", 0.30, AttributeModifier.Operation.MULTIPLY_BASE),
                    AttributeRegistry.COOLDOWN_REDUCTION.get(), new AttributeModifier(UUID.fromString("d2ee0db7-e71b-4619-945f-e47c8387ac99"), "Cinderstar cooldown", 0.10, AttributeModifier.Operation.MULTIPLY_BASE))));
    public static final RegistryObject<Item> CINDERSTAR_HAT = ITEMS.register("cinderstar_hat", () ->
            new CinderstarArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> CINDERSTAR_ROBE = ITEMS.register("cinderstar_robe", () ->
            new CinderstarArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> CINDERSTAR_LEGGINGS = ITEMS.register("cinderstar_leggings", () ->
            new CinderstarArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> CINDERSTAR_BOOTS = ITEMS.register("cinderstar_boots", () ->
            new CinderstarArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    private ModItems() {}
}

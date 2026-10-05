package local.ignisarmorcompat;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class IgnisArmorPolicy {
    private static final Set<ResourceLocation> ITEMS = Set.of(
            id("ignis_helmet"), id("ignis_chestplate"),
            id("ignis_leggings"), id("ignis_boots"), id("ignis_chestplate_elytra"));

    private IgnisArmorPolicy() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation("cataclysm_spellbooks", path);
    }

    public static boolean appliesTo(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && ITEMS.contains(key);
    }
}

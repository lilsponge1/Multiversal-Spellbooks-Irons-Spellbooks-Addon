package local.kischool;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KiSchoolMod.ID);
    public static final RegistryObject<Item> SAIYAN_BOOK = ITEMS.register("book_of_saiyan_ascension", SaiyanBook::new);
    public static final RegistryObject<Item> SAIYAN_WIG = ITEMS.register("saiyan_wig", SaiyanWig::new);
    static void fillCreativeTabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == io.redspace.ironsspellbooks.registries.CreativeTabRegistry.EQUIPMENT_TAB.get())
            event.accept(SAIYAN_WIG);
    }
    private ModItems() {}
}

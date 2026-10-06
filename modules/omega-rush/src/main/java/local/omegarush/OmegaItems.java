package local.omegarush;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
public final class OmegaItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,OmegaMod.ID);
    public static final RegistryObject<Item> SCARF=ITEMS.register("flowery_scarf",FloweryScarf::new);
    private OmegaItems() {}
}

package local.kischool;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Independent Ki addon: never registers the base addon's mod IDs or content. */
@Mod(KiSchoolMod.ID)
public final class KiSchoolMod {
    public static final String ID = "ki_school";
    public KiSchoolMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModSpells.SPELLS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(ModItems::fillCreativeTabs);
        MinecraftForge.EVENT_BUS.register(SaiyanManager.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, KiConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KiConfig.CLIENT);
        KiNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            SaiyanClient.register();
            bus.addListener(SaiyanClient::reload);
            bus.addListener(SaiyanWigRenderer::setup);
        });
    }
}

package local.ironsultimateexplosion;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

@Mod(GrandExplosionMod.ID)
public final class GrandExplosionMod {
    public static final String ID = "irons_ultimate_explosion";

    public GrandExplosionMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSpells.SPELLS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModItems.ITEMS.register(bus);
        bus.addListener(ModItems::fillCreativeTabs);
        MinecraftForge.EVENT_BUS.register(SpellEvents.class);
        MinecraftForge.EVENT_BUS.register(CraterManager.class);
        MinecraftForge.EVENT_BUS.register(ThundercrashManager.class);
        MinecraftForge.EVENT_BUS.register(SaiyanManager.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ExplosionConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ExplosionConfig.CLIENT);
        ExplosionNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEffects::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ThundercrashClient.register();
            bus.addListener(ThundercrashClient::reload);
            SaiyanClient.register();
            bus.addListener(SaiyanClient::reload);
            bus.addListener(SaiyanWigRenderer::setup);
        });
    }
}

package local.omegarush;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;

@Mod(OmegaMod.ID)
public final class OmegaMod {
    public static final String ID = "irons_omega_rush";
    public OmegaMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSpells.SPELLS.register(bus);
        OmegaParticles.TYPES.register(bus);
        OmegaSounds.SOUNDS.register(bus);
        OmegaItems.ITEMS.register(bus);
        OmegaEffects.EFFECTS.register(bus);
        OmegaLoot.SERIALIZERS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OmegaConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OmegaConfig.CLIENT);
        MinecraftForge.EVENT_BUS.register(OmegaManager.class);
        MinecraftForge.EVENT_BUS.register(OmegaFormManager.class);
        MinecraftForge.EVENT_BUS.register(OmegaLoot.class);
        OmegaNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            OmegaClient.register();
            bus.addListener(OmegaClient::reload);
            bus.addListener(OmegaParticles.Client::register);
            bus.addListener(OmegaOverlay::layers);
        });
    }
}

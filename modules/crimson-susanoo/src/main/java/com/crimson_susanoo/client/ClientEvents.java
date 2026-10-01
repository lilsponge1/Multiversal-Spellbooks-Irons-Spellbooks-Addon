package com.crimson_susanoo.client;

import com.crimson_susanoo.CrimsonSusanoo;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CrimsonSusanoo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(CrimsonSusanoo.GUARDIAN.get(), CrimsonRenderer::new);
            EntityRenderers.register(CrimsonSusanoo.WAVE.get(), WaveRenderer::new);
        });
    }
}

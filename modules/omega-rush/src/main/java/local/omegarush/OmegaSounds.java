package local.omegarush;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class OmegaSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,OmegaMod.ID);
    public static final RegistryObject<SoundEvent> DRIVE=SOUNDS.register("car_drive",()->SoundEvent.m_262824_(new ResourceLocation(OmegaMod.ID,"car_drive")));
    public static final RegistryObject<SoundEvent> BOMB=SOUNDS.register("bomb",()->SoundEvent.m_262824_(new ResourceLocation(OmegaMod.ID,"bomb")));
    private OmegaSounds() {}
}

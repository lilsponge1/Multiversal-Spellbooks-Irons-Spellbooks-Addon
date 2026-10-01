package local.ironsultimateexplosion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GrandExplosionMod.ID);
    public static final RegistryObject<SoundEvent> CAST = add("thundercrash_cast"),
            FLIGHT = add("thundercrash_flight"), IMPACT = add("thundercrash_impact");
    private static RegistryObject<SoundEvent> add(String name) {
        return SOUNDS.register(name, () -> SoundEvent.m_262824_(new ResourceLocation(GrandExplosionMod.ID, name)));
    }
    private ModSounds() {}
}

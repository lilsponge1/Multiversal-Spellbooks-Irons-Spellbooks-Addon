package local.ironsultimateexplosion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GrandExplosionMod.ID);
    public static final RegistryObject<SoundEvent> CAST = add("thundercrash_cast"),
            FLIGHT = add("thundercrash_flight"), IMPACT = add("thundercrash_impact");
    public static final RegistryObject<SoundEvent> SAIYAN_CHARGE = add("saiyan_charge"),
            SAIYAN_CHARGE_2 = add("saiyan_charge_2"), SAIYAN_CRACK = add("saiyan_crack"),
            SAIYAN_BURST = add("saiyan_burst"), SAIYAN_DOWN = add("saiyan_down"),
            SAIYAN_YELL = add("saiyan_yell"), SAIYAN_YELL_2 = add("saiyan_yell_2"),
            SAIYAN_CHARGE_3=add("saiyan_charge_3"), SAIYAN_YELL_3=add("saiyan_yell_3"), SAIYAN_MUSIC_3=add("saiyan_music_3");
    private static RegistryObject<SoundEvent> add(String name) {
        return SOUNDS.register(name, () -> SoundEvent.m_262824_(new ResourceLocation(GrandExplosionMod.ID, name)));
    }
    private ModSounds() {}
}

package local.ironsultimateexplosion;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;

/** A tracked charge cue that can stop immediately on launch, impact or cancellation. */
final class ThundercrashCastSound extends AbstractTickableSoundInstance {
    private final LivingEntity caster;
    private boolean ended;
    ThundercrashCastSound(LivingEntity caster) {
        super(ModSounds.CAST.get(), SoundSource.PLAYERS, SoundInstance.m_235150_());
        this.caster = caster; f_119578_ = false; f_119579_ = 0;
        f_119580_ = SoundInstance.Attenuation.LINEAR; f_119582_ = false;
        f_119574_ = 1; m_7788_();
    }
    @Override public void m_7788_() {
        if (ended || !caster.m_6084_() || caster.m_213877_()) { m_119609_(); return; }
        f_119575_ = caster.m_20185_(); f_119576_ = caster.m_20186_() + 1; f_119577_ = caster.m_20189_();
        f_119573_ = ThundercrashConfig.CAST_VOLUME.get().floatValue();
    }
    void end() { ended = true; m_119609_(); }
}

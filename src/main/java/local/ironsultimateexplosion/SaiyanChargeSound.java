package local.ironsultimateexplosion;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Rising native spell energy; follows the caster and stops at the authoritative phase boundary. */
final class SaiyanChargeSound extends AbstractTickableSoundInstance {
    private final Entity entity;
    private final float volume;
    private final boolean second;
    private final boolean vocal;
    SaiyanChargeSound(Entity entity, float volume, boolean second) {
        this(entity, volume, second, second ? ModSounds.SAIYAN_CHARGE_2.get() : ModSounds.SAIYAN_CHARGE.get(), false);
    }
    SaiyanChargeSound(Entity entity, float volume, boolean second, net.minecraft.sounds.SoundEvent event, boolean vocal) {
        super(event, SoundSource.PLAYERS, RandomSource.m_216327_());
        this.entity = entity; this.volume = volume; this.second = second; this.vocal = vocal; progress(0);
        f_119578_ = true; f_119579_ = 0; follow();
    }
    void progress(float amount) {
        float p = Math.max(0, Math.min(1, amount));
        // Full-length samples already match each charge; pitch changes would shorten the yell.
        if (vocal) { f_119573_ = volume * (second ? .95f + .25f * p : .8f + .2f * p); f_119574_ = 1; return; }
        f_119573_ = volume * (second ? .65f + 1.1f * p : .35f + .85f * p);
        f_119574_ = second ? .65f + .45f * p : .72f + .38f * p;
    }
    private void follow() { f_119575_ = entity.m_20185_(); f_119576_ = entity.m_20186_() + 1; f_119577_ = entity.m_20189_(); }
    @Override public void m_7788_() { if (!entity.m_6084_() || entity.m_213877_()) end(); else follow(); }
    void end() { m_119609_(); }
}

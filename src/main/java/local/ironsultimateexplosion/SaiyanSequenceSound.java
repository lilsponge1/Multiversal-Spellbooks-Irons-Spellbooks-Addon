package local.ironsultimateexplosion;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Positional one-shot voice/music with an explicit lifetime and cleanup. */
final class SaiyanSequenceSound extends AbstractTickableSoundInstance {
    private final Entity entity;
    private int remaining;
    SaiyanSequenceSound(Entity entity,SoundEvent event,float volume,int ticks,boolean loop) {
        super(event,SoundSource.PLAYERS,RandomSource.m_216327_()); this.entity=entity; remaining=ticks;
        f_119573_=volume; f_119574_=1; f_119578_=loop; f_119579_=0; follow();
    }
    private void follow() { f_119575_=entity.m_20185_(); f_119576_=entity.m_20186_()+1; f_119577_=entity.m_20189_(); }
    @Override public void m_7788_() { if(--remaining<=0 || !entity.m_6084_() || entity.m_213877_()) end(); else follow(); }
    void end() { m_119609_(); }
}

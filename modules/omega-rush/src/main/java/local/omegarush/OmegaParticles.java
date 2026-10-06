package local.omegarush;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.*;
import net.minecraft.client.particle.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
public final class OmegaParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,OmegaMod.ID);
    public static final RegistryObject<SimpleParticleType> RING=TYPES.register("rainbow_ring",()->new SimpleParticleType(false) {});
    public static final RegistryObject<SimpleParticleType> PULSE=TYPES.register("rainbow_pulse",()->new SimpleParticleType(false) {});
    public static final RegistryObject<SimpleParticleType> GLOBE=TYPES.register("rainbow_globe",()->new SimpleParticleType(false) {});
    public static final class Client {
        public static void register(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(RING.get(),sprites->(type,level,x,y,z,size,hue,flash)->new Frame(level,x,y,z,sprites,size,hue,flash,7));
            e.registerSpriteSet(PULSE.get(),sprites->(type,level,x,y,z,size,hue,flash)->new Frame(level,x,y,z,sprites,size,hue,flash,5));
            e.registerSpriteSet(GLOBE.get(),sprites->(type,level,x,y,z,size,hue,flash)->new Frame(level,x,y,z,sprites,size,hue,flash,8));
        }
    }
    public static final class Frame extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final boolean flash;
        private final float red,green,blue;
        Frame(ClientLevel level,double x,double y,double z,SpriteSet sprites,double size,double hue,double white,int frames) {
            super(level,x,y,z); this.sprites=sprites; flash=white>0;
            f_107225_=12; f_107219_=false; f_107663_=(float)Math.max(0.1,Math.min(6,size));
            int rgb=java.awt.Color.HSBtoRGB((float)(hue-Math.floor(hue)),0.9f,1);
            red=((rgb>>16)&255)/255f; green=((rgb>>8)&255)/255f; blue=(rgb&255)/255f;
            m_107253_(flash?1:red,flash?1:green,flash?1:blue);
            m_108337_(sprites.m_5819_(flash?0:Math.min(2,frames-1),frames-1));
        }
        @Override public void m_5989_() {
            f_107209_=f_107212_; f_107210_=f_107213_; f_107211_=f_107214_;
            if(++f_107224_>=f_107225_) { m_107274_(); return; }
            int index=flash?f_107224_:Math.min(11,f_107224_+3);
            m_108337_(sprites.m_5819_(index,11));
            m_107253_(flash&&f_107224_<2?1:red,flash&&f_107224_<2?1:green,flash&&f_107224_<2?1:blue);
            f_107229_=Math.max(0,1-f_107224_/12f);
        }
        @Override protected int m_6355_(float partial) { return 15728880; }
        @Override public ParticleRenderType m_7556_() { return ParticleRenderType.f_107432_; }
    }
    private OmegaParticles() {}
}

package local.omegarush;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
public final class OmegaVisuals {
    private record Active(UUID caster,long session,Particle particle) {}
    private static final List<Active> ACTIVE=new ArrayList<>();
    private static final Map<UUID,Double> NEXT_BOMB=new HashMap<>();
    private static int budget,burstSounds;
    private static long lastBudgetTick=-1;
    public static void beginTick() {
        ClientLevel l=Minecraft.m_91087_().f_91073_; if(l==null) return;
        long time=l.m_46467_(); if(time==lastBudgetTick) return; lastBudgetTick=time;
        budget=switch(OmegaConfig.QUALITY.get()) { case FULL->400; case REDUCED->120; case MINIMAL->32; }; burstSounds=0;
        NEXT_BOMB.values().removeIf(t->t>time+4||time-t>100);
        ACTIVE.removeIf(a->!a.particle.m_107276_());
    }
    private static Vector3f color(double hue) {
        int rgb=java.awt.Color.HSBtoRGB((float)(hue-Math.floor(hue)),0.9f,1);
        return new Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f);
    }
    private static void dust(ClientLevel l,Vec3 p,Vec3 velocity,double hue,float size) {
        if(budget--<=0) return;
        l.m_7106_(new DustParticleOptions(color(hue),size),p.f_82479_,p.f_82480_,p.f_82481_,velocity.f_82479_,velocity.f_82480_,velocity.f_82481_);
    }
    private static void frame(UUID caster,long session,Vec3 p,double size,double hue,boolean white,int variant) {
        if(budget--<=0) return;
        var type=switch(variant) { case 0->OmegaParticles.GLOBE.get(); case 1->OmegaParticles.PULSE.get(); default->OmegaParticles.RING.get(); };
        Particle particle=Minecraft.m_91087_().f_91061_.m_107370_(type,p.f_82479_,p.f_82480_,p.f_82481_,size,hue,white?1:0);
        if(particle!=null) ACTIVE.add(new Active(caster,session,particle));
    }
    public static void flight(ClientLevel l,OmegaClient.Flight f) {
        if(f.entity.m_20182_().m_82554_(Minecraft.m_91087_().f_91074_.m_20182_())>96) return;
        Vec3 center=f.entity.m_20191_().m_82399_(); double time=f.visualAge;
        if(f.packet.phase()==OmegaState.CHARGE) {
            double progress=Math.min(1,Math.max(f.packet.age(),f.visualAge)/(double)Math.max(1,f.packet.remaining()));
            double radius=2.4*(1-progress)+0.25;
            int count=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?12:4;
            for(int i=0;i<count;i++) {
                double a=time*0.35+i*Math.PI*2/count;
                Vec3 p=center.m_82520_(Math.cos(a)*radius,Math.sin(a*1.3)*radius*0.65,Math.sin(a)*radius);
                dust(l,p,center.m_82546_(p).m_82490_(0.12),i/(double)count+time/35,0.9f);
            }
            if(f.visualAge%4==0) frame(f.packet.caster(),f.packet.session(),center,radius,time/24,false,2);
        } else {
            Vec3 back=f.velocity.m_82541_().m_82490_(-0.7);
            int count=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?8:2;
            for(int i=0;i<count;i++) {
                double a=time*0.25+i*Math.PI*2/count;
                Vec3 p=center.m_82549_(back).m_82520_(Math.cos(a)*0.6,Math.sin(a)*0.7,Math.sin(a*1.3)*0.35);
                dust(l,p,back.m_82490_(0.08),time/25+i/(double)count,0.8f);
            }
        }
    }
    public static void launch(ClientLevel l,LivingEntity entity) {
        Vec3 center=entity.m_20191_().m_82399_();
        frame(entity.m_20148_(),OmegaClient.FLIGHTS.get(entity.m_20148_()).packet.session(),center,1.2,0.13,OmegaConfig.FLASH.get(),1);
    }
    public static void formPulse(ClientLevel l,LivingEntity e,long session){beginTick();frame(e.m_20148_(),session,e.m_20191_().m_82399_(),2.0,session*0.13,OmegaConfig.FLASH.get(),1);}
    static void ritualFlash(ClientLevel l,OmegaRitualEventPacket p){
        beginTick();frame(p.caster(),p.session(),p.position(),3.2,0.13,OmegaConfig.FLASH.get(),1);
        int n=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?36:12;
        for(int i=0;i<n;i++){double a=i*Math.PI*2/n;dust(l,p.position(),new Vec3(Math.cos(a),Math.sin(a),Math.sin(a*2)).m_82490_(0.3),i/(double)n,1.2f);}
    }
    static void finishPulse(ClientLevel l,OmegaRitualEventPacket p,Vec3 at,int index){frame(p.caster(),p.session(),at,2.4+index*0.7,index/6.0,false,index%2==0?1:2);}
    static void finishHalo(ClientLevel l,OmegaRitualEventPacket p,Vec3 at,int age){
        int n=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?6:2;
        for(int i=0;i<n;i++){double a=age*0.18+i*Math.PI*2/n;Vec3 atRing=at.m_82520_(Math.cos(a)*1.15,Math.sin(a)*0.9,Math.sin(a)*0.3);dust(l,atRing,new Vec3(0,0.015,0),age/25.0+i/(double)n,1.25f);}
    }
    public static void form(ClientLevel l,LivingEntity e,long session,int age,int charge,boolean active){
        if(e.m_20182_().m_82554_(Minecraft.m_91087_().f_91074_.m_20182_())>48)return;
        Vec3 center=e.m_20191_().m_82399_();double radius=active?0.7:2.5*(1-Math.min(1,age/(double)Math.max(1,charge)))+0.3;
        int n=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?6:2;
        for(int i=0;i<n;i++){double angle=age*0.16+i*Math.PI*2/n;Vec3 point=center.m_82520_(Math.cos(angle)*radius,Math.sin(angle*1.2)*radius*0.55,Math.sin(angle)*radius);
            dust(l,point,active?new Vec3(0,0.02,0):center.m_82546_(point).m_82490_(0.14),i/(double)n+age/45.0,active?0.65f:0.9f);}
    }
    public static void burst(ClientLevel l,OmegaBurstPacket p) {
        beginTick();
        if(p.position().m_82554_(Minecraft.m_91087_().f_91074_.m_20182_())>96) return;
        double hue=p.index()*0.13+p.session()*0.07;
        int variant=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.MINIMAL?2:Math.floorMod(p.index(),3);
        frame(p.caster(),p.session(),p.position(),p.radius(),hue,OmegaConfig.FLASH.get(),variant);
        int fanCount=switch(OmegaConfig.QUALITY.get()) { case FULL->6; case REDUCED->3; case MINIMAL->2; };
        int fanIndex=0;
        for(Vec3 position:OmegaCone.points(p.position(),p.direction(),p.index(),fanCount)) {
            frame(p.caster(),p.session(),position,p.radius()*(0.55+0.08*(fanIndex%3)),hue+fanIndex*0.17,false,(variant+1+fanIndex)%3);
            fanIndex++;
        }
        if(OmegaConfig.QUALITY.get()!=OmegaConfig.Quality.MINIMAL) {
            int sparks=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?24:8;
            for(int i=0;i<sparks;i++) {
                double a=i*Math.PI*2/sparks;
                Vec3 velocity=new Vec3(Math.cos(a),Math.sin(a),Math.sin(a*2)).m_82490_(0.2);
                dust(l,p.position(),velocity,hue+i/(double)sparks,0.85f);
            }
        }
        long now=l.m_46467_(); double due=NEXT_BOMB.getOrDefault(p.caster(),(double)now);
        // Keep a fractional deadline so eight default trail nodes/second yield six sounds,
        // without rounding each interval up or replaying a backlog after a pause.
        if(burstSounds<2&&now+1e-6>=due) {
            NEXT_BOMB.put(p.caster(),Math.max(due+20.0/6.0,now+1.0)); burstSounds++;
            sound(l,p.position(),OmegaSounds.BOMB.get(),0.45f,1.0f);
        }
    }
    private static void sound(ClientLevel l,Vec3 p,SoundEvent sound,float volume,float pitch) {
        l.m_7785_(p.f_82479_,p.f_82480_,p.f_82481_,sound,SoundSource.PLAYERS,volume*OmegaConfig.VOLUME.get().floatValue(),pitch,false);
    }
    public static void clearCaster(UUID caster,long session) {
        ACTIVE.removeIf(a->{ if(a.caster.equals(caster)&&a.session<=session) { a.particle.m_107274_(); return true; } return false; });
    }
    public static void clear() { for(Active a:ACTIVE) a.particle.m_107274_(); ACTIVE.clear(); NEXT_BOMB.clear(); lastBudgetTick=-1; }
    private OmegaVisuals() {}
}

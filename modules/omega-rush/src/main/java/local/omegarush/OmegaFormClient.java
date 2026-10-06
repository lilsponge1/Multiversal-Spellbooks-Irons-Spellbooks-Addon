package local.omegarush;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
final class OmegaFormClient {
    static final class Form {
        OmegaFormPacket packet;LivingEntity entity;Vec3 velocity;long received=System.nanoTime();int sequence,age,steps;
        OmegaFlightSound sound;final ArrayDeque<OmegaFormInputPacket> pending=new ArrayDeque<>();
        Form(OmegaFormPacket p){packet=p;velocity=p.velocity();age=p.age();sequence=Math.max(0,p.accepted());}
    }
    static final Map<UUID,Form> FORMS=new HashMap<>();
    private static final Map<UUID,Long> ENDED=new HashMap<>();
    static boolean active(Entity e){Form f=FORMS.get(e.m_20148_());return f!=null&&f.packet.phase()==1;}
    static boolean owns(Entity e){Form f=FORMS.get(e.m_20148_());return f!=null&&f.packet.phase()==1&&f.packet.hover()&&!OmegaClient.owns(e);}
    static void state(OmegaFormPacket p){
        var mc=Minecraft.m_91087_();var l=mc.f_91073_;
        if(l==null||mc.f_91074_==null||!p.dimension().equals(OmegaManager.dimension(mc.f_91074_))||!OmegaMovement.finite(p.position())||!OmegaMovement.finite(p.velocity())||!Double.isFinite(p.speed())||!Double.isFinite(p.rise())||p.speed()<=0||p.speed()>1||p.rise()<=0||p.rise()>0.6)return;
        Form f=FORMS.get(p.caster());if(f!=null&&(p.session()<f.packet.session()||p.session()==f.packet.session()&&p.tick()<f.packet.tick()))return;
        if(p.phase()==2){if(p.closed())ENDED.merge(p.caster(),p.session(),Math::max);remove(p.caster());return;}
        if(p.session()<=ENDED.getOrDefault(p.caster(),-1L))return;
        if(f==null||p.session()!=f.packet.session()){remove(p.caster());f=new Form(p);FORMS.put(p.caster(),f);}
        boolean launched=f.packet.phase()==0&&p.phase()==1;f.packet=p;f.received=System.nanoTime();f.age=Math.max(f.age,p.age());f.sequence=Math.max(f.sequence,p.accepted());
        f.entity=l.m_6815_(p.entity()) instanceof LivingEntity e&&e.m_20148_().equals(p.caster())?e:null;
        if(p.reset()){f.pending.clear();OmegaEchoes.clear(p.caster());}
        if(p.phase()!=0){stopSound(f);OmegaAnimations.stopCharge(p.caster());}
        while(!f.pending.isEmpty()&&f.pending.peekFirst().sequence()<=p.accepted())f.pending.removeFirst();
        f.steps=f.pending.size();Vec3 expected=p.position(),v=p.velocity();
        if(p.hover()&&f.entity==mc.f_91074_&&!OmegaClient.owns(f.entity)){
            for(var a:f.pending){v=OmegaHover.step(v,a.yaw(),a.forward(),a.strafe(),a.vertical(),p.speed(),p.rise());expected=expected.m_82549_(v);}
            Vec3 correction=expected.m_82546_(f.entity.m_20182_());
            if(correction.m_82553_()>3){f.entity.m_6034_(p.position().f_82479_,p.position().f_82480_,p.position().f_82481_);f.pending.clear();f.steps=0;v=p.velocity();}
            else f.entity.m_6478_(MoverType.SELF,correction);
        }
        f.velocity=v;
        if(launched&&f.entity!=null)OmegaVisuals.formPulse(l,f.entity,p.session());
    }
    static boolean input(LocalPlayer p){
        Form f=FORMS.get(p.m_20148_());if(f==null||f.packet.phase()!=1||OmegaClient.owns(p))return false;
        float up=(p.f_108618_.f_108572_?1:0)-(p.f_108618_.f_108573_?1:0);
        var a=new OmegaFormInputPacket(f.packet.session(),++f.sequence,p.m_146908_(),p.m_146909_(),forward(p),strafe(p),up);
        OmegaNetwork.formInput(a);if(f.packet.hover()&&f.pending.size()<4)f.pending.addLast(a);return owns(p);
    }
    private static float forward(LocalPlayer p){return (p.f_108618_.f_108568_?1:0)-(p.f_108618_.f_108569_?1:0);}
    private static float strafe(LocalPlayer p){return (p.f_108618_.f_108570_?1:0)-(p.f_108618_.f_108571_?1:0);}
    static boolean travel(LivingEntity p){
        var mc=Minecraft.m_91087_();if(p!=mc.f_91074_||!owns(p))return false;Form f=FORMS.get(p.m_20148_());
        p.m_20242_(true);p.m_183634_();
        if(System.nanoTime()-f.received>1_000_000_000L||f.steps++>=4){p.m_20256_(Vec3.f_82478_);return true;}
        float up=(mc.f_91074_.f_108618_.f_108572_?1:0)-(mc.f_91074_.f_108618_.f_108573_?1:0);
        f.velocity=OmegaHover.step(f.velocity,p.m_146908_(),forward(mc.f_91074_),strafe(mc.f_91074_),up,f.packet.speed(),f.packet.rise());
        if(OmegaCollision.safe(p.m_9236_(),p.m_20191_().m_82369_(f.velocity)))p.m_6478_(MoverType.SELF,f.velocity);
        p.m_20256_(f.velocity);return true;
    }
    static void tick(){
        var mc=Minecraft.m_91087_();var l=mc.f_91073_;if(l==null||mc.f_91074_==null){clear();return;}
        for(Form f:new ArrayList<>(FORMS.values())){
            if(System.nanoTime()-f.received>2_000_000_000L){remove(f.packet.caster());continue;}
            var e=l.m_6815_(f.packet.entity());if(!(e instanceof LivingEntity living)||!living.m_20148_().equals(f.packet.caster()))continue;
            f.entity=living;f.age++;
            if(f.packet.phase()==0){if(living instanceof net.minecraft.client.player.AbstractClientPlayer player)OmegaAnimations.charge(player,f.packet.chargeTicks());if(f.sound==null){f.sound=new OmegaFlightSound(living);mc.m_91106_().m_120367_(f.sound);}}else{stopSound(f);OmegaAnimations.stopCharge(f.packet.caster());}
            if(!living.m_20145_())OmegaVisuals.form(l,living,f.packet.session(),f.age,f.packet.chargeTicks(),f.packet.phase()==1);
        }
    }
    private static void stopSound(Form f){if(f.sound!=null){f.sound.end();Minecraft.m_91087_().m_91106_().m_120399_(f.sound);f.sound=null;}}
    static void remove(UUID id){Form f=FORMS.remove(id);if(f==null)return;stopSound(f);OmegaEchoes.clear(id);OmegaAnimations.stopCharge(id);
        if(f.entity==Minecraft.m_91087_().f_91074_&&!OmegaClient.owns(f.entity))f.entity.m_20242_(f.packet.originalGravity());}
    static void clear(){for(UUID id:new ArrayList<>(FORMS.keySet()))remove(id);ENDED.clear();OmegaEchoes.clear();}
}

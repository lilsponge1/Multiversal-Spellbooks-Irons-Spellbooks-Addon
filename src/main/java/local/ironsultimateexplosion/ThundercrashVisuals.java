package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.effect.AscensionEffect;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import io.redspace.ironsspellbooks.particle.*;
import io.redspace.ironsspellbooks.api.util.*;
import java.util.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class ThundercrashVisuals {
    private static int budget;
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final class Burst {
        final ThundercrashImpactPacket packet; final Random random; int age;
        Burst(ThundercrashImpactPacket p) { packet = p; random = new Random(p.session()); }
    }
    static void beginTick() { budget = ThundercrashConfig.GLOBAL_BUDGET.get(); }
    private static boolean spend(int n) { if (budget < n) return false; budget -= n; return true; }
    private static void particle(ClientLevel level, ParticleOptions type, Vec3 p, Vec3 v) {
        if (spend(1)) level.m_7106_(type,p.f_82479_,p.f_82480_,p.f_82481_,v.f_82479_,v.f_82480_,v.f_82481_);
    }
    private static Vec3 offset(Random r, double scale) { return new Vec3((r.nextDouble()*2-1)*scale,(r.nextDouble()*2-1)*scale,(r.nextDouble()*2-1)*scale); }
    static void flight(ClientLevel level, ThundercrashClient.Flight f, Vec3 viewer) {
        if (f.entity == null) return;
        double distance = f.entity.m_20182_().m_82554_(viewer);
        if (distance > 96*96) return;
        int quality = ExplosionConfig.PARTICLE_QUALITY.get(), count = ThundercrashConfig.PARTICLE_BUDGET.get();
        if (distance > 48*48) count /= 3;
        if (quality < 2) count = Math.max(4, count / (quality == 0 ? 4 : 2));
        int allowance = Math.min(budget, count), startBudget = budget;
        int calls = Math.min(ThundercrashConfig.AURA.get(), Math.max(1, allowance / 5));
        if (f.packet.phase() == ThundercrashState.CHARGE) calls = Math.min(calls, 1 + f.visualAge / 3);
        for (int i=0; i<calls && budget > 1; i++) if (spend(2)) AscensionEffect.ambientParticles(level, f.entity);
        if (f.packet.phase() == ThundercrashState.CHARGE) return;
        Vec3 torso = f.entity.m_20182_().m_82520_(0, f.entity.m_20206_()*0.55, 0);
        if (!f.launchPresented) {
            f.launchPresented = true;
            for(int i=0;i<3 && startBudget-budget<allowance;i++)
                particle(level,new ZapParticleOption(torso.m_82520_((i-1)*0.4,1.6,0)),torso.m_82520_((i-1)*0.4,-0.7,0),Vec3.f_82478_);
            for(int i=0;i<6 && startBudget-budget<allowance;i++)
                particle(level,ParticleHelper.ELECTRICITY,torso.m_82549_(offset(RANDOM,0.5)),new Vec3(0,0.18,0));
        }
        Vec3 direction = f.packet.velocity().m_82556_() > 1e-5 ? f.packet.velocity().m_82541_() : f.packet.launchLook();
        Vec3 front = torso.m_82549_(direction.m_82490_(0.8));
        for(int i=0; i<4 && startBudget-budget<allowance; i++) particle(level,ParticleHelper.ELECTRICITY,front.m_82549_(offset(RANDOM,0.3)),offset(RANDOM,0.04));
        for(int i=0; i<3 && startBudget-budget<allowance; i++) particle(level,ParticleHelper.ELECTRICITY,
                torso.m_82549_(offset(RANDOM,0.4)),direction.m_82490_(-0.05).m_82549_(offset(RANDOM,0.04)));
        if (f.entity == net.minecraft.client.Minecraft.m_91087_().f_91074_) {
            trail(level,f,f.entity.m_20182_(),startBudget,allowance);
        } else while (!f.authoritativePath.isEmpty()) trail(level,f,f.authoritativePath.removeFirst(),startBudget,allowance);
        if (startBudget-budget<allowance) particle(level,ParticleHelper.ELECTRIC_SPARKS,front,offset(RANDOM,0.1));
        if (f.visualAge%8==0 && startBudget-budget<allowance)
            particle(level,new ZapParticleOption(torso),front,Vec3.f_82478_);
        f.history.addLast(torso); while(f.history.size()>12) f.history.removeFirst();
        if (f.visualAge%4==0 && f.history.size()>2 && startBudget-budget<allowance) {
            Vec3 old = f.history.peekFirst();
            if (old.m_82554_(torso)<12*12) particle(level,new ZapParticleOption(torso),old,Vec3.f_82478_);
        }
    }
    private static void trail(ClientLevel level, ThundercrashClient.Flight f, Vec3 now, int startBudget, int allowance) {
        double travelled = now.m_82546_(f.trailPosition).m_82553_();
        if (travelled > 4 || !Double.isFinite(travelled)) { f.trailPosition = now; f.history.clear(); f.spacingRemainder = 0; return; }
        Vec3 unit = now.m_82546_(f.trailPosition).m_82541_();
        double spacing = ThundercrashConfig.SPACING.get();
        for(double along=spacing-f.spacingRemainder; along<=travelled && startBudget-budget<allowance-2; along+=spacing) {
            Vec3 point = f.trailPosition.m_82549_(unit.m_82490_(along)).m_82520_(0,0.8,0);
            particle(level,ParticleHelper.ELECTRICITY,point.m_82549_(offset(RANDOM,0.2)),offset(RANDOM,0.04));
            particle(level,ParticleHelper.ELECTRICITY,point.m_82549_(offset(RANDOM,0.2)),offset(RANDOM,0.04));
        }
        f.spacingRemainder = (f.spacingRemainder+travelled)%spacing; f.trailPosition=now;
    }
    static void impact(ClientLevel level, ThundercrashImpactPacket p) {
        if (BURSTS.size() >= 16) BURSTS.remove(0);
        BURSTS.add(new Burst(p));
        CameraShakeManager.addCameraShake(new CameraShakeData(level, 5, p.position(), 20));
    }
    static void tickImpacts(ClientLevel level) {
        var viewer=net.minecraft.client.Minecraft.m_91087_().f_91074_;
        if(viewer!=null) BURSTS.sort(Comparator.comparingDouble(b -> b.packet.position().m_82554_(viewer.m_20182_())));
        int remaining=BURSTS.size();
        for(Iterator<Burst> it=BURSTS.iterator();it.hasNext();) {
            Burst b=it.next(); Vec3 p=b.packet.position(); float radius=b.packet.radius();
            int initial=budget, cap=Math.min(56,budget/Math.max(1,remaining--));
            if (b.age==0) {
                if(cap>0) particle(level,new BlastwaveParticleOptions(new Vector3f(0.65f,1f,1f),radius),p,Vec3.f_82478_);
                for(int i=0;i<12 && initial-budget<cap;i++) particle(level,new ZapParticleOption(p.m_82549_(offset(b.random,radius))),p,Vec3.f_82478_);
            }
            for(int i=0;i<16 && initial-budget<cap;i++) {
                double angle=b.random.nextDouble()*Math.PI*2, r=radius*Math.min(1,(b.age+1)/12.0);
                Vec3 point=p.m_82520_(Math.cos(angle)*r,b.random.nextDouble()*0.5,Math.sin(angle)*r);
                particle(level,ParticleHelper.ELECTRICITY,point,offset(b.random,0.1));
                if(i%4==0 && initial-budget<cap) particle(level,ParticleHelper.ELECTRIC_SPARKS,p.m_82520_(0,b.random.nextDouble()*4,0),offset(b.random,0.15));
                if(i%8==0 && initial-budget<cap) particle(level,ParticleTypes.f_123762_,point,new Vec3(0,0.05,0));
            }
            if(++b.age>=15) it.remove();
        }
    }
    static void clear() { BURSTS.clear(); }
    private ThundercrashVisuals() {}
}

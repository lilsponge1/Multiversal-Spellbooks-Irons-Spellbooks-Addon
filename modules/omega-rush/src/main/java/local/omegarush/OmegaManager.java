package local.omegarush;

import java.util.*;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class OmegaManager {
    private static final Map<UUID,OmegaState> STATES=new HashMap<>();
    private static final Map<Long,Trail> TRAILS=new HashMap<>();
    private static final Map<UUID,Landing> LANDINGS=new HashMap<>();
    private static final String RECOVERY="irons_omega_rush.originalGravity";
    private static long nextSession=1,tick;
    private static boolean handingOff;
    private record Landing(String dimension,long expires) {}
    private record Node(Vec3 point,Vec3 direction,long due,int index) {}
    private static final class Hits { int count; long last=Long.MIN_VALUE/2; }
    private static final class Trail {
        final OmegaState state;
        final ArrayDeque<Node> nodes=new ArrayDeque<>();
        final Map<UUID,Hits> hits=new HashMap<>();
        int nextIndex;
        Trail(OmegaState s) { state=s; }
    }
    public static boolean owns(Entity e) { OmegaState s=STATES.get(e.m_20148_()); return s!=null&&!s.terminal&&s.phase==OmegaState.FLIGHT; }
    public static boolean eligible(ServerPlayer p) {
        return !STATES.containsKey(p.m_20148_())&&p.m_6084_()&&!p.m_5833_()&&!p.m_20159_()&&!p.m_5803_()&&!p.m_21255_()
            &&!p.m_20069_()&&!p.m_20077_()&&!p.m_21209_()&&!p.m_21023_(MobEffectRegistry.ASCENSION.get())
            &&!p.getPersistentData().m_128441_("irons_ultimate_explosion.thundercrash.originalGravity");
    }
    public static void charge(ServerPlayer p,int rank,int duration) {
        if(!eligible(p)) return;
        OmegaState s=new OmegaState(p,nextSession++,0,duration); STATES.put(p.m_20148_(),s); broadcast(s,false);
    }
    public static boolean start(ServerPlayer p,int rank,float power) {
        OmegaState old=STATES.get(p.m_20148_());
        if(old!=null&&old.phase!=OmegaState.CHARGE||old==null&&!eligible(p)) return false;
        if(!p.m_6084_()||p.m_5833_()||p.m_20159_()||p.m_5803_()||p.m_21255_()||p.m_20069_()||p.m_20077_()) { abort(p); return false; }
        OmegaState s=new OmegaState(p,old==null?nextSession++:old.session,power,old==null?OmegaConfig.CHARGE.get():old.chargeTicks);
        s.phase=OmegaState.FLIGHT;
        p.getPersistentData().m_128379_(RECOVERY,s.originalGravity); p.m_20242_(true); p.m_183634_();
        STATES.put(p.m_20148_(),s); TRAILS.put(s.session,new Trail(s)); LANDINGS.remove(p.m_20148_()); broadcast(s,false); return true;
    }
    public static void input(ServerPlayer p,OmegaInputPacket packet) {
        OmegaState s=STATES.get(p.m_20148_());
        if(s==null||s.phase!=OmegaState.FLIGHT||s.terminal||s.session!=packet.session()||packet.sequence()<0||packet.sequence()<=s.inputSequence
            ||!Float.isFinite(packet.yaw())||!Float.isFinite(packet.pitch())||Math.abs(packet.yaw())>1e7||Math.abs(packet.pitch())>90) return;
        if(s.inputTick!=(int)tick) { s.inputTick=(int)tick; s.packetsThisTick=0; }
        if(++s.packetsThisTick>2) return;
        s.inputSequence=packet.sequence(); s.yaw=packet.yaw()%360; s.pitch=packet.pitch();
        p.m_146922_(s.yaw); p.m_146926_(s.pitch);
        if(packet.cancel()) finish(s,true,false);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return; tick++;
        for(OmegaState s:new ArrayList<>(STATES.values())) {
            try { advance(s); } catch(RuntimeException error) { System.err.println("Omega Rush controller aborted: "+error); error.printStackTrace(); finish(s,true,true); }
        }
        for(Trail trail:new ArrayList<>(TRAILS.values())) {
            OmegaState s=trail.state; ServerPlayer p=s.player;
            if(!p.m_6084_()||p.m_213877_()||!s.dimension.equals(dimension(p))) { clearTrails(p); continue; }
            try {
                while(!trail.nodes.isEmpty()&&trail.nodes.peekFirst().due<=tick) detonate(trail,trail.nodes.removeFirst());
            } catch(RuntimeException error) {
                System.err.println("Omega Rush trail cleared after controller error: "+error); error.printStackTrace(); clearTrails(p);
            }
            if(s.terminal&&trail.nodes.isEmpty()) TRAILS.remove(s.session);
        }
        LANDINGS.entrySet().removeIf(e1->e1.getValue().expires<=tick);
    }
    private static void advance(OmegaState s) {
        if(s.terminal) return;
        ServerPlayer p=s.player;
        if(!p.m_6084_()||p.m_213877_()||!s.dimension.equals(dimension(p))||p.m_5833_()||p.m_20159_()||p.m_5803_()) { finish(s,false,true); return; }
        if(s.phase==OmegaState.CHARGE) { if(++s.age>s.chargeTicks+20) finish(s,false,true); return; }
        if(p.m_20182_().m_82554_(s.position)>4) { finish(s,false,true); return; }
        if(p.m_20069_()||p.m_20077_()||p.m_21255_()) { finish(s,true,true); return; }
        if(s.age>=s.duration) { finish(s,true,false); return; }
        s.velocity=OmegaMovement.step(s.velocity,OmegaMovement.look(s.yaw,s.pitch),s.speed,s.steering);
        if(!OmegaCollision.safe(p.m_9236_(),p.m_20191_().m_82369_(s.velocity).m_82400_(1e-5))) { finish(s,true,true); return; }
        ServerLevel level=p.m_284548_();
        OmegaCollision.Hit hit=OmegaCollision.find(level,p,s.velocity,false);
        Vec3 move=hit==null?s.velocity:s.velocity.m_82490_(Math.max(0,hit.fraction()-0.001));
        s.previous=p.m_20182_(); p.m_20242_(true); p.m_183634_(); p.m_20256_(move); p.m_6478_(MoverType.SELF,move);
        s.position=p.m_20182_(); level.m_7726_().m_8385_(p); p.m_183634_(); s.age++;
        Trail trail=TRAILS.get(s.session);
        for(Vec3 point:s.path.sample(s.previous,s.position,s.spacing)) {
            if(trail.nodes.size()<16) trail.nodes.addLast(new Node(point.m_82520_(0,p.m_20206_()*0.5,0),move.m_82541_(),tick+5,trail.nextIndex++));
        }
        if(hit!=null||s.position.m_82546_(s.previous).m_82546_(move).m_82556_()>1e-5) { finish(s,true,false); return; }
        if(s.age>=s.duration) { finish(s,true,false); return; }
        if(tick%2==0) broadcast(s,false);
    }
    public static boolean canHit(LivingEntity caster,Entity target) {
        if(target==caster||!target.m_6084_()||target.m_5833_()||DamageSources.isFriendlyFireBetween(caster,target)) return false;
        if(target instanceof Player player) {
            if(!OmegaConfig.PVP.get()) return false;
            if(caster instanceof ServerPlayer p&&(!p.f_8924_.m_129799_()||!p.m_7099_(player))) return false;
        }
        return true;
    }
    private static void detonate(Trail trail,Node node) {
        OmegaState s=trail.state; ServerLevel level=s.player.m_284548_();
        AABB bounds=new AABB(node.point,node.point).m_82400_(s.radius);
        if(!OmegaCollision.safe(level,bounds)) return;
        for(Entity target:level.m_6249_(s.player,bounds,e->e instanceof LivingEntity&&canHit(s.player,e))) {
            Hits hits=trail.hits.computeIfAbsent(target.m_20148_(),id->new Hits());
            if(hits.count>=3||tick-hits.last<10) continue;
            if(OmegaCollision.closest(target.m_20191_(),node.point).m_82554_(node.point)>s.radius) continue;
            if(level.m_45547_(new ClipContext(node.point,target.m_20191_().m_82399_(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.player)).m_6662_()!=HitResult.Type.MISS) continue;
            if(DamageSources.applyDamage(target,s.damage,ModSpells.OMEGA_RUSH.get().getDamageSource(s.player))) { hits.count++; hits.last=tick; }
        }
        OmegaNetwork.burst(level,new OmegaBurstPacket(s.player.m_20148_(),s.dimension,s.session,node.index,node.point,node.direction,(float)s.radius,false));
    }
    private static void finish(OmegaState s,boolean protect,boolean clearTrail) {
        if(s.terminal) return; s.terminal=true; STATES.remove(s.player.m_20148_(),s);
        ServerPlayer p=s.player; boolean flying=s.phase==OmegaState.FLIGHT; s.phase=OmegaState.END;
        if(flying) {
            p.m_20242_(s.originalGravity); p.getPersistentData().m_128473_(RECOVERY); p.m_20256_(Vec3.f_82478_); p.m_183634_();
            if(protect&&p.m_6084_()) LANDINGS.put(p.m_20148_(),new Landing(dimension(p),tick+100));
        }
        if(clearTrail) clearTrails(p);
        broadcast(s,clearTrail);
        if(flying&&p.f_8906_!=null&&p.m_6084_()&&s.dimension.equals(dimension(p))) {
            handingOff=true;
            try { p.f_8906_.m_9774_(p.m_20185_(),p.m_20186_(),p.m_20189_(),p.m_146908_(),p.m_146909_()); } finally { handingOff=false; }
        }
    }
    private static void clearTrails(ServerPlayer p) {
        for(Trail trail:new ArrayList<>(TRAILS.values())) if(trail.state.player==p) {
            TRAILS.remove(trail.state.session);
            OmegaNetwork.burst(p.m_284548_(),new OmegaBurstPacket(p.m_20148_(),trail.state.dimension,trail.state.session,-1,trail.state.position,Vec3.f_82478_,0,true));
        }
    }
    static String dimension(Entity p) { return p.m_9236_().m_46472_().m_135782_().toString(); }
    private static void broadcast(OmegaState s,boolean clear) { OmegaNetwork.state(s.player,s.packet(tick,clear)); }
    public static void cancelCharge(ServerPlayer p) { OmegaState s=STATES.get(p.m_20148_()); if(s!=null&&s.phase==OmegaState.CHARGE) finish(s,false,true); }
    public static void abort(ServerPlayer p) { OmegaState s=STATES.get(p.m_20148_()); if(s!=null) finish(s,false,true); clearTrails(p); LANDINGS.remove(p.m_20148_()); }
    public static void externalTeleport(ServerPlayer p) { if(!handingOff) abort(p); }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        OmegaState s=STATES.get(e.getTarget().m_20148_()); if(s!=null&&e.getEntity() instanceof ServerPlayer p) OmegaNetwork.stateTo(p,s.packet(tick,false));
    }
    @SubscribeEvent public static void stoppedTracking(PlayerEvent.StopTracking e) {
        OmegaState s=STATES.get(e.getTarget().m_20148_());
        if(s!=null&&e.getEntity() instanceof ServerPlayer p) {
            var a=s.packet(tick,false);
            OmegaNetwork.stateTo(p,new OmegaStatePacket(a.caster(),a.entity(),a.dimension(),a.session(),a.tick(),OmegaState.END,0,a.age(),a.acceptedInput(),a.position(),a.previous(),a.velocity(),a.speed(),a.steering(),a.originalGravity(),false,false));
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if(e.getEntity() instanceof ServerPlayer p) abort(p); }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) abort(p); }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if(e.getEntity() instanceof ServerPlayer p) abort(p); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p&&p.getPersistentData().m_128441_(RECOVERY)) { p.m_20242_(p.getPersistentData().m_128471_(RECOVERY)); p.getPersistentData().m_128473_(RECOVERY); p.m_183634_(); }
    }
    @SubscribeEvent public static void fall(LivingFallEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            Landing l=LANDINGS.get(p.m_20148_());
            if(owns(p)||l!=null&&l.expires>tick&&l.dimension.equals(dimension(p))) { e.setCanceled(true); LANDINGS.remove(p.m_20148_()); }
        }
    }
    @SubscribeEvent public static void grounded(TickEvent.PlayerTickEvent e) { if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&!owns(p)&&p.m_20096_()) LANDINGS.remove(p.m_20148_()); }
    @SubscribeEvent public static void otherCast(SpellPreCastEvent e) { if(owns(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e) { for(OmegaState s:new ArrayList<>(STATES.values())) finish(s,false,true); TRAILS.clear(); LANDINGS.clear(); }
    private OmegaManager() {}
}

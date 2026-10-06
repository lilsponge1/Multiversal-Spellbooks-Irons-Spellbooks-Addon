package local.omegarush;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;

public final class OmegaClient {
    static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    private static final LinkedHashSet<String> BURSTS=new LinkedHashSet<>();
    private static final Map<UUID,Long> ENDED=new HashMap<>();
    static final class Flight {
        OmegaStatePacket packet;
        LivingEntity entity;
        Vec3 velocity;
        int sequence,predictedAge,visualAge;
        long received=System.nanoTime();
        OmegaFlightSound sound;
        boolean launched;
        final OmegaPrediction prediction=new OmegaPrediction();
        Flight(OmegaStatePacket p) { packet=p; velocity=p.velocity(); predictedAge=p.age(); visualAge=p.age(); }
    }
    static void register() { MinecraftForge.EVENT_BUS.register(OmegaClient.class);MinecraftForge.EVENT_BUS.register(OmegaEchoes.class); }
    public static void state(OmegaStatePacket p) {
        Minecraft mc=Minecraft.m_91087_(); ClientLevel level=mc.f_91073_;
        if(level==null||mc.f_91074_==null||!p.dimension().equals(OmegaManager.dimension(mc.f_91074_))||!OmegaMovement.finite(p.position())||!OmegaMovement.finite(p.velocity())) return;
        if(p.phase()!=OmegaState.END&&p.session()<=ENDED.getOrDefault(p.caster(),-1L)) return;
        Flight f=FLIGHTS.get(p.caster());
        if(f!=null&&(p.session()<f.packet.session()||p.session()==f.packet.session()&&p.tick()<f.packet.tick())) return;
        if(p.phase()==OmegaState.END) {
            if(p.closed()) ENDED.merge(p.caster(),p.session(),Math::max); remove(p.caster());
            if(p.clearTrail()) OmegaVisuals.clearCaster(p.caster(),p.session()); return;
        }
        if(f==null||f.packet.session()!=p.session()) { remove(p.caster()); f=new Flight(p); FLIGHTS.put(p.caster(),f); }
        byte old=f.packet.phase(); f.packet=p; f.received=System.nanoTime(); f.sequence=Math.max(f.sequence,p.acceptedInput());
        if(p.phase()!=OmegaState.CHARGE) stopSound(f);
        f.entity=level.m_6815_(p.entity()) instanceof LivingEntity living&&living.m_20148_().equals(p.caster())?living:null;
        if(f.entity==mc.f_91074_&&p.phase()==OmegaState.FLIGHT) {
            Vec3 predicted=p.position(),v=p.velocity(); int age=p.age();
            for(var i:f.prediction.acknowledge(p.acceptedInput())) {
                if(age++>=p.age()+p.remaining()) break;
                v=OmegaMovement.step(v,OmegaMovement.look(i.yaw(),i.pitch()),p.speed(),p.steering()); predicted=predicted.m_82549_(v);
            }
            Vec3 correction=predicted.m_82546_(f.entity.m_20182_());
            if(correction.m_82553_()>3) {
                f.entity.m_6034_(p.position().f_82479_,p.position().f_82480_,p.position().f_82481_); f.prediction.reset(); v=p.velocity(); age=p.age();
            } else f.entity.m_6478_(MoverType.SELF,correction);
            f.velocity=v; f.predictedAge=age;
        }
        if(old==OmegaState.CHARGE&&p.phase()==OmegaState.FLIGHT) f.visualAge=0;
    }
    public static boolean owns(Entity e) { Flight f=FLIGHTS.get(e.m_20148_()); return f!=null&&f.packet.phase()==OmegaState.FLIGHT; }
    public static boolean travel(LivingEntity player) {
        if(player!=Minecraft.m_91087_().f_91074_||!owns(player)) return false;
        Flight f=FLIGHTS.get(player.m_20148_()); var p=f.packet;
        player.m_20242_(true); player.m_183634_();
        if(System.nanoTime()-f.received>1_000_000_000L||f.predictedAge>=p.age()+p.remaining()||!f.prediction.beginStep()) { player.m_20256_(Vec3.f_82478_); return true; }
        f.velocity=OmegaMovement.step(f.velocity,OmegaMovement.look(player.m_146908_(),player.m_146909_()),p.speed(),p.steering()); f.predictedAge++;
        if(OmegaCollision.safe(player.m_9236_(),player.m_20191_().m_82369_(f.velocity))) player.m_6478_(MoverType.SELF,f.velocity);
        player.m_20256_(f.velocity); player.m_183634_(); return true;
    }
    public static boolean sendInput(LocalPlayer player) {
        Flight f=FLIGHTS.get(player.m_20148_()); if(f==null||f.packet.phase()!=OmegaState.FLIGHT) return OmegaFormClient.input(player);
        OmegaFormClient.input(player); // Reset the tap pair while Rush owns movement.
        int sequence=++f.sequence; float yaw=player.m_146908_(),pitch=Math.max(-90,Math.min(90,player.m_146909_()));
        OmegaNetwork.input(new OmegaInputPacket(f.packet.session(),sequence,yaw,pitch,player.m_6144_()));
        f.prediction.sentInput(sequence,yaw,pitch); return true;
    }
    public static void burst(OmegaBurstPacket p) {
        ClientLevel level=Minecraft.m_91087_().f_91073_;
        if(level==null||!p.dimension().equals(level.m_46472_().m_135782_().toString())) return;
        if(p.clear()) { OmegaVisuals.clearCaster(p.caster(),p.session()); return; }
        if(!OmegaMovement.finite(p.position())||p.radius()<=0||p.radius()>6) return;
        String key=p.caster()+":"+p.session()+":"+p.index(); if(!BURSTS.add(key)) return;
        if(BURSTS.size()>2048) BURSTS.remove(BURSTS.iterator().next());
        OmegaVisuals.burst(level,p);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.m_91087_(); ClientLevel level=mc.f_91073_;
        if(level==null||mc.f_91074_==null) { clear(); return; }
        OmegaVisuals.beginTick();
        OmegaFormClient.tick();
        List<Flight> flights=new ArrayList<>(FLIGHTS.values());
        flights.sort(Comparator.comparingDouble(f->f.packet.position().m_82554_(mc.f_91074_.m_20182_())));
        for(Flight f:flights) {
            if(!f.packet.dimension().equals(level.m_46472_().m_135782_().toString())||System.nanoTime()-f.received>2_000_000_000L) { remove(f.packet.caster()); continue; }
            Entity entity=level.m_6815_(f.packet.entity());
            if(!(entity instanceof LivingEntity living)||!living.m_20148_().equals(f.packet.caster())||!living.m_6084_()) {
                if(System.nanoTime()-f.received>500_000_000L) remove(f.packet.caster()); continue;
            }
            f.entity=living; f.visualAge++;
            if(f.packet.phase()==OmegaState.CHARGE&&System.nanoTime()-f.received<1_000_000_000L) {
                if(f.sound==null) { f.sound=new OmegaFlightSound(living); mc.m_91106_().m_120367_(f.sound); }
            } else stopSound(f);
            if(f.packet.phase()==OmegaState.FLIGHT) {
                if(living instanceof net.minecraft.client.player.AbstractClientPlayer player) OmegaAnimations.start(player);
                if(!f.launched) { f.launched=true; OmegaVisuals.launch(level,living); }
            }
            OmegaVisuals.flight(level,f);
        }
    }
    private static void stopSound(Flight f) { if(f.sound!=null) { f.sound.end(); Minecraft.m_91087_().m_91106_().m_120399_(f.sound); f.sound=null; } }
    private static void remove(UUID id) {
        Flight f=FLIGHTS.remove(id); if(f==null) return; stopSound(f); OmegaAnimations.stop(id);
        if(f.entity==Minecraft.m_91087_().f_91074_&&f.packet.phase()==OmegaState.FLIGHT) {
            f.entity.m_20242_(f.packet.originalGravity()); f.entity.m_183634_(); f.entity.m_20256_(Vec3.f_82478_);
        }
    }
    public static void clear() { for(UUID id:new ArrayList<>(FLIGHTS.keySet())) remove(id); BURSTS.clear(); ENDED.clear(); OmegaFormClient.clear();OmegaVisuals.clear(); OmegaAnimations.clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload e) { if(e.getLevel() instanceof ClientLevel) clear(); }
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(new net.minecraft.server.packs.resources.ResourceManagerReloadListener() {
            @Override public void m_6213_(net.minecraft.server.packs.resources.ResourceManager manager) { clear(); }
        });
    }
    private OmegaClient() {}
}

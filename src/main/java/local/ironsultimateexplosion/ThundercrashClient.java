package local.ironsultimateexplosion;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

public final class ThundercrashClient {
    static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    private static final LinkedHashSet<Long> IMPACTS = new LinkedHashSet<>();
    static final class Flight {
        ThundercrashStatePacket packet;
        LivingEntity entity;
        Vec3 velocity, trailPosition;
        double spacingRemainder;
        int sequence, predictedAge, visualAge;
        long received = System.nanoTime();
        ThundercrashFlightSound sound;
        boolean chargePlayed, launchPresented;
        final ArrayDeque<Vec3> history = new ArrayDeque<>();
        final ArrayDeque<Vec3> authoritativePath = new ArrayDeque<>();
        final ThundercrashPrediction prediction = new ThundercrashPrediction();
        Flight(ThundercrashStatePacket packet) { this.packet = packet; velocity = packet.velocity(); trailPosition = packet.previous(); predictedAge = packet.age(); }
    }
    static void register() { MinecraftForge.EVENT_BUS.register(ThundercrashClient.class); }
    public static void state(ThundercrashStatePacket p) {
        Minecraft mc = Minecraft.m_91087_(); ClientLevel level = mc.f_91073_;
        if (level == null || !p.dimension().equals(level.m_46472_().m_135782_().toString()) || !ThundercrashMovement.finite(p.position())) return;
        Flight f = FLIGHTS.get(p.caster());
        if (f != null && (p.session() < f.packet.session() || p.session() == f.packet.session() && p.tick() < f.packet.tick())) return;
        if (p.phase() == ThundercrashState.END) { remove(p.caster()); return; }
        if (f == null || f.packet.session() != p.session()) { remove(p.caster()); f = new Flight(p); FLIGHTS.put(p.caster(), f); }
        byte oldPhase = f.packet.phase(); f.packet = p; f.received = System.nanoTime();
        f.sequence = Math.max(f.sequence, p.acceptedInput());
        f.entity = level.m_6815_(p.entity()) instanceof LivingEntity living && living.m_20148_().equals(p.caster()) ? living : null;
        if (p.phase() != ThundercrashState.CHARGE && f.entity != mc.f_91074_) {
            f.authoritativePath.addLast(p.previous()); f.authoritativePath.addLast(p.position());
            while (f.authoritativePath.size() > 8) f.authoritativePath.removeFirst();
        }
        if (f.entity == mc.f_91074_ && p.phase() != ThundercrashState.CHARGE) {
            List<ThundercrashPrediction.Input> replay = f.prediction.acknowledge(p.acceptedInput());
            Vec3 predicted = p.position(), v = p.velocity(); int age = p.age();
            for (ThundercrashPrediction.Input i : replay) {
                Vec3 look = p.guided() ? ThundercrashMovement.look(i.yaw(), i.pitch()) : p.launchLook();
                v = ThundercrashMovement.step(v, look, p.speed(), p.steering(), age++ < p.launchTicks(), p.lift());
                predicted = predicted.m_82549_(v);
            }
            // Reconcile through collision-preserving movement; large corrections reset the trail.
            Vec3 correction = predicted.m_82546_(f.entity.m_20182_());
            if (correction.m_82553_() > 3) {
                f.entity.m_6034_(p.position().f_82479_,p.position().f_82480_,p.position().f_82481_);
                f.prediction.reset(); f.history.clear(); f.authoritativePath.clear(); f.trailPosition = p.position(); f.spacingRemainder = 0;
                v = p.velocity(); age = p.age();
            }
            else f.entity.m_6478_(MoverType.SELF, correction);
            f.velocity = v; f.predictedAge = age;
        }
        if (oldPhase == ThundercrashState.CHARGE && p.phase() != ThundercrashState.CHARGE) f.history.clear();
    }
    public static boolean owns(Entity e) {
        Flight f = FLIGHTS.get(e.m_20148_());
        return f != null && f.packet.phase() != ThundercrashState.CHARGE;
    }
    public static boolean travel(LivingEntity player) {
        Minecraft mc = Minecraft.m_91087_();
        if (player != mc.f_91074_ || !owns(player)) return false;
        Flight f = FLIGHTS.get(player.m_20148_()); var p = f.packet;
        player.m_20242_(true); player.m_183634_();
        if (System.nanoTime() - f.received > 1_000_000_000L || !f.prediction.beginStep()) { player.m_20256_(Vec3.f_82478_); return true; }
        Vec3 look = p.guided() ? ThundercrashMovement.look(player.m_146908_(), player.m_146909_()) : p.launchLook();
        f.velocity = ThundercrashMovement.step(f.velocity, look, p.speed(), p.steering(), f.predictedAge++ < p.launchTicks(), p.lift());
        if (ThundercrashCollision.safe(player.m_9236_(), player.m_20191_().m_82369_(f.velocity))) player.m_6478_(MoverType.SELF, f.velocity);
        player.m_20256_(f.velocity); player.m_183634_();
        return true;
    }
    public static boolean sendInput(LocalPlayer player) {
        Flight f = FLIGHTS.get(player.m_20148_());
        if (f == null || f.packet.phase() == ThundercrashState.CHARGE) return false;
        int sequence = ++f.sequence;
        float yaw = player.m_146908_(), pitch = Math.max(-90, Math.min(90, player.m_146909_()));
        ExplosionNetwork.input(new ThundercrashInputPacket(f.packet.session(), sequence, yaw, pitch));
        f.prediction.sentInput(sequence, yaw, pitch);
        return true;
    }
    public static void impact(ThundercrashImpactPacket p) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null || !p.dimension().equals(level.m_46472_().m_135782_().toString()) || !IMPACTS.add(p.session())) return;
        if (IMPACTS.size() > 128) IMPACTS.remove(IMPACTS.iterator().next());
        Flight active = FLIGHTS.get(p.caster());
        if (active != null && active.packet.session() == p.session()) remove(p.caster());
        ThundercrashVisuals.impact(level, p);
        level.m_7785_(p.position().f_82479_,p.position().f_82480_,p.position().f_82481_, ModSounds.IMPACT.get(), SoundSource.PLAYERS,
                ThundercrashConfig.IMPACT_VOLUME.get().floatValue(), 1, false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.m_91087_(); ClientLevel level = mc.f_91073_;
        if (level == null || mc.f_91074_ == null) { clear(); return; }
        ThundercrashVisuals.beginTick();
        ThundercrashVisuals.tickImpacts(level);
        // Nearby casters get the global particle budget first.
        List<Flight> flights = new ArrayList<>(FLIGHTS.values());
        flights.sort(Comparator.comparingDouble(f -> f.packet.position().m_82554_(mc.f_91074_.m_20182_())));
        for (Flight f : flights) {
            if (!f.packet.dimension().equals(level.m_46472_().m_135782_().toString()) ||
                    f.entity != mc.f_91074_ && System.nanoTime() - f.received > 5_000_000_000L) { remove(f.packet.caster()); continue; }
            Entity entity = level.m_6815_(f.packet.entity());
            if (!(entity instanceof LivingEntity living) || !living.m_20148_().equals(f.packet.caster()) || !living.m_6084_()) {
                if (System.nanoTime() - f.received > 500_000_000L) remove(f.packet.caster()); continue;
            }
            f.entity = living; f.visualAge++;
            if (!f.chargePlayed) {
                f.chargePlayed = true;
                if (f.packet.phase() == ThundercrashState.CHARGE) level.m_7785_(living.m_20185_(),living.m_20186_(),living.m_20189_(), ModSounds.CAST.get(), SoundSource.PLAYERS, ThundercrashConfig.CAST_VOLUME.get().floatValue(),1,false);
            }
            if (f.packet.phase() != ThundercrashState.CHARGE) {
                if (living instanceof AbstractClientPlayer p) ThundercrashAnimations.start(p);
                if (f.sound == null && System.nanoTime() - f.received < 1_000_000_000L) {
                    f.sound = new ThundercrashFlightSound(living); mc.m_91106_().m_120367_(f.sound);
                }
                if (System.nanoTime() - f.received > 1_000_000_000L) stopSound(f);
            }
            ThundercrashVisuals.flight(level, f, mc.f_91074_.m_20182_());
        }
    }
    private static void stopSound(Flight f) { if (f.sound != null) { f.sound.end(); Minecraft.m_91087_().m_91106_().m_120399_(f.sound); f.sound = null; } }
    private static void remove(UUID id) {
        Flight f = FLIGHTS.remove(id); if (f == null) return;
        stopSound(f); ThundercrashAnimations.stop(id);
        if (f.entity == Minecraft.m_91087_().f_91074_ && f.packet.phase() != ThundercrashState.CHARGE) {
            f.entity.m_20242_(f.packet.originalGravity()); f.entity.m_183634_(); f.entity.m_20256_(Vec3.f_82478_);
        }
    }
    public static void clear() { for (UUID id : new ArrayList<>(FLIGHTS.keySet())) remove(id); IMPACTS.clear(); ThundercrashAnimations.clear(); ThundercrashVisuals.clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload e) { if (e.getLevel() instanceof ClientLevel) clear(); }
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(new net.minecraft.server.packs.resources.ResourceManagerReloadListener() {
            @Override public void m_6213_(net.minecraft.server.packs.resources.ResourceManager manager) { clear(); }
        });
    }
    private ThundercrashClient() {}
}

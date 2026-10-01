package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/** Server-owned sessions; vanilla player movement packets have no position authority during flight. */
public final class ThundercrashManager {
    private static final Map<UUID, ThundercrashState> STATES = new HashMap<>();
    private static final Map<UUID, Landing> LANDINGS = new HashMap<>();
    private static final String RECOVERY = "irons_ultimate_explosion.thundercrash.originalGravity";
    private static long nextSession = 1, tick;
    private static boolean handingOff;
    private record Landing(String dimension, long expires) {}
    public static boolean owns(Entity e) {
        ThundercrashState s = STATES.get(e.m_20148_());
        return s != null && !s.terminal && s.phase != ThundercrashState.CHARGE;
    }
    public static boolean eligible(ServerPlayer p) {
        return !STATES.containsKey(p.m_20148_()) && p.m_6084_() && !p.m_5833_() && !p.m_20159_()
                && !p.m_5803_() && !p.m_21255_() && !p.m_20069_() && !p.m_20077_()
                && !p.m_21023_(MobEffectRegistry.ASCENSION.get()) && !p.m_21209_();
    }
    public static void charge(ServerPlayer p) {
        charge(p, 1);
    }
    public static void charge(ServerPlayer p, int spellLevel) {
        if (!eligible(p)) return;
        ThundercrashState s = new ThundercrashState(p, nextSession++, spellLevel, 0);
        STATES.put(p.m_20148_(), s); broadcast(s);
    }
    public static boolean start(ServerPlayer p, float power) {
        return start(p, 1, power);
    }
    public static boolean start(ServerPlayer p, int spellLevel, float power) {
        ThundercrashState old = STATES.get(p.m_20148_());
        if (old != null && old.phase != ThundercrashState.CHARGE) return false;
        if (old == null && !eligible(p)) return false;
        if (!p.m_6084_() || p.m_5833_() || p.m_20159_() || p.m_5803_() || p.m_21255_()) { abort(p); return false; }
        ThundercrashState s = new ThundercrashState(p, old == null ? nextSession++ : old.session, spellLevel, power);
        s.phase = ThundercrashState.LAUNCH;
        p.getPersistentData().m_128379_(RECOVERY, s.originalGravity);
        p.m_20242_(true); p.m_183634_();
        STATES.put(p.m_20148_(), s); LANDINGS.remove(p.m_20148_()); broadcast(s);
        return true;
    }
    public static void input(ServerPlayer p, ThundercrashInputPacket packet) {
        ThundercrashState s = STATES.get(p.m_20148_());
        if (s == null || s.phase == ThundercrashState.CHARGE || s.session != packet.session() || s.terminal ||
                !Float.isFinite(packet.yaw()) || !Float.isFinite(packet.pitch()) || packet.sequence() < 0 ||
                packet.sequence() <= s.inputSequence || Math.abs(packet.yaw()) > 1e7 || Math.abs(packet.pitch()) > 90) return;
        if (s.inputTick != (int)tick) { s.packetsThisTick = 0; s.inputTick = (int)tick; }
        if (++s.packetsThisTick > 2) return;
        s.inputSequence = packet.sequence();
        s.yaw = packet.yaw() % 360; s.pitch = packet.pitch();
        p.m_146922_(s.yaw); p.m_146926_(s.pitch);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        for (ThundercrashState s : new ArrayList<>(STATES.values())) {
            try { advance(s); } catch (RuntimeException error) {
                System.err.println("Thundercrash aborted after controller error: " + error);
                error.printStackTrace(System.err);
                finish(s, null, true);
            }
        }
        LANDINGS.entrySet().removeIf(e -> e.getValue().expires <= tick);
    }
    private static void advance(ThundercrashState s) {
        if (s.terminal) return;
        ServerPlayer p = s.player;
        if (!p.m_6084_() || p.m_213877_() || !s.dimension.equals(dimension(p)) || p.m_5833_() || p.m_20159_() || p.m_5803_()) {
            finish(s, null, false); return;
        }
        if (s.phase == ThundercrashState.CHARGE) {
            if (++s.age > ThundercrashConfig.CHARGE.get() + 20) finish(s, null, false);
            return;
        }
        if (p.m_20069_() || p.m_20077_() || p.m_21255_() ||
                p.m_20182_().m_82554_(s.position) > 4 || s.age >= s.launchTicks + s.duration) { finish(s, null, true); return; }
        byte phase = s.age < s.launchTicks ? ThundercrashState.LAUNCH : ThundercrashState.FLIGHT;
        boolean changed = phase != s.phase; s.phase = phase;
        Vec3 look = s.guided ? ThundercrashMovement.look(s.yaw, s.pitch) : s.launchLook;
        s.velocity = ThundercrashMovement.step(s.velocity, look, s.speed, s.steering, phase == ThundercrashState.LAUNCH, s.lift);
        if (!s.guided) {
            double remaining = s.range - s.travelled, length = s.velocity.m_82553_();
            if (remaining <= 1e-6) { finish(s, null, true); return; }
            if (length > remaining) s.velocity = s.velocity.m_82490_(remaining / length);
        }
        if (!ThundercrashMovement.finite(s.velocity) || !ThundercrashCollision.safe(p.m_9236_(), p.m_20191_().m_82369_(s.velocity).m_82400_(1e-5))) {
            finish(s, null, true); return;
        }
        ServerLevel level = p.m_284548_();
        ThundercrashCollision.Hit hit = ThundercrashCollision.find(level, p, s.velocity, true);
        Vec3 move = hit == null ? s.velocity : s.velocity.m_82490_(Math.max(0, hit.fraction() - 0.001));
        // Installed movement mods can carry the body slightly between controller ticks.
        // Compare actual movement with this step's starting position, not the prior snapshot.
        s.previous = p.m_20182_();
        p.m_20242_(true); p.m_183634_(); p.m_20256_(move);
        p.m_6478_(MoverType.SELF, move);
        s.position = p.m_20182_(); s.travelled += s.position.m_82546_(s.previous).m_82553_();
        level.m_7726_().m_8385_(p);
        p.m_183634_(); s.age++;
        if (hit != null) { finish(s, hit, true); return; }
        if (s.position.m_82546_(s.previous).m_82546_(move).m_82556_() > 1e-5) {
            finish(s, new ThundercrashCollision.Hit(0, p.m_20191_().m_82399_(), null), true); return;
        }
        if (!s.guided && s.travelled >= s.range - 1e-6) { finish(s, null, true); return; }
        if (changed || tick % 2 == 0) broadcast(s);
    }
    public static boolean canHit(LivingEntity caster, Entity target) {
        if (target == caster || !target.m_6084_() || target.m_5833_() || DamageSources.isFriendlyFireBetween(caster, target)) return false;
        if (target instanceof Player player) {
            if (!ThundercrashConfig.PVP.get()) return false;
            if (caster instanceof ServerPlayer p && (!p.f_8924_.m_129799_() || !p.m_7099_(player))) return false;
        }
        return true;
    }
    private static void finish(ThundercrashState s, ThundercrashCollision.Hit hit, boolean protect) {
        if (s.terminal) return;
        s.terminal = true;
        STATES.remove(s.player.m_20148_(), s);
        ServerPlayer p = s.player;
        boolean flying = s.phase != ThundercrashState.CHARGE;
        s.phase = ThundercrashState.END;
        // Restore first; spell-damage callbacks cannot leave gravity or movement ownership behind.
        if (flying) {
            p.m_20242_(s.originalGravity); p.getPersistentData().m_128473_(RECOVERY);
            p.m_20256_(Vec3.f_82478_); p.m_183634_();
            if (protect && p.m_6084_()) LANDINGS.put(p.m_20148_(), new Landing(dimension(p), tick + ThundercrashConfig.LANDING.get()));
        }
        try {
            if (hit != null && p.m_6084_() && s.dimension.equals(dimension(p))) detonate(s, hit);
        } finally {
            broadcast(s);
            if (flying && p.f_8906_ != null && p.m_6084_() && s.dimension.equals(dimension(p))) {
                handingOff = true;
                try { p.f_8906_.m_9774_(p.m_20185_(), p.m_20186_(), p.m_20189_(), p.m_146908_(), p.m_146909_()); }
                finally { handingOff = false; }
            }
        }
    }
    private static void detonate(ThundercrashState s, ThundercrashCollision.Hit hit) {
        ServerLevel level = s.player.m_284548_();
        Vec3 point = hit.point(), center = s.player.m_20191_().m_82399_();
        // Cast visibility from the discharge, displaced just onto the clear side of its contact surface.
        Vec3 origin = point.m_82549_(center.m_82546_(point).m_82541_().m_82490_(0.02));
        AABB area = new AABB(point, point).m_82400_(s.radius);
        for (Entity e : level.m_6249_(s.player, area, e -> e instanceof LivingEntity && canHit(s.player, e))) {
            Vec3 target = ThundercrashCollision.closest(e.m_20191_(), point);
            double distance = target.m_82546_(point).m_82553_();
            if (distance > s.radius) continue;
            if (e != hit.target() && level.m_45547_(new ClipContext(origin, e.m_20191_().m_82399_(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, s.player)).m_6662_() != HitResult.Type.MISS) continue;
            float damage = (float)(s.damage * (1 - (1 - s.edgeDamage) * distance / s.radius));
            if (DamageSources.applyDamage(e, damage, ModSpells.THUNDERCRASH.get().getDamageSource(s.player))) {
                LivingEntity living = (LivingEntity)e;
                double dx = e.m_20185_() - origin.f_82479_, dz = e.m_20189_() - origin.f_82481_;
                if (dx * dx + dz * dz < 1e-8) { dx = s.launchLook.f_82479_; dz = s.launchLook.f_82481_; }
                living.m_147240_(s.knockback * Math.max(0.25, 1 - distance / s.radius), -dx, -dz);
                e.f_19864_ = true;
            }
        }
        ExplosionNetwork.impact(level, new ThundercrashImpactPacket(s.player.m_20148_(), s.dimension, s.session, point, (float)s.radius));
    }
    private static String dimension(Entity p) { return p.m_9236_().m_46472_().m_135782_().toString(); }
    private static void broadcast(ThundercrashState s) { ExplosionNetwork.state(s.player, s.packet(tick)); }
    public static void cancelCharge(ServerPlayer p) {
        ThundercrashState s = STATES.get(p.m_20148_());
        if (s != null && s.phase == ThundercrashState.CHARGE) finish(s, null, false);
    }
    public static void abort(ServerPlayer p) { ThundercrashState s = STATES.get(p.m_20148_()); if (s != null) finish(s, null, true); }
    public static void externalTeleport(ServerPlayer p) { if (!handingOff) abort(p); }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        ThundercrashState s = STATES.get(e.getTarget().m_20148_());
        if (s != null && e.getEntity() instanceof ServerPlayer p) ExplosionNetwork.stateTo(p, s.packet(tick));
    }
    @SubscribeEvent public static void stoppedTracking(PlayerEvent.StopTracking e) {
        ThundercrashState s = STATES.get(e.getTarget().m_20148_());
        if (s != null && e.getEntity() instanceof ServerPlayer p) {
            ThundercrashStatePacket a = s.packet(tick);
            ExplosionNetwork.stateTo(p, new ThundercrashStatePacket(a.caster(), a.entity(), a.dimension(), a.session(), tick, ThundercrashState.END,
                    0,a.age(),a.acceptedInput(),a.position(),a.previous(),a.velocity(),a.launchLook(),a.speed(),a.steering(),a.lift(),a.launchTicks(),a.guided(),a.originalGravity()));
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) { abort(p); LANDINGS.remove(p.m_20148_()); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) { abort(p); LANDINGS.remove(p.m_20148_()); } }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) { abort(p); LANDINGS.remove(p.m_20148_()); } }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().m_128441_(RECOVERY)) {
            p.m_20242_(p.getPersistentData().m_128471_(RECOVERY)); p.getPersistentData().m_128473_(RECOVERY); p.m_183634_();
        }
    }
    @SubscribeEvent public static void fall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Landing l = LANDINGS.get(p.m_20148_());
            if (owns(p) || l != null && l.expires > tick && l.dimension.equals(dimension(p))) { e.setCanceled(true); LANDINGS.remove(p.m_20148_()); }
        }
    }
    @SubscribeEvent public static void grounded(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p && !owns(p) && p.m_20096_()) LANDINGS.remove(p.m_20148_());
    }
    @SubscribeEvent public static void otherCast(SpellPreCastEvent e) {
        if (owns(e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e) {
        for (ThundercrashState s : new ArrayList<>(STATES.values())) finish(s, null, false);
        LANDINGS.clear();
    }
    private ThundercrashManager() {}
}

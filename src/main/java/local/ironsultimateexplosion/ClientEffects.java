package local.ironsultimateexplosion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

public final class ClientEffects {
    private static final List<Effect> CHARGES = new ArrayList<>();
    private static final List<Effect> BLASTS = new ArrayList<>();
    private static final DustParticleOptions[] CHARGE_COLORS = {
            new DustParticleOptions(new Vector3f(0.98f, 0.07f, 0.57f), 1.8f),
            new DustParticleOptions(new Vector3f(0.29f, 1.0f, 0.26f), 1.8f),
            new DustParticleOptions(new Vector3f(0.12f, 0.82f, 1.0f), 1.8f),
            new DustParticleOptions(new Vector3f(1.0f, 0.17f, 0.09f), 1.8f)
    };

    static void register() { MinecraftForge.EVENT_BUS.register(ClientEffects.class); }

    static void add(ExplosionPacket packet) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null || !level.m_46472_().m_135782_().toString().equals(packet.dimension())) return;
        if (packet.phase() == ExplosionPacket.CHARGE) {
            CHARGES.removeIf(effect -> effect.packet.seed() == packet.seed());
            if (CHARGES.size() >= 8) CHARGES.remove(0);
            CHARGES.add(new Effect(packet));
        } else if (packet.phase() == ExplosionPacket.CANCEL) {
            CHARGES.removeIf(effect -> effect.packet.seed() == packet.seed());
        } else if (packet.phase() == ExplosionPacket.DETONATE) {
            CHARGES.removeIf(effect -> effect.packet.seed() == packet.seed());
            boolean ballistix = ModList.get().isLoaded("ballistix");
            if (BLASTS.size() >= (ballistix ? 2 : 8)) BLASTS.remove(0);
            BLASTS.add(new Effect(packet, ballistix ? new BallistixVisuals(level, packet) : null));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) { CHARGES.clear(); BLASTS.clear(); return; }
        Vec3 viewer = minecraft.f_91074_.m_20182_();
        for (Iterator<Effect> it = CHARGES.iterator(); it.hasNext();) {
            Effect charge = it.next();
            if (expired(level, charge, Math.max(1, charge.packet.duration()) + 5)) { it.remove(); continue; }
            renderCharge(level, viewer, charge);
        }
        for (Iterator<Effect> it = BLASTS.iterator(); it.hasNext();) {
            Effect blast = it.next();
            if (expired(level, blast, blast.ballistix == null ? 100 : Math.min(1500, blast.packet.duration()))) {
                it.remove(); continue;
            }
            if (blast.ballistix == null) renderBlast(level, viewer, blast);
            else blast.ballistix.tick(blast.age);
        }
    }

    private static boolean expired(ClientLevel level, Effect effect, int lifetime) {
        return !level.m_46472_().m_135782_().toString().equals(effect.packet.dimension()) || effect.age++ > lifetime;
    }

    private static int density(Vec3 viewer, ExplosionPacket packet, int near) {
        double dx = viewer.f_82479_ - packet.x(), dy = viewer.f_82480_ - packet.y(), dz = viewer.f_82481_ - packet.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = distance < 48 ? near : distance < 96 ? Math.max(3, near / 3) : 2;
        return switch (ExplosionConfig.PARTICLE_QUALITY.get()) {
            case 0 -> Math.max(1, count / 4);
            case 1 -> Math.max(1, count / 2);
            default -> count;
        };
    }

    private static void renderCharge(ClientLevel level, Vec3 viewer, Effect charge) {
        ExplosionPacket p = charge.packet;
        Random random = charge.random;
        double progress = Math.min(1.0, charge.age / (double)Math.max(1, p.duration()));
        double ring = 6.0 - 4.5 * progress;
        int count = density(viewer, p, 16);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = ring * (0.8 + 0.4 * random.nextDouble());
            double x = p.x() + Math.cos(angle) * radius;
            double z = p.z() + Math.sin(angle) * radius;
            double y = p.y() + 0.2 + random.nextDouble() * (1.0 + 2.0 * progress);
            Minecraft.m_91087_().f_91061_.m_107370_(CHARGE_COLORS[(i + charge.age / 4) % CHARGE_COLORS.length],
                    x, y, z, -Math.cos(angle) * 0.08, 0.02 + 0.04 * progress, -Math.sin(angle) * 0.08);
            if (i % 4 == 0) Minecraft.m_91087_().f_91061_.m_107370_(ParticleTypes.f_123809_,
                    x, y + 0.3, z, -Math.cos(angle) * 0.05, 0.02, -Math.sin(angle) * 0.05);
        }
        int streams = density(viewer, p, 8);
        for (int i = 0; i < streams; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 8.0 + random.nextDouble() * 3.0;
            double dx = Math.cos(angle) * radius, dz = Math.sin(angle) * radius;
            double dy = (random.nextDouble() - 0.5) * 8.0;
            Minecraft.m_91087_().f_91061_.m_107370_(CHARGE_COLORS[(i + charge.age) % CHARGE_COLORS.length],
                    p.x() + dx, p.y() + dy, p.z() + dz, -dx * 0.07, -dy * 0.07, -dz * 0.07);
        }
    }

    private static void renderBlast(ClientLevel level, Vec3 viewer, Effect blast) {
        ExplosionPacket p = blast.packet;
        Random random = blast.random;
        int age = blast.age;
        if (age == 1) {
            level.m_7107_(ParticleTypes.f_123747_, p.x(), p.y(), p.z(), 0, 0, 0);
            level.m_7107_(ParticleTypes.f_123812_, p.x(), p.y(), p.z(), 0, 0, 0);
        }
        int count = density(viewer, p, 18);
        double wave = Math.min(p.radius(), age * p.radius() / 30.0);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            if (age <= 32) {
                double radius = wave * (0.88 + 0.24 * random.nextDouble());
                level.m_7107_(age < 22 ? ParticleTypes.f_123744_ : ParticleTypes.f_123762_,
                        p.x() + Math.cos(angle) * radius, p.y() + random.nextDouble() * 3,
                        p.z() + Math.sin(angle) * radius, Math.cos(angle) * 0.08, 0.05, Math.sin(angle) * 0.08);
            }
            if (age <= 75) {
                double stemHeight = Math.min(19.0, age * 0.7);
                double height = random.nextDouble() * stemHeight;
                double stemRadius = 1.5 + 2.5 * (height / 19.0);
                level.m_7107_(ParticleTypes.f_123762_,
                        p.x() + Math.cos(angle) * stemRadius * random.nextDouble(), p.y() + height,
                        p.z() + Math.sin(angle) * stemRadius * random.nextDouble(), 0, 0.12, 0);
            }
            if (age >= 12) {
                double growth = Math.min(1.0, (age - 12) / 28.0);
                double capRadius = (3 + 13 * growth) * Math.sqrt(random.nextDouble());
                double capY = p.y() + 18 + 5 * (1 - capRadius / 16) + random.nextDouble() * 2;
                level.m_7107_(i % 5 == 0 && age < 40 ? ParticleTypes.f_123744_ : ParticleTypes.f_123762_,
                        p.x() + Math.cos(angle) * capRadius, capY,
                        p.z() + Math.sin(angle) * capRadius, Math.cos(angle) * 0.02, 0.05, Math.sin(angle) * 0.02);
            }
        }
    }

    private static final class Effect {
        final ExplosionPacket packet;
        final Random random;
        final BallistixVisuals ballistix;
        int age;
        Effect(ExplosionPacket packet) { this(packet, null); }
        Effect(ExplosionPacket packet, BallistixVisuals ballistix) {
            this.packet = packet;
            this.random = new Random(packet.seed());
            this.ballistix = ballistix;
        }
    }

    private ClientEffects() {}
}

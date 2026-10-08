package local.ironsultimateexplosion;

import com.mojang.blaze3d.vertex.*;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Random;

/** Soft rising flame sheets and small, body-relative electrical arcs. */
public final class SaiyanVisuals {
    private static final Random RANDOM = new Random();
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, .78f, .08f), 1.1f);
    private static final DustParticleOptions PALE = new DustParticleOptions(new Vector3f(1f, .96f, .5f), .8f);
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(.55f,.42f,.24f),.85f);
    private static final RenderType AURA = SaiyanRenderType.energy(new ResourceLocation(GrandExplosionMod.ID, "textures/entity/saiyan_energy.png"));
    private static final float[] FLAME_EDGES = new float[5];
    static {
        for (int column = 0; column < FLAME_EDGES.length; column++)
            FLAME_EDGES[column] = (float)Math.pow(Math.max(0, Math.cos((-1 + column * .5) * Math.PI / 2)), 1.5);
    }
    private record FlameRow(double radius, double y, double curl, double width,
            float ends, float warm, float blue, float v) {}
    private static int budget;
    static void beginTick() { budget = SaiyanConfig.BUDGET.get(); }
    private static void particle(ParticleOptions type, Vec3 p, Vec3 velocity, int life) {
        if (budget <= 0) return; budget--;
        var q = Minecraft.m_91087_().f_91061_.m_107370_(type, p.f_82479_, p.f_82480_, p.f_82481_, velocity.f_82479_, velocity.f_82480_, velocity.f_82481_);
        if (q != null && type instanceof DustParticleOptions) { q.m_172260_(velocity.f_82479_, velocity.f_82480_, velocity.f_82481_); q.m_107257_(life); }
        if (q != null && type == ParticleRegistry.ELECTRICITY_PARTICLE.get()) { q.m_6569_(.45f); q.m_107257_(5); }
    }
    static void particles(SaiyanClient.Visual v) {
        Minecraft mc = Minecraft.m_91087_(); Vec3 p = v.entity.m_20182_();
        double distance = p.m_82554_(mc.f_91074_.m_20182_());
        if (distance > 64 * 64 || v.entity.m_20145_()) return;
        int tier = v.tier(); boolean charging = v.packet.target() != 0;
        double density = SaiyanConfig.DENSITY.get() * (distance > 32 * 32 ? .3 : 1);
        int quality = ExplosionConfig.PARTICLE_QUALITY.get(); density *= quality == 0 ? .25 : quality == 1 ? .5 : 1;
        if (density <= 0 || v.visualAge % 2 != 0) return;
        if(tier==3) density*=SaiyanConfig.SSJ3_PARTICLES.get();
        double size = tier == 3 ? 1.7 : tier == 2 ? 1.1 : .7;
        int count = Math.min(budget, (int)Math.ceil((tier == 3 ? 6 : tier == 2 ? 4 : 2) * density * (charging ? .3 + v.charge() * 1.2 : v.calm())));
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2, r = size * (.65 + RANDOM.nextDouble() * .4);
            Vec3 start = p.m_82520_(Math.cos(angle) * r, RANDOM.nextDouble() * .5, Math.sin(angle) * r);
            Vec3 velocity = new Vec3(-Math.cos(angle) * .012, tier == 3 ? .19 : tier == 2 ? .12 : .08, -Math.sin(angle) * .012);
            if(tier==3 && charging) velocity=new Vec3(-Math.sin(angle)*.11+Math.cos(angle)*v.charge()*.05,.12+.14*v.charge(),Math.cos(angle)*.11+Math.sin(angle)*v.charge()*.05);
            if (v.fade >= 0) { start = p.m_82520_(Math.cos(angle) * size * (1 - v.fade / 12.0), .3 + RANDOM.nextDouble() * 1.6, Math.sin(angle) * size * (1 - v.fade / 12.0)); velocity = p.m_82520_(0, 1, 0).m_82546_(start).m_82490_(.16); }
            particle(i % 3 == 0 ? PALE : GOLD, start, velocity, 12 + RANDOM.nextInt(8));
        }
        if (v.fade < 0) {
            if (tier >= 2 && v.visualAge % (tier==3?4:6) == 0) {
                double a = RANDOM.nextDouble() * Math.PI * 2;
                particle(ParticleRegistry.ELECTRICITY_PARTICLE.get(), p.m_82520_(Math.cos(a) * .72, .35 + RANDOM.nextDouble() * 1.35, Math.sin(a) * .72), Vec3.f_82478_, 5);
            }
            if (v.visualAge % 6 == 0) for (int i = 0; i < 2; i++) particle(PALE, p.m_82520_(i == 0 ? -.4 : .4, .1, 0), new Vec3(0, .065, 0), 8);
            if (tier >= 2 && v.entity.m_20142_() && (tier!=3 || SaiyanConfig.SSJ3_COMBAT.get())) {
                if (v.last != null && v.last.m_82554_(p) < 4) for (int i = 1; i <= 3; i++) particle(GOLD, p.m_82549_(v.last.m_82546_(p).m_82490_(i / 3.0)).m_82520_(0, .4, 0), new Vec3(0, .025, 0), 8);
            }
        }
        if(tier==3 && charging && v.fade<0 && v.charge()>.2f && v.visualAge%6==0) for(int i=0;i<3;i++) {
            double a=RANDOM.nextDouble()*Math.PI*2;
            particle(DUST,p.m_82520_(Math.cos(a)*1.2,.05,Math.sin(a)*1.2),new Vec3(Math.cos(a)*.06,.04,Math.sin(a)*.06),12);
        }
        v.last = p;
    }
    static void combat(SaiyanCombatPacket packet) {
        var mc=Minecraft.m_91087_();
        if(!SaiyanConfig.SSJ3_COMBAT.get() || mc.f_91073_==null || mc.f_91074_==null || packet.kind()<0 || packet.kind()>3
                || !packet.dimension().equals(mc.f_91073_.m_46472_().m_135782_().toString()) || packet.position().m_82554_(mc.f_91074_.m_20182_())>48*48) return;
        var v=SaiyanClient.STATES.get(packet.player()); if(v==null || v.packet.form()!=3) return;
        if(!Double.isFinite(packet.position().f_82479_) || !Double.isFinite(packet.position().f_82480_) || !Double.isFinite(packet.position().f_82481_)) return;
        int count=packet.kind()==SaiyanCombatPacket.HIT?12:8;
        for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count;
            particle(i%3==0?PALE:GOLD,packet.position(),new Vec3(Math.cos(a)*.11,.025+RANDOM.nextDouble()*.065,Math.sin(a)*.11),8);
        }
        particle(ParticleRegistry.ELECTRICITY_PARTICLE.get(),packet.position(),Vec3.f_82478_,5);
        if(packet.kind()==SaiyanCombatPacket.HIT && v.visualAge%3==0) mc.f_91073_.m_7785_(packet.position().f_82479_,packet.position().f_82480_,packet.position().f_82481_,ModSounds.SAIYAN_CRACK.get(),net.minecraft.sounds.SoundSource.PLAYERS,.22f*v.packet.volume(),1.45f,false);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        // Solas replaces the placeholder sky during its deferred pass. Draw afterwards,
        // once translucent terrain has begun, so soft energy blends over the finished sky.
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.m_91087_(); if (mc.f_91073_ == null || mc.f_91074_ == null) return;
        var buffers = mc.m_91269_().m_110104_(); VertexConsumer out = buffers.m_6299_(AURA);
        Vec3 camera = e.getCamera().m_90583_(); int rendered = 0;
        for (SaiyanClient.Visual v : SaiyanClient.STATES.values()) {
            if (v.entity == null || v.entity.m_20145_() || v.entity.m_20182_().m_82554_(camera) > 64 * 64 || rendered++ >= 16) continue;
            // Avoid obstructing the caster's first-person view; all other players see the full shape.
            if (v.entity == mc.f_91074_ && mc.f_91066_.m_92176_().m_90612_()) continue;
            float dt = e.getPartialTick();
            Vec3 p = new Vec3(v.entity.f_19790_ + (v.entity.m_20185_() - v.entity.f_19790_) * dt,
                    v.entity.f_19791_ + (v.entity.m_20186_() - v.entity.f_19791_) * dt,
                    v.entity.f_19792_ + (v.entity.m_20189_() - v.entity.f_19792_) * dt).m_82546_(camera);
            Matrix4f matrix = new Matrix4f(e.getPoseStack().m_85850_().m_252922_()).translate((float)p.f_82479_, (float)p.f_82480_, (float)p.f_82481_);
            double time = v.visualAge + dt; int tier = v.tier();
            float strength = v.fade >= 0 ? Math.max(0, 1 - (v.fade + dt) / 12f) : v.packet.target() != 0 ? (v.packet.form() == 0 ? .15f + .85f * v.charge() : 1) : v.calm();
            if (strength <= .001f) continue;
            double third=tier==3?(v.packet.target()==3?Math.max(0,Math.min(1,v.charge()*1.25)):1):0;
            if(tier==3) strength*=SaiyanConfig.SSJ3_AURA.get().floatValue();
            double radius = (tier == 3 ? 1.25+.95*third : tier == 2 ? 1.25 : 1.05) * strength;
            if (v.drop >= 0) radius += .25 * (1 - v.drop / 12.0);
            double height=tier==3?3.55+2.6*third:tier==2?3.55:3.10;
            Vec3 eye=new Vec3(-p.f_82479_,-p.f_82480_,-p.f_82481_);
            flame(out,matrix,time,radius,height,strength,tier,eye);
            boolean detailed=p.m_82554_(Vec3.f_82478_)<32*32;
            if(tier==3 && detailed) flame(out,matrix,time*.87,radius*.58,height*.61,strength*.34f,tier,eye,.85f);
            if(tier==3 && v.packet.target()==3 && v.packet.chargeAge()+v.since<24) {
                double age=v.packet.chargeAge()+v.since+dt;
                ring(out,matrix,.6+age*.09,(float)Math.max(0,.22*(1-age/24)));
            }
            if (tier >= 2 && v.fade < 0 && strength > .1f) lightning(out, matrix, v, new Vec3(-p.f_82479_, -p.f_82480_, -p.f_82481_), strength);
            if (SaiyanConfig.HEAD.get() && !SaiyanWig.worn((net.minecraft.world.entity.LivingEntity)v.entity) && (v.packet.form() != 0 || v.charge() > .75f) && v.fade < 0) head(out, matrix, v.entity.m_20206_(), tier, time, strength);
            if (v.packet.target() != 0 && v.charge() > .62f) {
                double pulse = (time % 12) / 12;
                ring(out, matrix, .6 + pulse * (tier == 3 ? 5.5 : tier == 2 ? 2.8 : 1.6), (float)((1 - pulse) * .25 * v.charge()));
            }
            if(tier==3 && v.packet.target()==3 && v.charge()>.4f) {
                double beat=(time%20)/20;
                ring(out,matrix,.5+beat*5,(float)((1-beat)*.26*v.charge()));
                if(detailed && v.charge()>.6f) flame(out,matrix,time*1.4,radius*.72,6+3*v.charge(),strength*.48f,tier,eye,.35f);
                if(detailed) sphere(out,matrix,2+.15*Math.sin(time*.6),(v.charge()>.85f?.12f:.075f)*strength,time);
            }
            if (v.burst >= 0) {
                double age = v.burst + dt; float alpha = (float)Math.max(0, 1 - age / (tier == 3 ? 24 : tier == 2 ? 18 : 12));
                // Solas squares particle alpha. A .07 shell was effectively invisible.
                sphere(out, matrix, 1.25 + age * (tier == 3 ? .8 : tier == 2 ? .62 : .4), alpha * (tier==3?.46f:.40f), time);
                ring(out, matrix, .4 + age * (tier == 3 ? 1.4 : tier == 2 ? 1.15 : .7), alpha * .9f);
                if (tier == 2 && age > 3) ring(out, matrix, .4 + (age - 3) * .85, alpha * .55f);
            }
        }
        buffers.m_109912_(AURA);
    }
    private static void flame(VertexConsumer out, Matrix4f matrix, double time, double radius, double height, float strength, int tier, Vec3 eye) {
        flame(out,matrix,time,radius,height,strength,tier,eye,0);
    }
    private static void flame(VertexConsumer out,Matrix4f matrix,double time,double radius,double height,float strength,int tier,Vec3 eye,float pale) {
        double eyeRadius = Math.hypot(eye.f_82479_, eye.f_82481_);
        int count = tier == 3 ? (eyeRadius>32?6:12) : tier == 2 ? 10 : 8;
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double phase = time * (tier == 3 ? .24 : tier == 2 ? .18 : .13) + i * 2.17;
            double h = height * (.87 + .13 * Math.sin(phase * .6));
            double flare = 1 + .09 * Math.sin(phase) + .045 * Math.sin(phase * 1.7);
            double facing = eyeRadius < .001 ? 0 : (cos * eye.f_82479_ + sin * eye.f_82481_) / eyeRadius;
            float openness = (float)(1 - .92 * Math.max(0, facing));
            // Adjacent quads share rows. Evaluate the curve once per row and retain
            // the reviewed vertex order, color, transparency and texture coordinates.
            FlameRow lower = flameRow(radius * flare, h, phase, 0);
            for (int row = 0; row < 9; row++) {
                FlameRow upper = flameRow(radius * flare, h, phase, (row + 1) / 9.0);
                for (int column = 0; column < 4; column++) {
                    flameVertex(out, matrix, cos, sin, lower, column, strength * openness,pale);
                    flameVertex(out, matrix, cos, sin, lower, column + 1, strength * openness,pale);
                    flameVertex(out, matrix, cos, sin, upper, column + 1, strength * openness,pale);
                    flameVertex(out, matrix, cos, sin, upper, column, strength * openness,pale);
                }
                lower = upper;
            }
        }
    }
    private static FlameRow flameRow(double radius, double height, double phase, double t) {
        // Rounded shoulders and traveling curls keep the silhouette broad below the tips.
        double r = radius * (.75 + .25 * Math.sin(t * Math.PI) - .26 * t * t
                + .10 * Math.sin(t * 9 - phase * 1.25) * Math.sin(t * Math.PI));
        double curl = .29 * t * Math.sin(t * 8 - phase);
        double width = radius * (.52 * Math.pow(1 - t, .7) + .02);
        float ends = (float)(Math.min(1, t * 9) * Math.min(1, (1 - t) * 7));
        return new FlameRow(r, .035 + t * height, curl, width, ends,
                (float)(.66 + .16 * t), .035f + .07f * (float)t, (float)(1 - t));
    }
    private static void flameVertex(VertexConsumer out, Matrix4f matrix, double cos, double sin,
            FlameRow row, int column, float strength,float pale) {
        double side = -1 + column * .5;
        double offset = side * row.width() + row.curl();
        // Strong at the back and sides, nearly transparent directly in front of the body.
        // This retains a visible gold silhouette even with Solas' squared particle alpha.
        float alpha = Math.min(.98f,.70f * strength * FLAME_EDGES[column] * row.ends());
        vertex(out, matrix, (float)(cos * row.radius() - sin * offset), (float)row.y(),
                (float)(sin * row.radius() + cos * offset), column * .25f, row.v(), alpha, 1, row.warm()+(1-row.warm())*pale,row.blue()+(.68f-row.blue())*pale);
    }
    private static void lightning(VertexConsumer out, Matrix4f matrix, SaiyanClient.Visual v, Vec3 eye, float strength) {
        boolean third=v.tier()==3;
        int interval = third?(v.packet.target()!=0?3:5):v.packet.target() != 0 ? 5 : v.burst >= 0 ? 4 : 10;
        if (v.visualAge % interval >= 4) return;
        // A stable seed holds each flash for four ticks; every point is local to the player.
        Random random = new Random(v.packet.player().getLeastSignificantBits() ^ (v.visualAge / interval * 1009L));
        int count = third?(v.packet.target()!=0?6:4):v.packet.target() != 0 ? 4 : v.burst >= 0 ? 5 : 2;
        for (int arc = 0; arc < count; arc++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double startY = .15 + random.nextDouble() * (third?1.8:.85);
            double length = (third?1.0:.65) + random.nextDouble() * (third?1.0:.6);
            Vec3[] points = new Vec3[8];
            for (int node = 0; node < points.length; node++) {
                double t = node / 7.0;
                double a = angle + t * .6 + (random.nextDouble() - .5) * .25;
                double r = (third?1.6:1.0) + random.nextDouble() * .24;
                points[node] = polar(a, r, startY + t * length + (node == 0 || node == 7 ? 0 : (random.nextDouble() - .5) * .12));
            }
            for (int node = 0; node < 7; node++) boltSegment(out, matrix, points[node], points[node + 1], eye, strength);
            Vec3 branch = points[4].m_82520_((random.nextDouble() - .5) * .4, .22, (random.nextDouble() - .5) * .4);
            boltSegment(out, matrix, points[4], branch, eye, strength * .75f);
        }
    }
    private static void boltSegment(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 eye, float strength) {
        Vec3 line = b.m_82546_(a), view = eye.m_82546_(a.m_82549_(b).m_82490_(.5));
        Vec3 cross = new Vec3(line.f_82480_ * view.f_82481_ - line.f_82481_ * view.f_82480_,
                line.f_82481_ * view.f_82479_ - line.f_82479_ * view.f_82481_,
                line.f_82479_ * view.f_82480_ - line.f_82480_ * view.f_82479_);
        double length = Math.sqrt(cross.f_82479_ * cross.f_82479_ + cross.f_82480_ * cross.f_82480_ + cross.f_82481_ * cross.f_82481_);
        if (length < 1.0e-6) return;
        // Iron's ZapParticle uses a white core and blue outer passes. Keep that palette,
        // with much narrower widths and more bends for a close-range transformation.
        Vec3 glow = cross.m_82490_(.030 / length), core = cross.m_82490_(.008 / length);
        quad(out, matrix, a.m_82546_(glow), a.m_82549_(glow), b.m_82549_(glow), b.m_82546_(glow), .28f * strength, .35f, .80f, 1);
        quad(out, matrix, a.m_82546_(core), a.m_82549_(core), b.m_82549_(core), b.m_82546_(core), .88f * strength, .86f, .97f, 1);
    }
    private static void head(VertexConsumer out, Matrix4f matrix, float height, int tier, double time, float strength) {
        int count = tier == 2 ? 9 : 7;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count, spike = (tier == 2 ? .35 : .24) + .05 * Math.sin(i * 1.7);
            Vec3 left = polar(a - .23, .28, height + .01), right = polar(a + .23, .28, height + .01);
            quad(out, matrix, left, right, polar(a + .015, .29, height + spike), polar(a - .015, .29, height + spike), .22f * strength, 1, .86f, .12f);
        }
    }
    private static Vec3 polar(double angle, double radius, double y) { return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius); }
    private static void sphere(VertexConsumer out, Matrix4f matrix, double radius, float alpha, double time) {
        for (int lat = 0; lat < 8; lat++) for (int lon = 0; lon < 20; lon++) {
            double a = lon * Math.PI * 2 / 20 + time * .015, b = (lon + 1) * Math.PI * 2 / 20 + time * .015;
            double low = Math.max(-Math.PI / 2 + .002, -Math.PI / 2 + lat * Math.PI / 8);
            double high = Math.min(Math.PI / 2 - .002, -Math.PI / 2 + (lat + 1) * Math.PI / 8);
            quad(out, matrix, ball(a, low, radius), ball(b, low, radius), ball(b, high, radius), ball(a, high, radius), alpha, 1, .88f, .23f);
        }
    }
    private static Vec3 ball(double a, double b, double r) { return new Vec3(Math.cos(a) * Math.cos(b) * r, 1 + Math.sin(b) * r, Math.sin(a) * Math.cos(b) * r); }
    private static void ring(VertexConsumer out, Matrix4f matrix, double radius, float alpha) {
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48, b = (i + 1) * Math.PI * 2 / 48;
            quad(out, matrix, polar(a, radius, .08), polar(b, radius, .08), polar(b, radius + .25, .08), polar(a, radius + .25, .08), alpha, 1, .90f, .32f);
        }
    }
    private static void quad(VertexConsumer out, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float alpha, float red, float green, float blue) {
        vertex(out, m, a, 0, 1, alpha, red, green, blue); vertex(out, m, b, 1, 1, alpha, red, green, blue);
        vertex(out, m, c, 1, 0, alpha, red, green, blue); vertex(out, m, d, 0, 0, alpha, red, green, blue);
    }
    private static void vertex(VertexConsumer out, Matrix4f m, Vec3 p, float u, float v, float alpha, float red, float green, float blue) {
        vertex(out, m, (float)p.f_82479_, (float)p.f_82480_, (float)p.f_82481_, u, v, alpha, red, green, blue);
    }
    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float z,
            float u, float v, float alpha, float red, float green, float blue) {
        // PARTICLE uses Position, UV0, Color, UV2, unlike the entity format.
        out.m_252986_(m, x, y, z).m_7421_(u, v).m_85950_(red, green, blue, alpha)
                .m_85969_(15728880).m_5752_();
    }
    private SaiyanVisuals() {}
}

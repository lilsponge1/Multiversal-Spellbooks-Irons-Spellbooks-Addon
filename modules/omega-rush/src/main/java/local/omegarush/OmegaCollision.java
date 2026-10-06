package local.omegarush;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OmegaCollision {
    public record Hit(double fraction, Vec3 point, Entity target) {}
    /** Continuous slab sweep; boxes which are merely touching and separating are ignored. */
    public static double sweep(AABB body, Vec3 delta, AABB obstacle) {
        double enter = -Double.MAX_VALUE, exit = Double.MAX_VALUE;
        double[] lo = {body.f_82288_, body.f_82289_, body.f_82290_};
        double[] hi = {body.f_82291_, body.f_82292_, body.f_82293_};
        double[] b0 = {obstacle.f_82288_, obstacle.f_82289_, obstacle.f_82290_};
        double[] b1 = {obstacle.f_82291_, obstacle.f_82292_, obstacle.f_82293_};
        double[] d = {delta.f_82479_, delta.f_82480_, delta.f_82481_};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-10) {
                if (hi[i] <= b0[i] + 1e-7 || lo[i] >= b1[i] - 1e-7) return Double.POSITIVE_INFINITY;
            } else {
                double a = (b0[i] - hi[i]) / d[i], b = (b1[i] - lo[i]) / d[i];
                enter = Math.max(enter, Math.min(a, b)); exit = Math.min(exit, Math.max(a, b));
            }
        }
        if (exit <= 1e-7 || enter > exit || enter > 1) return Double.POSITIVE_INFINITY;
        return Math.max(0, enter);
    }
    public static boolean safe(Level level, AABB bounds) {
        if (!Double.isFinite(bounds.f_82288_) || !Double.isFinite(bounds.f_82291_) ||
                !Double.isFinite(bounds.f_82289_) || !Double.isFinite(bounds.f_82292_) ||
                !Double.isFinite(bounds.f_82290_) || !Double.isFinite(bounds.f_82293_) ||
                bounds.f_82289_ < level.m_141937_() || bounds.f_82292_ >= level.m_151558_() ||
                !level.m_6857_().m_61935_(bounds)) return false;
        int minX = ((int)Math.floor(bounds.f_82288_)) >> 4, maxX = ((int)Math.floor(bounds.f_82291_)) >> 4;
        int minZ = ((int)Math.floor(bounds.f_82290_)) >> 4, maxZ = ((int)Math.floor(bounds.f_82293_)) >> 4;
        if (maxX - minX > 2 || maxZ - minZ > 2) return false;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (!level.m_7232_(x, z)) return false;
        return true;
    }
    public static Hit find(Level level, LivingEntity caster, Vec3 delta, boolean entities) {
        AABB body = caster.m_20191_();
        AABB area = body.m_82369_(delta).m_82400_(1e-5);
        double first = Double.POSITIVE_INFINITY;
        Entity target = null;
        AABB contactedBlock = null;
        for (VoxelShape shape : level.m_186434_(caster, area)) for (AABB box : shape.m_83299_()) {
            double at = sweep(body, delta, box);
            if (at < first) { first = at; contactedBlock = box; }
        }
        if (entities) for (Entity entity : level.m_6249_(caster, area,
                e -> e instanceof LivingEntity && OmegaManager.canHit(caster, e))) {
            double at = sweep(body, delta, entity.m_20191_());
            if (at < first - 1e-7) { first = at; target = entity; }
        }
        if (!Double.isFinite(first)) return null;
        // Feet plus half-height gives a body-centered contact; clamp to the contacted box below.
        Vec3 center = body.m_82399_().m_82549_(delta.m_82490_(first));
        if (target != null) center = closest(target.m_20191_(), center);
        else if (contactedBlock != null) center = closest(contactedBlock, center);
        return new Hit(first, center, target);
    }
    public static Vec3 closest(AABB b, Vec3 p) {
        return new Vec3(Math.max(b.f_82288_, Math.min(b.f_82291_, p.f_82479_)),
                Math.max(b.f_82289_, Math.min(b.f_82292_, p.f_82480_)),
                Math.max(b.f_82290_, Math.min(b.f_82293_, p.f_82481_)));
    }
    private OmegaCollision() {}
}

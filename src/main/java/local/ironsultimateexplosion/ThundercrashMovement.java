package local.ironsultimateexplosion;

import net.minecraft.world.phys.Vec3;

/** No world or config access: identical arithmetic on the predicting client and server. */
public final class ThundercrashMovement {
    public static Vec3 look(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }
    public static boolean finite(Vec3 v) {
        return Double.isFinite(v.f_82479_) && Double.isFinite(v.f_82480_) && Double.isFinite(v.f_82481_);
    }
    public static Vec3 step(Vec3 velocity, Vec3 look, double speed, double steering, boolean launch, double lift) {
        if (!finite(velocity) || !finite(look) || !Double.isFinite(speed) || speed <= 0
                || !Double.isFinite(steering) || steering < 0 || steering > 1
                || launch && !Double.isFinite(lift)) return Vec3.f_82478_;
        Vec3 wanted = launch ? new Vec3(look.f_82479_ * 0.125, lift, look.f_82481_ * 0.125)
                : look.m_82541_().m_82490_(speed);
        Vec3 next = launch ? wanted : velocity.m_82490_(1 - steering).m_82549_(wanted.m_82490_(steering));
        double length = next.m_82553_();
        return length > speed ? next.m_82490_(speed / length) : next;
    }
    private ThundercrashMovement() {}
}

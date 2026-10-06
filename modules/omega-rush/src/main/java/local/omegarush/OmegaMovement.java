package local.omegarush;
import net.minecraft.world.phys.Vec3;
/** Shared server/prediction arithmetic, matching Thundercrash's flight response. */
public final class OmegaMovement {
    public static Vec3 look(float yaw,float pitch) {
        double y=Math.toRadians(yaw), p=Math.toRadians(pitch);
        return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
    }
    public static boolean finite(Vec3 v) { return Double.isFinite(v.f_82479_) && Double.isFinite(v.f_82480_) && Double.isFinite(v.f_82481_); }
    public static Vec3 step(Vec3 velocity,Vec3 look,double speed,double steering) {
        if(!finite(velocity)||!finite(look)||!Double.isFinite(speed)||speed<=0||!Double.isFinite(steering)||steering<0||steering>1) return Vec3.f_82478_;
        Vec3 wanted=look.m_82541_().m_82490_(speed);
        Vec3 next=velocity.m_82490_(1-steering).m_82549_(wanted.m_82490_(steering));
        double length=next.m_82553_();
        return length>speed?next.m_82490_(speed/length):next;
    }
    private OmegaMovement() {}
}

package local.omegarush;
import net.minecraft.world.phys.Vec3;
public final class OmegaHover {
    public static Vec3 step(Vec3 old,float yaw,float forward,float strafe,float vertical,double speed,double rise) {
        Vec3 desired=OmegaMovement.look(yaw,0).m_82490_(forward).m_82549_(OmegaMovement.look(yaw-90,0).m_82490_(strafe));
        if(desired.m_82556_()>1)desired=desired.m_82541_();desired=desired.m_82490_(speed);
        return new Vec3(old.f_82479_*0.75+desired.f_82479_*0.25,vertical*rise,old.f_82481_*0.75+desired.f_82481_*0.25);
    }
    private OmegaHover(){}
}

package local.omegarush;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Cosmetic explosion fan behind a path node, oriented to that segment's movement. */
public final class OmegaCone {
    public static List<Vec3> points(Vec3 center,Vec3 direction,int index,int count) {
        if(!OmegaMovement.finite(center)||!OmegaMovement.finite(direction)||count<=0) return List.of();
        Vec3 forward=direction.m_82556_()<1e-8?new Vec3(0,0,1):direction.m_82541_();
        Vec3 lateral=forward.m_82537_(new Vec3(0,1,0));
        Vec3 right=lateral.m_82556_()<1e-8?new Vec3(1,0,0):lateral.m_82541_();
        Vec3 up=right.m_82537_(forward).m_82541_();
        List<Vec3> points=new ArrayList<>();
        for(int i=0;i<Math.min(count,8);i++) {
            double depth=1+(i+0.5)*2/count,spread=(0.5+depth*0.8)*1.35;
            double angle=index*2.399963229728653+i*Math.PI*2/count;
            points.add(center.m_82549_(forward.m_82490_(-depth))
                .m_82549_(right.m_82490_(Math.cos(angle)*(spread+3.0)))
                .m_82549_(up.m_82490_(0.8+Math.sin(angle)*spread)));
        }
        return points;
    }
    private OmegaCone() {}
}

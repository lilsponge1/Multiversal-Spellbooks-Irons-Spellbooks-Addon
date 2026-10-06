package local.omegarush;
import java.util.*;
import net.minecraft.world.phys.Vec3;
/** Arc-length sampling; remainder survives curves and short tick segments. */
public final class OmegaPath {
    private double remainder;
    public List<Vec3> sample(Vec3 from,Vec3 to,double spacing) {
        var points=new ArrayList<Vec3>();
        if(!OmegaMovement.finite(from)||!OmegaMovement.finite(to)||!Double.isFinite(spacing)||spacing<=0) return points;
        Vec3 delta=to.m_82546_(from); double length=delta.m_82553_();
        if(length<1e-9) return points;
        double offset=spacing-remainder;
        while(offset<=length+1e-9 && points.size()<16) {
            points.add(from.m_82549_(delta.m_82490_(Math.min(1,offset/length)))); offset+=spacing;
        }
        remainder=(remainder+length)%spacing;
        if(remainder<1e-9||spacing-remainder<1e-9) remainder=0;
        return points;
    }
    public double remainder() { return remainder; }
}

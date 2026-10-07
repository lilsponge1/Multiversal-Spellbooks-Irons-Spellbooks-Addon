package local.omegarush;
import net.minecraft.world.phys.Vec3;

/** Shared clock for the physical rise, six arrivals, orbit and final absorption. */
public final class OmegaFormRitual {
    public static final int FLOWERS=6,MIN_TICKS=96;
    public static double progress(double age,int ticks){return clamp(age/Math.max(1,ticks));}
    private static double clamp(double n){return Math.max(0,Math.min(1,n));}
    private static double smooth(double n){n=clamp(n);return n*n*(3-2*n);}
    public static double height(double age,int ticks,double lift){return lift*smooth(progress(age,ticks)/0.22);}
    public static int arrival(int flower,int ticks){return (int)Math.ceil(Math.max(1,ticks)*(0.25+0.05*flower));}
    public static int count(double age,int ticks){int n=0;while(n<FLOWERS&&age>=arrival(n,ticks))n++;return n;}
    public static float pitch(int flower){return 1.0f+Math.max(0,Math.min(5,flower))*0.18f;}
    public static double absorption(double age,int ticks){return smooth((progress(age,ticks)-0.78)/0.195);}
    public static double scale(int flower,double age,int ticks){return Math.min(1,Math.max(0,(age-arrival(flower,ticks))/5.0))*(1-absorption(age,ticks));}
    public static Vec3 offset(int flower,double age,int ticks,float yaw){
        double a=age*0.075+flower*Math.PI*2/FLOWERS;
        double remaining=1-absorption(age,ticks),radius=2.1*remaining;
        double side=Math.cos(a)*radius,depth=Math.sin(a)*0.45*remaining,y=Math.sin(a)*1.65*remaining,r=Math.toRadians(yaw);
        return new Vec3(side*Math.cos(r)-depth*Math.sin(r),y,side*Math.sin(r)+depth*Math.cos(r));
    }
    private OmegaFormRitual(){}
}

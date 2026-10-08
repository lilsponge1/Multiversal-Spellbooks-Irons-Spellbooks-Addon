package local.ironsultimateexplosion;

import java.util.ArrayList;
import java.util.List;

/** Immutable mesh built once per form. Coordinates are head-local model pixels. */
final class SaiyanHairMesh {
    private static final int TOP=0, LEFT=1, RIGHT=2, FRONT=3, BACK=4;
    private static final int STEPS=12, SIDES=16;
    private static final float[][][] FORMS = {build(0),build(1),build(2)};
    private static final float[][] THIRD_HEAD=buildThirdHead();
    private static final float[][][] THIRD_TAILS=buildThirdTails();
    static float[][] faces(int tier) { return FORMS[Math.max(0, Math.min(2,tier))]; }
    static float[][] thirdHead() { return THIRD_HEAD; }
    static float[][][] thirdTails() { return THIRD_TAILS; }

    private record P(double x,double y,double z) {
        P add(P p) { return new P(x+p.x,y+p.y,z+p.z); }
        P mul(double n) { return new P(x*n,y*n,z*n); }
        double dot(P p) { return x*p.x+y*p.y+z*p.z; }
        P cross(P p) { return new P(y*p.z-z*p.y,z*p.x-x*p.z,x*p.y-y*p.x); }
        P unit() { return mul(1/Math.sqrt(dot(this))); }
    }
    private static P p(double x,double y,double z) { return new P(x,y,z); }
    private static float[][] buildThirdHead() {
        List<float[]> f=new ArrayList<>(); scalp(f,true);
        // Broad locks travel from the forehead over the crown into the same swept-back
        // silhouette as the mane. There is no separate ring of sideways cap spikes.
        thirdLock(f,TOP,3.0,1.8,30,p(0,-10.3,-1.5),p(-.7,-16.5,-1),p(-1,-20,2),p(-.5,-18.5,6));
        thirdLock(f,TOP,3.0,1.8,31,p(-2.4,-10.2,-.4),p(-4,-16,2),p(-6.5,-18,5),p(-8,-15.5,9));
        thirdLock(f,TOP,3.0,1.8,32,p(2.4,-10.2,-.4),p(4,-16,2),p(6.5,-18,5),p(8,-15.5,9));
        thirdLock(f,TOP,3.2,2.0,33,p(-1,-10.5,3.4),p(-1.8,-14,6),p(-2.2,-12,9),p(-3,-8.7,11.5));
        thirdLock(f,TOP,3.2,2.0,34,p(2,-10.5,3.4),p(3.5,-14,6),p(5,-11.5,9),p(7,-8.7,11));
        thirdLock(f,LEFT,2.9,1.8,35,p(-5.1,-8.4,.6),p(-7,-12,3),p(-10,-11,6),p(-12,-7.5,9));
        thirdLock(f,RIGHT,2.9,1.8,36,p(5.1,-8.4,.6),p(7,-12,3),p(10,-11,6),p(12,-7.5,9));
        thirdLock(f,FRONT,1.25,.65,37,p(2,-10.2,-5.1),p(3.4,-9.2,-5.8),p(4.4,-6.5,-6),p(3.7,-5.6,-5.3));
        thirdLock(f,FRONT,.95,.6,38,p(-1.8,-10.2,-5.1),p(-2.8,-9.2,-5.8),p(-3.8,-7.5,-6),p(-3,-6.8,-5.3));
        return f.toArray(float[][]::new);
    }
    private static float[][][] buildThirdTails() {
        List<float[][]> tails=new ArrayList<>();
        // Overlapping tiers descend from the nape along the spine. The mane has
        // its own shallow depth; the head's large collision box does not offset it.
        for(int layer=0;layer<4;layer++) for(int side=0;side<4;side++) {
            double x=(side-1.5)*2.85, direction=(side-1.5)/1.5;
            double rootY=-10.6+layer*.25, rootZ=5.5+layer*.25;
            double endY=3.8+layer*3.3+(1-Math.abs(direction));
            double spread=direction*(12.5-layer*1.05);
            double endZ=6.6+layer*.4;
            List<float[]> f=new ArrayList<>();
            thirdLock(f,BACK,3.3-layer*.12,2.0,40+layer*4+side,
                p(x,rootY,rootZ),
                p(x+direction*2.2,-.7+layer*1.1,6.8+layer*.15),
                p(spread*.86,endY*.7,7.2+layer*.15),
                p(spread,endY,endZ));
            tails.add(f.toArray(float[][]::new));
        }
        return tails.toArray(float[][][]::new);
    }
    private static void thirdLock(List<float[]> faces,int region,double width,double depth,int seed,P a,P b,P c,P d) {
        // Fewer, broader ridges produce pointed anime locks rather than round ropes.
        // The older forms keep their approved cross sections and shading unchanged.
        strand(faces,0,region,width,depth,seed,a,b,c,d,true);
    }
    private static float[][] build(int tier) {
        List<float[]> faces = new ArrayList<>();
        scalp(faces);
        // Reference silhouette: tall asymmetric peak and broad, swept lateral locks.
        lock(faces,tier,TOP,2.65,1.65,0, p(0,-10.0,.8),p(.5,-13.4,1.0),p(-.3,-17.5,0),p(-3.4,-20.8,-.8));
        lock(faces,tier,TOP,2.45,1.55,1, p(-2.3,-9.8,.6),p(-4.4,-12.4,.5),p(-7.3,-14.5,.2),p(-10.4,-16.4,-.3));
        lock(faces,tier,TOP,2.2,1.45,2, p(-3.7,-9.1,.9),p(-6.4,-10.4,.9),p(-9.1,-11.8,.6),p(-12.0,-12.7,.2));
        lock(faces,tier,TOP,2.35,1.5,3, p(2.5,-9.7,1.2),p(4.2,-11.8,1.4),p(6.4,-13.8,1.3),p(8.3,-15.3,.8));
        lock(faces,tier,TOP,2.0,1.4,4, p(3.6,-9.0,1.2),p(6.0,-10.3,1.1),p(8.7,-11.4,.7),p(11.0,-12.4,.2));
        // Side and rear layers wrap the skull instead of sitting on a flat cap.
        lock(faces,tier,LEFT,1.75,1.25,5, p(-5.0,-7.5,1.2),p(-6.9,-8.2,1.4),p(-9.2,-8.3,1.4),p(-11.6,-8.6,.6));
        lock(faces,tier,RIGHT,1.75,1.25,6, p(5.0,-7.4,1.3),p(6.7,-7.8,1.4),p(8.8,-8.3,1.1),p(11.0,-8.4,.4));
        lock(faces,tier,BACK,2.3,1.35,7, p(.2,-8.6,5.3),p(.8,-10.1,6.2),p(2.4,-11.6,8.0),p(4.0,-12.4,9.7));
        lock(faces,tier,BACK,1.8,1.15,8, p(-2.7,-6.6,5.3),p(-4.6,-7.0,6.2),p(-6.3,-7.5,7.4),p(-8.0,-8.0,8.8));
        lock(faces,tier,BACK,1.8,1.15,9, p(2.8,-6.5,5.3),p(4.6,-6.9,6.2),p(6.4,-7.1,7.4),p(8.3,-7.5,8.7));
        lock(faces,tier,BACK,1.65,1.1,10, p(.1,-5.6,5.4),p(.3,-4.4,6.0),p(.6,-3.2,7.4),p(1.4,-2.8,8.5));
        // Curved bangs stay entirely in front of the outer skin, above the eyes.
        lock(faces,tier,FRONT,1.6,.67,11, p(-1.8,-10.0,-5.1),p(-3.0,-8.8,-5.7),p(-2.8,-6.2,-5.8),p(-1.25,-5.3,-5.4));
        lock(faces,tier,FRONT,1.45,.62,12, p(1.4,-10.0,-5.1),p(2.8,-8.8,-5.7),p(3.8,-7.0,-5.6),p(3.4,-5.9,-5.1));
        lock(faces,tier,FRONT,.95,.52,13, p(.1,-9.7,-5.25),p(.5,-8.7,-5.9),p(-.5,-7.3,-5.8),p(-.65,-6.5,-5.35));
        lock(faces,tier,LEFT,1.1,.65,14, p(-5.1,-8.1,-2.5),p(-5.5,-6.8,-2.6),p(-5.4,-4.9,-2.5),p(-4.8,-3.7,-2.2));
        lock(faces,tier,RIGHT,1.1,.65,15, p(5.1,-8.1,-2.5),p(5.5,-6.8,-2.6),p(5.4,-4.9,-2.5),p(4.8,-3.7,-2.2));
        return faces.toArray(float[][]::new);
    }
    private static P shape(P p,int tier,int region) {
        if (tier == 0) return p;
        if (region == TOP) return p(p.x*.76,-8.6+(p.y+8.6)*(tier==2?1.15:1.04),p.z*.92);
        if (region == FRONT) return p(p.x,-8.7+(p.y+8.7)*.8,p.z);
        return p;
    }
    private static P clear(P p,int region) {
        return switch(region) {
            case LEFT -> p(Math.min(-4.7,p.x),p.y,p.z);
            case RIGHT -> p(Math.max(4.7,p.x),p.y,p.z);
            case FRONT -> p(p.x,p.y,Math.min(-4.7,p.z));
            case BACK -> p(p.x,p.y,Math.max(4.7,p.z));
            default -> p(p.x,Math.min(-8.6,p.y),p.z);
        };
    }
    private static P curve(P a,P b,P c,P d,double t) {
        double s=1-t;
        return a.mul(s*s*s).add(b.mul(3*s*s*t)).add(c.mul(3*s*t*t)).add(d.mul(t*t*t));
    }
    private static P tangent(P a,P b,P c,P d,double t) {
        double s=1-t;
        return b.add(a.mul(-1)).mul(3*s*s).add(c.add(b.mul(-1)).mul(6*s*t))
            .add(d.add(c.mul(-1)).mul(3*t*t)).unit();
    }
    private static void lock(List<float[]> faces,int tier,int region,double width,double depth,int seed,P a,P b,P c,P d) {
        strand(faces,tier,region,width,depth,seed,a,b,c,d,false);
    }
    private static void strand(List<float[]> faces,int tier,int region,double width,double depth,int seed,P a,P b,P c,P d,boolean third) {
        a=shape(a,tier,region); b=shape(b,tier,region); c=shape(c,tier,region); d=shape(d,tier,region);
        float[][][] rings = new float[STEPS+1][SIDES][];
        for (int step=0; step<=STEPS; step++) {
            double t=(double)step/STEPS;
            P center=curve(a,b,c,d,t), direction=tangent(a,b,c,d,t);
            P axis=Math.abs(direction.x)<.9?p(1,0,0):p(0,0,1);
            P u=axis.add(direction.mul(-axis.dot(direction))).unit(),v=direction.cross(u).unit();
            double taper=Math.max(.004,Math.pow(1-t,.7)*(1+.35*Math.sin(Math.PI*t)));
            for (int side=0; side<SIDES; side++) {
                double angle=2*Math.PI*side/SIDES,co=Math.cos(angle),si=Math.sin(angle);
                double groove=third?.96+.04*Math.cos(3*angle+seed*.2):.975+.025*Math.cos(6*angle+seed*.45);
                P raw=center.add(u.mul(co*width*taper*groove)).add(v.mul(si*depth*taper*groove));
                P vertex=third && region==BACK ? p(raw.x,raw.y,Math.max(2.55+2.15*Math.max(0,Math.min(1,(3-raw.y)/3)),raw.z)) : clear(raw,region);
                P n=u.mul(co/width).add(v.mul(si/depth)).unit();
                double shade=third?(.9+.1*Math.cos(3*angle+seed*.2))*(.97+.03*Math.sin(Math.PI*t)):(.79+.21*Math.cos(6*angle+seed*.45))*(.94+.06*Math.sin(Math.PI*t));
                rings[step][side]=vertex(vertex,n,(double)side/SIDES,t,shade);
            }
        }
        for(int step=0;step<STEPS;step++) for(int side=0;side<SIDES;side++) {
            int next=(side+1)%SIDES;
            face(faces,rings[step][side],rings[step][next],rings[step+1][next],rings[step+1][side]);
        }
        // Tiny end caps keep the tips closed when viewed from above or behind.
        for(int side=0;side<SIDES;side++) {
            int next=(side+1)%SIDES;
            P tip=third && region==BACK ? p(d.x,d.y,Math.max(2.55+2.15*Math.max(0,Math.min(1,(3-d.y)/3)),d.z)) : clear(d,region),n=tangent(a,b,c,d,1);
            float[] center=vertex(tip,n,.5,1,.8);
            face(faces,rings[STEPS][side],rings[STEPS][next],center,center);
        }
    }
    private static void scalp(List<float[]> faces) {
        scalp(faces,false);
    }
    private static void scalp(List<float[]> faces,boolean third) {
        int sides=32;
        float[][][] rings = new float[6][sides][];
        double[] radii={1,1,.94,.73,.38,.002};
        double[] heights={0,-8.6,-9.7,-10.6,-11.1,-11.35};
        for(int ring=0;ring<rings.length;ring++) for(int side=0;side<sides;side++) {
            double angle=2*Math.PI*side/sides,co=Math.cos(angle),si=Math.sin(angle);
            double perimeter=4.7/Math.max(Math.abs(co),Math.abs(si));
            double y=ring==0?-4.8-2.4*Math.max(0,-si)+1.2*Math.max(0,si):heights[ring];
            double rear=third?1.5*Math.max(0,si)*Math.sqrt(radii[ring]):0;
            P pos=p(co*perimeter*radii[ring],y,si*perimeter*radii[ring]+rear);
            P n=p(co,-ring*.36,si).unit();
            rings[ring][side]=vertex(pos,n,(double)side/sides,(double)ring/(rings.length-1),third?.9+.1*Math.cos(angle*6):.83+.17*Math.cos(angle*12));
        }
        for(int ring=0;ring<rings.length-1;ring++) for(int side=0;side<sides;side++) {
            int next=(side+1)%sides;
            face(faces,rings[ring][side],rings[ring][next],rings[ring+1][next],rings[ring+1][side]);
        }
    }
    private static float[] vertex(P p,P n,double u,double v,double shade) {
        return new float[]{(float)p.x,(float)p.y,(float)p.z,(float)n.x,(float)n.y,(float)n.z,(float)u,(float)v,(float)shade};
    }
    private static void face(List<float[]> faces,float[] a,float[] b,float[] c,float[] d) {
        P ab=p(b[0]-a[0],b[1]-a[1],b[2]-a[2]),ac=p(c[0]-a[0],c[1]-a[1],c[2]-a[2]);
        P average=p(a[3]+b[3]+c[3]+d[3],a[4]+b[4]+c[4]+d[4],a[5]+b[5]+c[5]+d[5]);
        float[][] order=ab.cross(ac).dot(average)<0?new float[][]{a,d,c,b}:new float[][]{a,b,c,d};
        float[] face=new float[36];
        for(int i=0;i<4;i++) System.arraycopy(order[i],0,face,i*9,9);
        faces.add(face);
    }
}

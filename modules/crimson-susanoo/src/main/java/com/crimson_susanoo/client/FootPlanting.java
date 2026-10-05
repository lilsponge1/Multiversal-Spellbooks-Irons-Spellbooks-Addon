package com.crimson_susanoo.client;

import java.util.HashMap;
import java.util.Map;

/** Client-only foot contacts in world space. Does not move the entity or retime damage. */
final class FootPlanting {
    @FunctionalInterface interface Ground { double height(double x, double z); }
    private static final String[] SIDES = {"left", "right"};
    private static final double[] HIP_X = {-8, 8}; // Gecko's converted, anatomically correct rig
    private static final double CONTACT_HEIGHT = .20 / 16;
    private final Foot[] feet = {new Foot(), new Foot()};
    private double lastFrame = Double.NEGATIVE_INFINITY;
    private double lastX, lastY, lastZ, lastYaw;
    private double bodyDrop;
    private int lastAction, nextFoot;
    private Map<String, float[]> cached = Map.of();

    private static final class Foot {
        Point point, from, to, releaseOffset = new Point(0, 0, 0);
        double yaw, fromYaw, toYaw, started, duration;
        boolean swinging, contact, finishingWalk;
    }
    private record Point(double x, double y, double z) {
        Point add(Point p) { return new Point(x+p.x, y+p.y, z+p.z); }
        Point subtract(Point p) { return new Point(x-p.x, y-p.y, z-p.z); }
        Point scale(double s) { return new Point(x*s, y*s, z*s); }
        double horizontal() { return Math.hypot(x, z); }
    }

    void apply(double frame, double x, double y, double z, double yaw, int action,
               boolean eligible, Map<String, float[]> pose, Ground ground) {
        // Active clips have no right-arm translation. Ground support owns this
        // offset, including clearing history before a new solve or an ending pose.
        float[] weaponArm = pose.get("right_arm");
        if (weaponArm != null) weaponArm[3] = weaponArm[4] = weaponArm[5] = 0;
        if (!eligible) {
            lastFrame = Double.NEGATIVE_INFINITY;
            bodyDrop = 0;
            cached = Map.of();
            return;
        }
        if (frame == lastFrame) {
            cached.forEach((name, value) -> pose.put(name, value.clone()));
            return;
        }
        double dt = frame-lastFrame;
        boolean reset = dt <= 0 || dt > 2 || Math.hypot(x-lastX, z-lastZ) > 2.5
                || Math.abs(y-lastY) > 1.25;
        Point origin = new Point(x, y, z);
        double alpha = Math.toRadians(180-yaw);
        Point[] authored = new Point[2];
        double[] legReach = new double[2];
        for (int i=0; i<2; i++) {
            // Planting owns the sole position, but attack anticipation still owns
            // knee loading. Solving every contact at full extension erased that bend.
            double knee = action == 0 ? 0 : pose.get(SIDES[i]+"_shin")[0];
            legReach[i] = Math.sqrt(20*20+16*16+2*20*16*Math.cos(knee));
            Point p = toWorld(sole(pose, i), origin, alpha);
            // Authored lift is measured above the entity's reference floor, not
            // above a lower terrain sample. Preserve that lift on either surface.
            double lift = Math.max(0,p.y-y);
            authored[i] = onGround(p,ground).add(new Point(0,lift,0));
        }
        // Active clips do not animate pelvis translation. This layer owns its
        // support offset; attack-entry history was already read into authored feet.
        float[] pelvis = pose.get("pelvis");
        if (pelvis != null) pelvis[4] = 0;
        if (reset) {
            for (int i=0; i<2; i++) {
                Foot f = feet[i];
                f.point = action == 0 ? home(i, origin, alpha, ground) : authored[i];
                f.yaw = yaw;
                f.swinging = false;
                f.finishingWalk = false;
                f.contact = action == 0 || f.point.y-ground.height(f.point.x, f.point.z) <= CONTACT_HEIGHT;
                if (f.contact) f.point = onGround(f.point, ground);
                f.releaseOffset = new Point(0,0,0);
            }
            nextFoot = 0;
            bodyDrop = 0;
            dt = 0;
        }
        if (action == 0) {
            // Finish a lifted step after stopping; never advance planted feet on a timer.
            if (!reset && lastAction != 0) {
                for (int i=0; i<2; i++) {
                    Foot f=feet[i];
                    f.finishingWalk=false;
                    f.swinging=false;
                    if (f.point.y-ground.height(f.point.x, f.point.z) > CONTACT_HEIGHT)
                        start(f, home(i, origin, alpha, ground), yaw, frame, 4);
                }
            }
            Point velocity = dt>0 ? new Point((x-lastX)/dt,0,(z-lastZ)/dt) : new Point(0,0,0);
            double turnRate = dt>0 ? wrap(yaw-lastYaw)/dt : 0;
            double travel = velocity.horizontal()+Math.toRadians(Math.abs(turnRate))*.75;
            // A large armored summon needs one readable stride, not several
            // tiny catch-up steps. Cadence follows actual travel, never an idle timer.
            double duration = clamp(1.05 / Math.max(.08,travel), 3, 10);
            double landingYaw = yaw+clamp(turnRate*duration*.55,-45,45);
            Point lead = velocity.scale(duration*.95);
            if (lead.horizontal() > .95) lead = lead.scale(.95/lead.horizontal());
            Point[] landings = new Point[2];
            for (int i=0; i<2; i++) {
                landings[i]=onGround(home(i,origin,Math.toRadians(180-landingYaw),ground).add(lead),ground);
                Foot f=feet[i];
                // Commit the landing at lift-off. Retargeting it every frame
                // until65% created a velocity kink when the target froze, which
                // looked like a second step. The next foot handles a changed heading.
                if (f.swinging && duration < f.duration) {
                    // Acceleration may shorten a slow first stride. Preserve its
                    // current phase and ease the pace instead of jumping forward.
                    double phase=clamp((lastFrame-f.started)/f.duration,0,1);
                    f.duration+=(duration-f.duration)*(1-Math.exp(-dt/2));
                    f.started=lastFrame-phase*f.duration;
                }
                advance(f,frame);
            }
            if (!feet[0].swinging && !feet[1].swinging) {
                Point[] homes = {home(0,origin,alpha,ground), home(1,origin,alpha,ground)};
                double a = homes[0].subtract(feet[0].point).horizontal();
                double b = homes[1].subtract(feet[1].point).horizontal();
                // Keep a genuine left/right rhythm through ordinary turns;
                // picking whichever foot lagged farther could repeat one foot.
                int i = nextFoot;
                double error = i==0 ? a : b;
                double turn = Math.abs(wrap(yaw-feet[i].yaw));
                if (error > .28 || turn > 35) {
                    start(feet[i], landings[i], landingYaw, frame, duration);
                    nextFoot = 1-i;
                }
            }
        } else {
            for (int i=0; i<2; i++) {
                Foot f=feet[i];
                if (lastAction == 0) f.finishingWalk=f.swinging;
                if (f.finishingWalk) {
                    // Finish the airborne approach step before locking combat
                    // contacts. Otherwise a long trailing stride freezes far
                    // behind the forward attack lunge and stretches the hip.
                    advance(f,frame);
                    if (!f.swinging) f.finishingWalk=false;
                    continue;
                }
                f.swinging=false;
                Point target=authored[i];
                double floor=ground.height(target.x,target.z);
                double lift=Math.max(0,target.y-floor);
                if (lift <= CONTACT_HEIGHT) {
                    if (!f.contact) {
                        f.point=onGround(target,ground);
                        f.yaw=yaw;
                    }
                    f.contact=true; // Hold position AND heading until the authored lift-off.
                } else {
                    if (f.contact) {
                        Point offset=f.point.subtract(target);
                        // Retain horizontal continuity. Reapplying a negative lift-off
                        // height during descent would pull the sole beneath the floor.
                        f.releaseOffset=new Point(offset.x,0,offset.z);
                    }
                    f.contact=false;
                    double release=1-ease(clamp(lift/(2.0/16),0,1));
                    f.point=target.add(f.releaseOffset.scale(release));
                    f.yaw += wrap(yaw-f.yaw)*Math.min(1,dt/2);
                }
            }
        }
        Point[] local = {toLocal(feet[0].point,origin,alpha),toLocal(feet[1].point,origin,alpha)};
        if (pelvis != null) {
            float[] root = pose.get("root");
            double rootY = root == null ? 0 : root[4], needed = 0, terrainDepth = 0;
            for (int i=0; i<2; i++) {
                double horizontal = Math.hypot(local[i].x-HIP_X[i],local[i].z);
                double ankleY = local[i].y+8-rootY;
                double down = Math.min(44-ankleY,Math.sqrt(Math.max(1,legReach[i]*legReach[i]-horizontal*horizontal)));
                needed = Math.max(needed,44-ankleY-down);
                terrainDepth = Math.max(terrainDepth,(y-ground.height(feet[i].point.x,feet[i].point.z))*16);
            }
            // Settle the body between uneven supports rather than pulling a hip
            // out of its armor. Two pixels of headroom allow a soft return to level.
            // Flat and half-block support retain their existing envelope. A full
            // stair drop needs additional settling to keep the hips inside armor.
            double maximumDrop = 8+clamp(terrainDepth-8,0,8);
            double desired = clamp(needed-8,0,maximumDrop);
            bodyDrop += (desired-bodyDrop)*(reset ? 1 : 1-Math.exp(-dt/2));
            bodyDrop = Math.max(bodyDrop,clamp(needed-10,0,maximumDrop));
            pelvis[4] = (float)-bodyDrop;
            supportWeapon(pose);
        }
        for (int i=0; i<2; i++) solve(pose, i, local[i], Math.toRadians(wrap(yaw-feet[i].yaw)),legReach[i]);
        cached=new HashMap<>();
        for (String side:SIDES) for (String part:new String[]{"_leg","_shin","_foot"}) {
            String name=side+part;
            cached.put(name,pose.get(name).clone());
        }
        if (pelvis != null) cached.put("pelvis",pelvis.clone());
        if (weaponArm != null) cached.put("right_arm",weaponArm.clone());
        lastFrame=frame; lastX=x; lastY=y; lastZ=z; lastYaw=yaw; lastAction=action;
    }

    /** Reapply after upper-body counterbalance, using the final parent rotations. */
    static void supportWeapon(Map<String,float[]> pose) {
        float[] arm=pose.get("right_arm"), pelvis=pose.get("pelvis");
        if (arm == null) return;
        arm[3]=arm[4]=arm[5]=0;
        double lift=pelvis == null ? 0 : Math.max(0,-pelvis[4]-8);
        if (lift == 0) return;
        // Invert the final parent rotation to lift in the pelvis parent's vertical
        // direction. Translating the full arm keeps its hand and katana joined.
        double[] parent={1,0,0,0,1,0,0,0,1};
        for (String name : new String[]{"pelvis","waist","ribcage","right_shoulder"}) {
            float[] ancestor=pose.get(name);
            if (ancestor != null) parent=mul(parent,rotation(ancestor));
        }
        arm[3]=(float)(parent[3]*lift);
        arm[4]=(float)(parent[4]*lift);
        arm[5]=(float)(parent[5]*lift);
    }

    private static void start(Foot f, Point target, double yaw, double frame, double duration) {
        f.from=f.point; f.to=target; f.fromYaw=f.yaw; f.toYaw=yaw;
        f.started=frame; f.duration=duration; f.swinging=true; f.contact=false;
    }
    private static void advance(Foot f, double frame) {
        if (!f.swinging) return;
        double u=clamp((frame-f.started)/f.duration,0,1), s=ease(u);
        // Zero vertical velocity on lift-off and landing, with a visible bent-knee swing.
        double lift=.34*Math.pow(Math.sin(Math.PI*u),2);
        f.point=f.from.scale(1-s).add(f.to.scale(s)).add(new Point(0,lift,0));
        f.yaw=f.fromYaw+wrap(f.toYaw-f.fromYaw)*s;
        if (u>=1) { f.swinging=false; f.contact=true; }
    }
    private static Point home(int side, Point origin, double alpha, Ground ground) {
        return onGround(toWorld(new Point(HIP_X[side],0,0),origin,alpha),ground);
    }
    private static Point onGround(Point p, Ground ground) { return new Point(p.x,ground.height(p.x,p.z),p.z); }
    private static Point toWorld(Point p, Point origin, double a) {
        return origin.add(new Point((Math.cos(a)*p.x+Math.sin(a)*p.z)/16,p.y/16,(-Math.sin(a)*p.x+Math.cos(a)*p.z)/16));
    }
    private static Point toLocal(Point p, Point origin, double a) {
        Point d=p.subtract(origin).scale(16);
        return new Point(Math.cos(a)*d.x-Math.sin(a)*d.z,d.y,Math.sin(a)*d.x+Math.cos(a)*d.z);
    }
    private static double ease(double u) { return u*u*u*(10+u*(-15+6*u)); }
    private static double clamp(double v,double lo,double hi) { return Math.max(lo,Math.min(hi,v)); }
    private static double wrap(double degrees) { return degrees-360*Math.floor((degrees+180)/360); }

    /** Two rigid leg segments (20 and 16 pixels), with an 8-pixel boot below the ankle. */
    private static void solve(Map<String,float[]> pose, int side, Point sole, double toeYaw, double legReach) {
        String s=SIDES[side];
        float[] hip=pose.get(s+"_leg"), knee=pose.get(s+"_shin"), foot=pose.get(s+"_foot");
        float[] root=pose.get("root"), pelvis=pose.get("pelvis");
        double rootY=(root==null?0:root[4])+(pelvis==null?0:pelvis[4]);
        double dx=sole.x-HIP_X[side], dz=sole.z, ankleY=sole.y+8-rootY;
        double horizontal=Math.hypot(dx,dz);
        // Lower the hip within the overlapping armored joint instead of stretching a shin.
        double down=Math.min(44-ankleY, Math.sqrt(Math.max(1,legReach*legReach-horizontal*horizontal)));
        double lateralDown=Math.hypot(down,dx), reach=Math.hypot(lateralDown,dz);
        double bend=Math.acos(clamp((20*20+16*16-reach*reach)/(2*20*16),-1,1));
        double kneeAngle=bend-Math.PI;
        double hipAngle=Math.atan2(-dz,lateralDown)+Math.acos(clamp((20*20+reach*reach-16*16)/(2*20*Math.max(.001,reach)),-1,1));
        double roll=Math.atan2(dx,down);
        hip[0]=(float)hipAngle; hip[1]=0; hip[2]=(float)roll;
        hip[3]=0; hip[4]=(float)(ankleY+down-44); hip[5]=0;
        knee[0]=(float)kneeAngle; knee[1]=knee[2]=knee[3]=knee[4]=knee[5]=0;
        // Exact inverse rotation keeps heel AND toe level while hips roll and knees bend.
        double[] ankle=mul(mul(rx(-hipAngle-kneeAngle),rz(-roll)),ry(toeYaw));
        foot[0]=(float)Math.atan2(ankle[7],ankle[8]);
        foot[1]=(float)Math.asin(clamp(-ankle[6],-1,1));
        foot[2]=(float)Math.atan2(ankle[3],ankle[0]);
        foot[3]=foot[4]=foot[5]=0;
    }

    private static Point sole(Map<String,float[]> pose,int side) {
        String s=SIDES[side];
        float[] h=pose.get(s+"_leg"), k=pose.get(s+"_shin"), f=pose.get(s+"_foot");
        double[] hr=rotation(h), kr=mul(hr,rotation(k)), fr=mul(kr,rotation(f));
        float[] root=pose.get("root"), pelvis=pose.get("pelvis");
        double y=(root==null?0:root[4])+(pelvis==null?0:pelvis[4]);
        return new Point(HIP_X[side]+h[3],44+h[4]+y,h[5])
                .add(transform(hr,new Point(k[3],-20+k[4],k[5])))
                .add(transform(kr,new Point(f[3],-16+f[4],f[5])))
                .add(transform(fr,new Point(0,-8,0)));
    }
    private static double[] rotation(float[] p) { return mul(mul(rz(p[2]),ry(p[1])),rx(p[0])); }
    private static Point transform(double[] m,Point p) {
        return new Point(m[0]*p.x+m[1]*p.y+m[2]*p.z,m[3]*p.x+m[4]*p.y+m[5]*p.z,m[6]*p.x+m[7]*p.y+m[8]*p.z);
    }
    private static double[] mul(double[] a,double[] b) {
        double[] m=new double[9];
        for(int r=0;r<3;r++) for(int c=0;c<3;c++) for(int k=0;k<3;k++) m[r*3+c]+=a[r*3+k]*b[k*3+c];
        return m;
    }
    private static double[] rx(double a) { double c=Math.cos(a),s=Math.sin(a); return new double[]{1,0,0,0,c,-s,0,s,c}; }
    private static double[] ry(double a) { double c=Math.cos(a),s=Math.sin(a); return new double[]{c,0,s,0,1,0,-s,0,c}; }
    private static double[] rz(double a) { double c=Math.cos(a),s=Math.sin(a); return new double[]{c,-s,0,s,c,0,0,0,1}; }
}

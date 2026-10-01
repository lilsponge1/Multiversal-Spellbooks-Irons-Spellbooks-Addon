package com.crimson_susanoo.client;

import java.util.*;

/** Checks isolation from contact/hit poses and the continuity of the new idle/stride layer. */
public final class LocomotionPoseCheck {
    private static final String[] UPPER={"waist","head","left_arm","right_arm","left_forearm","right_forearm","left_hand","right_hand"};
    private static Map<String,float[]> pose() {
        Map<String,float[]> p=new HashMap<>();
        for(String name:UPPER) p.put(name,new float[6]);
        for(String side:new String[]{"left","right"}) for(String part:new String[]{"_leg","_shin","_foot"}) p.put(side+part,new float[6]);
        p.put("root",new float[6]); p.put("pelvis",new float[6]);
        return p;
    }
    private static void check(boolean ok,String why) { if(!ok) throw new AssertionError(why); }
    private static Map<String,float[]> copy(Map<String,float[]> p) {
        Map<String,float[]> c=new HashMap<>(); p.forEach((n,v)->c.put(n,v.clone())); return c;
    }
    private static double simulate(int fps) {
        LocomotionPose body=new LocomotionPose(); FootPlanting feet=new FootPlanting();
        double maxArm=0,minArm=0,last=0,maxJump=0;
        for(int i=0;i<=fps*8;i++) {
            double t=i*20.0/fps,z=Math.min(t,100)*.18;
            var p=pose(); feet.apply(t,0,0,z,0,0,true,p,(x,zz)->0);
            var before=copy(p);
            body.apply(t,0,z,0,.31,true,p);
            for(String name:before.keySet()) if(!Arrays.asList(UPPER).contains(name))
                check(Arrays.equals(p.get(name),before.get(name)),"upper-body layer modified planted feet or root: "+name);
            double arm=p.get("left_arm")[0];
            maxArm=Math.max(maxArm,arm); minArm=Math.min(minArm,arm);
            if(i>0) maxJump=Math.max(maxJump,Math.abs(arm-last));
            last=arm;
            var repeated=copy(before); body.apply(t,0,z,0,.31,true,repeated);
            for(String name:UPPER) check(Arrays.equals(p.get(name),repeated.get(name)),"repeat render changed "+name);
        }
        check(maxArm-minArm>Math.toRadians(5),"no meaningful counter-swing during real foot steps");
        check(maxJump<Math.toRadians(5),"upper-body jump exceeds five degrees per sampled frame");
        check(Math.abs(last)<Math.toRadians(1),"arms keep marching after stopping");
        return last;
    }
    public static void main(String[] args) {
        double a=simulate(30),b=simulate(60),c=simulate(144);
        check(Math.abs(a-b)<.001 && Math.abs(b-c)<.001,"resting pose depends on frame rate");
        LocomotionPose body=new LocomotionPose();
        double firstHead=0,maxHeadChange=0;
        for(int i=0;i<800;i++) {
            var p=pose(); body.apply(i*.25,0,0,0,0,true,p);
            if(i==0) firstHead=p.get("head")[1];
            maxHeadChange=Math.max(maxHeadChange,Math.abs(p.get("head")[1]-firstHead));
            check(Math.abs(p.get("right_hand")[0]-Math.toRadians(42))<Math.toRadians(1),"idle loses angled katana grip");
            check(p.get("waist")[4]>=0 && p.get("waist")[4]<.241,"breathing translation exceeds subtle envelope");
        }
        check(maxHeadChange>Math.toRadians(2),"idle head never surveys surroundings");
        var hit=pose(); for(String name:UPPER) hit.get(name)[0]=.6f;
        var exact=copy(hit); body.apply(200,0,0,0,0,false,hit);
        for(String name:exact.keySet()) check(Arrays.equals(hit.get(name),exact.get(name)),"authored attack/hurt/ending overwritten");
        var recovering=pose(); body.apply(200.25,0,0,0,0,true,recovering);
        check(Math.abs(recovering.get("left_arm")[0]-.6)<.09,"recovery snaps from the authored pose");
        AttackPoseBlend entry=new AttackPoseBlend();
        var resting=entry.apply(201,0,0,true,pose()); body.apply(201,0,0,0,0,true,resting); entry.remember(resting);
        var attack=entry.apply(201.25,1,.25,true,pose());
        check(Math.abs(attack.get("left_arm")[0]-resting.get("left_arm")[0])<1e-6,"attack entry loses visible locomotion stance");
        var contact=entry.apply(205.5,1,4.5,true,pose());
        check(contact.get("left_arm")[0]==0,"locomotion offset survives into authored strike");
        var fresh=new LocomotionPose(); var x=pose(); var y=pose();
        body.apply(300,100,100,90,.3,true,x); fresh.apply(300,100,100,90,.3,true,y);
        for(String name:UPPER) check(Arrays.equals(x.get(name),y.get(name)),"stale/offscreen stance leaked");
        System.out.println("Locomotion pose: real-foot counter-swing at 30/60/144 fps, idle breathing, resting grip, lower-body isolation, repeated renders, authored-state bypass, attack entry/recovery and stale-state reset passed.");
    }
}

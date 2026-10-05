package com.crimson_susanoo.client;

import java.nio.file.*;
import java.util.*;

/** Checks world-space boot corners by applying the exported rig's transforms independently. */
public final class FootPlantingCheck {
    private static final String[] BONES={"left_leg","left_shin","left_foot","right_leg","right_shin","right_foot"};
    private static int checks;
    private static double worstSlide, worstPenetration, largestHipDrop, largestContactBodyDrop;
    static Map<String,float[]> neutral() {
        Map<String,float[]> p=new HashMap<>();
        for(String bone:BONES) p.put(bone,new float[6]);
        p.put("root",new float[6]); p.put("pelvis",new float[6]);
        p.put("right_arm",new float[]{.2f,.3f,.1f,0,0,0});
        p.put("waist",new float[]{.18f,-.23f,.1f,0,0,0});
        p.put("ribcage",new float[]{-.07f,.3f,-.04f,0,0,0});
        p.put("right_shoulder",new float[]{.12f,-.14f,.08f,0,0,0});
        return p;
    }
    private static void require(boolean ok,String why) { checks++; if(!ok) throw new AssertionError(why); }
    private static double[] rotate(double[] p,float[] r) {
        double cx=Math.cos(r[0]),sx=Math.sin(r[0]),cy=Math.cos(r[1]),sy=Math.sin(r[1]),cz=Math.cos(r[2]),sz=Math.sin(r[2]);
        double y=p[1]*cx-p[2]*sx,z=p[1]*sx+p[2]*cx;
        double x=p[0]*cy+z*sy; z=-p[0]*sy+z*cy;
        return new double[]{x*cz-y*sz,x*sz+y*cz,z};
    }
    static double[] corner(Map<String,float[]> p,int side,double toe,double x,double y,double z,double yaw) {
        double hipX=side==0?-8:8;
        double[] point={hipX,0,toe};
        String s=side==0?"left":"right";
        String[] parts={"_foot","_shin","_leg"};
        double[] heights={8,24,44};
        for(int i=0;i<3;i++) {
            float[] pose=p.get(s+parts[i]);
            point[0]-=hipX; point[1]-=heights[i];
            point=rotate(point,pose);
            point[0]+=hipX+pose[3]; point[1]+=heights[i]+pose[4]; point[2]+=pose[5];
        }
        point[1]+=p.get("root")[4]+p.get("pelvis")[4];
        double a=Math.toRadians(180-yaw);
        return new double[]{x+(Math.cos(a)*point[0]+Math.sin(a)*point[2])/16,
                y+point[1]/16,z+(-Math.sin(a)*point[0]+Math.cos(a)*point[2])/16};
    }
    private static double distance(double[] a,double[] b) { return Math.sqrt(Math.pow(a[0]-b[0],2)+Math.pow(a[1]-b[1],2)+Math.pow(a[2]-b[2],2)); }
    private static double[][] checkPose(Map<String,float[]> p,double x,double y,double z,double yaw,double[][] previous,FootPlanting.Ground ground,String label) {
        double[][] now=new double[6][];
        for(int side=0;side<2;side++) {
            for(int i=0;i<3;i++) {
                int index=side*3+i;
                now[index]=corner(p,side,new double[]{0,-11,10}[i],x,y,z,yaw);
                double floor=ground.height(now[index][0],now[index][2]);
                double above=now[index][1]-floor;
                worstPenetration=Math.min(worstPenetration,above);
                if(above<=-.002) System.out.println(label+" side="+side+" corner="+i+" world="+Arrays.toString(now[index])+" pelvis="+Arrays.toString(p.get("pelvis"))+" hip="+Arrays.toString(p.get(side==0?"left_leg":"right_leg")));
                require(above>-.002,label+" boot below floor "+above);
                if(previous!=null && Math.abs(above)<.00001 && Math.abs(previous[index][1]-floor)<.00001) {
                    double slide=distance(now[index],previous[index]);
                    worstSlide=Math.max(worstSlide,slide);
                    require(slide<.00005,label+" planted corner slid "+slide+" foot="+side);
                }
            }
            float[] hip=p.get(side==0?"left_leg":"right_leg");
            if(-hip[4]>largestHipDrop && -hip[4]>10) System.out.println(label+" hip="+(-hip[4]));
            largestHipDrop=Math.max(largestHipDrop,-hip[4]);
            require(Float.isFinite(hip[0]) && Float.isFinite(hip[4]),label+" finite joints");
        }
        require(Math.abs(p.get("right_arm")[0]-.2)<.00001,label+" upper body changed");
        float[] arm=p.get("right_arm");
        double[] offset={arm[3],arm[4],arm[5]};
        for(String ancestor:new String[]{"right_shoulder","ribcage","waist","pelvis"})
            offset=rotate(offset,p.get(ancestor));
        double expected=Math.max(0,-p.get("pelvis")[4]-8);
        require(Math.abs(offset[0])<.00001 && Math.abs(offset[2])<.00001
                && Math.abs(offset[1]-expected)<.00001,label+" stair arm lift does not cancel extra blade lowering");
        require(-p.get("pelvis")[4]-offset[1]<=8.00001,label+" weapon support exceeds checked lowering");
        return now;
    }
    private static void walk(double speed,int fps) {
        FootPlanting feet=new FootPlanting();
        double[][] previous=null;
        int lifts=0; boolean wasLifted=false;
        double end=0;
        for(int sample=0;sample<=fps*7;sample++) {
            double t=sample*20.0/fps;
            double z=Math.min(t,80)*speed;
            Map<String,float[]> p=neutral();
            p.get("root")[4]=(float)(.35*Math.pow(Math.sin(t/5),2));
            feet.apply(t,0,0,z,0,0,true,p,(a,b)->0);
            previous=checkPose(p,0,0,z,0,previous,(a,b)->0,"walk "+speed+" fps="+fps+" t="+t);
            boolean lifted=previous[0][1]>.02 || previous[3][1]>.02;
            if(lifted&&!wasLifted) lifts++;
            wasLifted=lifted;
            if(t>110) require(!lifted,"stopped actor keeps marching");
            end=previous[0][1]+previous[3][1];
        }

        require(lifts>2,"moving actor has visible steps");
        require(Math.abs(end)<.00001,"stop finishes with both feet planted");
    }
    private static void turn() {
        FootPlanting feet=new FootPlanting(); double[][] previous=null;
        for(int i=0;i<480;i++) {
            double t=i/3.0;
            double angle=t<60?t*3:180; // Includes wrap across +/-180.
            double x=t<60?0:(Math.min(t-60,50)*.12);
            Map<String,float[]> p=neutral();
            feet.apply(t,x,0,0,angle,0,true,p,(a,b)->0);
            previous=checkPose(p,x,0,0,angle,previous,(a,b)->0,"turn t="+t);
        }
    }
    private static void resetAndIsolation() {
        FootPlanting a=new FootPlanting(), b=new FootPlanting();
        var p=neutral(); a.apply(0,0,0,0,0,0,true,p,(x,z)->0);
        p=neutral(); a.apply(.5,0,0,.5,0,0,true,p,(x,z)->0);
        var repeated=neutral(); a.apply(.5,0,0,.5,0,0,true,repeated,(x,z)->0);
        for(String name:BONES) require(Arrays.equals(p.get(name),repeated.get(name)),"repeat render altered pose");
        p=neutral(); a.apply(1,100,0,100,270,0,true,p,(x,z)->0);
        var fresh=neutral(); b.apply(1,100,0,100,270,0,true,fresh,(x,z)->0);
        for(String name:BONES) require(Arrays.equals(p.get(name),fresh.get(name)),"teleport or entity isolation");
        p=neutral(); p.get("left_leg")[0]=.47f;
        a.apply(1.5,100,0,100,270,0,false,p,(x,z)->0);
        require(p.get("left_leg")[0]==.47f,"ending / airborne pose overridden");
        p=neutral(); a.apply(40,100,0,100,270,0,true,p,(x,z)->0);
        for(String name:BONES) require(Arrays.equals(p.get(name),fresh.get(name)),"stale offscreen contact reused");
        FootPlanting stair=new FootPlanting();
        var step=neutral(); step.get("right_shin")[0]=-.9f;
        stair.apply(0,0,1,0,0,1,true,step,(x,z)->x>0?1:0);
        require(-step.get("pelvis")[4]>8,"fixture must exercise extra stair settling");
        var repeat=neutral();
        stair.apply(0,0,1,0,0,1,true,repeat,(x,z)->x>0?1:0);
        require(Arrays.equals(step.get("right_arm"),repeat.get("right_arm")),"repeat render loses stair arm lift");
        var ending=neutral(); ending.put("right_arm",step.get("right_arm").clone());
        stair.apply(.5,0,1,0,0,1,false,ending,(x,z)->x>0?1:0);
        for(int axis=3;axis<6;axis++) require(ending.get("right_arm")[axis]==0,"stair arm translation leaked into ending pose");
        require(ending.get("right_arm")[0]==step.get("right_arm")[0],"ending reset changed arm rotation");
    }
    private static void attacks(String fixture) throws Exception {
        for(int action=1;action<=3;action++) {
            FootPlanting feet=new FootPlanting();
            double[][] previous=null;
            for(String line:Files.readAllLines(Path.of(fixture))) {
                String[] fields=line.split("\t");
                if(Integer.parseInt(fields[0])!=action) continue;
                double t=Double.parseDouble(fields[1]);
                // Reproduce up to .72 blocks of forward server lunge and windup facing.
                double hit=action==1?11:action==2?14:20;
                double z=action==2?0:.72*Math.max(0,Math.min(1,(t-(hit-7))/6));
                double yaw=35*Math.min(1,t/Math.max(1,hit-4));
                var p=neutral();
                int n=2;
                for(String name:BONES) for(int j=0;j<6;j++) p.get(name)[j]=Float.parseFloat(fields[n++]);
                double[] knees={Math.abs(p.get("left_shin")[0]),Math.abs(p.get("right_shin")[0])};
                feet.apply(t,0,0,z,yaw,action,true,p,(a,b)->0);
                previous=checkPose(p,0,0,z,yaw,previous,(a,b)->0,"action="+action+" t="+t);
                checkKneeLoading(p,knees,"action="+action+" t="+t);
                if(Math.abs(t-hit)<.001) checkContactDrop(p);
            }
        }
    }
    private static void checkKneeLoading(Map<String,float[]> pose,double[] knees,String label) {
        for(int i=0;i<2;i++) {
            double solved=Math.abs(pose.get(i==0?"left_shin":"right_shin")[0]);
            require(solved+.00001>=knees[i],label+" erased authored knee loading: "+knees[i]+" -> "+solved);
        }
    }
    private static void checkContactDrop(Map<String,float[]> pose) {
        checkContactDrop(pose,8);
    }
    private static void checkContactDrop(Map<String,float[]> pose,double maximumDrop) {
        double drop=-pose.get("pelvis")[4];
        largestContactBodyDrop=Math.max(largestContactBodyDrop,drop);
        // This offset also moves the blade. Keep it in the separately checked
        // half-block contact corridor instead of validating only the authored clip.
        require(drop>=0 && drop<=maximumDrop,"combat support exceeds the fixture terrain height envelope");
    }
    private static void changingDirection() {
        FootPlanting feet=new FootPlanting(); double[][] previous=null;
        double x=0,z=0;
        for(int i=0;i<600;i++) {
            double t=i/3.0;
            double yaw=t<30?0:t<40?(t-30)*18:180;
            double speed=t<15?.22*t/15:t<100?.22:t<125?.22*(125-t)/25:0;
            x-=Math.sin(Math.toRadians(yaw))*speed/3;
            z+=Math.cos(Math.toRadians(yaw))*speed/3;
            var p=neutral(); feet.apply(t,x,0,z,yaw,0,true,p,(a,b)->0);
            previous=checkPose(p,x,0,z,yaw,previous,(a,b)->0,"curved pursuit t="+t);
        }
    }
    private static void splitSupport(double drop,double speed,int fps) {
        FootPlanting feet=new FootPlanting(); double[][] previous=null;
        FootPlanting.Ground ground=(x,z)->x>0 ? drop : 0;
        for(int i=0;i<=fps*7;i++) {
            double t=i*20.0/fps,z=Math.min(t,100)*speed;
            var p=neutral(); p.get("root")[4]=(float)(.35*Math.pow(Math.sin(t/5),2));
            feet.apply(t,0,drop,z,0,0,true,p,ground);
            previous=checkPose(p,0,drop,z,0,previous,ground,"split="+drop+" speed="+speed+" fps="+fps);
            require(p.get("pelvis")[4]>=-(drop<=.5?8:16),"support settling exceeds the fixture terrain height envelope");
            require(-p.get("left_leg")[4]<=10.00001 && -p.get("right_leg")[4]<=10.00001,"uneven-floor hip leaves covered joint");
            if(t>125) require(Math.abs(previous[0][1]-drop)<.00001 && Math.abs(previous[3][1])<.00001,"uneven stop leaves a floating foot");
            var repeated=neutral(); feet.apply(t,0,drop,z,0,0,true,repeated,ground);
            require(Arrays.equals(p.get("pelvis"),repeated.get("pelvis")),"repeat render loses pelvis support offset");
        }
    }
    private static void transitions(String fixture,double drop) throws Exception {
        FootPlanting.Ground ground=(x,z)->x>0 ? drop : 0;
        for(int action=1;action<=3;action++) for(int offset:new int[]{0,3,6}) {
            FootPlanting feet=new FootPlanting(); AttackPoseBlend blend=new AttackPoseBlend();
            double[][] previous=null;
            double start=12+offset, z=0, yaw=0;
            for(double t=0;t<start;t+=.25) {
                z=t*.12;
                var p=blend.apply(t,0,0,true,neutral());
                feet.apply(t,0,drop,z,0,0,true,p,ground); blend.remember(p);
                previous=checkPose(p,0,drop,z,0,previous,ground,"pre-attack walk");
            }
            double approach=z;
            double finish=start;
            for(String line:Files.readAllLines(Path.of(fixture))) {
                String[] f=line.split("\t"); if(Integer.parseInt(f[0])!=action) continue;
                double phase=Double.parseDouble(f[1]),t=start+phase;
                double hit=action==1?11:action==2?14:20;
                z=approach+(action==2?0:.72*Math.max(0,Math.min(1,(phase-(hit-7))/6)));
                yaw=drop==0 ? 25*Math.min(1,phase/Math.max(1,hit-4)) : 0;
                var p=neutral(); int n=2;
                for(String name:BONES) for(int j=0;j<6;j++) p.get(name)[j]=Float.parseFloat(f[n++]);
                p=blend.apply(t,action,phase,true,p);
                double[] knees={Math.abs(p.get("left_shin")[0]),Math.abs(p.get("right_shin")[0])};
                feet.apply(t,0,drop,z,yaw,action,true,p,ground); blend.remember(p);
                previous=checkPose(p,0,drop,z,yaw,previous,ground,"split="+drop+" blended attack="+action+" phase="+phase);
                checkKneeLoading(p,knees,"split="+drop+" blended attack="+action+" phase="+phase);
                if(Math.abs(phase-hit)<.001) require(Math.abs(previous[0][1]-drop)<.00001 && Math.abs(previous[3][1])<.00001,"both attack supports must land at contact");
                if(Math.abs(phase-hit)<.001) checkContactDrop(p,drop<=.5?8:16);
                finish=t;
            }
            double recoveryZ=z;
            for(double t=.25;t<30;t+=.25) {
                z=recoveryZ+Math.min(t,12)*.12;
                var p=blend.apply(finish+t,0,0,true,neutral());
                feet.apply(finish+t,0,drop,z,yaw,0,true,p,ground); blend.remember(p);
                previous=checkPose(p,0,drop,z,yaw,previous,ground,"post-attack movement");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        for(int fps:new int[]{30,60,144}) for(double speed:new double[]{.04,.12,.24,.32}) walk(speed,fps);
        turn(); changingDirection(); resetAndIsolation(); attacks(args[0]);
        for(double drop:new double[]{0,.25,.5,1}) transitions(args[0],drop);
        for(int fps:new int[]{30,60,144}) for(double speed:new double[]{.04,.12,.24,.32})
            for(double drop:new double[]{.25,.5,1}) splitSupport(drop,speed,fps);
        require(largestHipDrop<10.00001,"hip compensation exceeds hidden armor overlap: "+largestHipDrop);
        System.out.printf(Locale.ROOT,"Foot planting: %,d checks passed. Worst planted-corner drift %.8f blocks; penetration %.8f blocks; maximum hip compensation %.3f pixels.%n",checks,worstSlide,worstPenetration,largestHipDrop);
        System.out.printf(Locale.ROOT,"Authored combat knee loading retained; maximum contact pelvis drop %.3f pixels.%n",largestContactBodyDrop);
    }
}

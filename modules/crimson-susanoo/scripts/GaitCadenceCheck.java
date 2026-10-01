package com.crimson_susanoo.client;

import java.nio.file.*;
import java.util.*;

/** Observe actual world-space soles: alternation, cadence and airborne continuity. */
public final class GaitCadenceCheck {
    record Motion(double x,double z,double yaw) {}
    interface Route { Motion at(double tick); }
    private static boolean reportOnly;
    private static void require(boolean ok,String message) {
        if (!ok && !reportOnly) throw new AssertionError(message);
    }
    private static double wrap(double d) { return d-360*Math.floor((d+180)/360); }
    private static double run(String name,int fps,double speed,Route route,double end,boolean straight) {
        FootPlanting solver=new FootPlanting();
        boolean[] lifted={false,false};
        double[][] prior=new double[2][], priorVelocity=new double[2][];
        double lastLift=-1,lastEstablishedLift=-1,totalIntervals=0,minInterval=Double.POSITIVE_INFINITY;
        double maxAirVelocityChange=0,maxGroundDrift=0;
        int lastSide=-1,repeats=0,lifts=0,intervals=0;
        Motion priorMotion=null;
        for(int i=0;i<=end*fps/20;i++) {
            double tick=i*20.0/fps,dt=20.0/fps;
            Motion m=route.at(tick);
            boolean reset=priorMotion!=null && Math.hypot(m.x-priorMotion.x,m.z-priorMotion.z)>2.5;
            if(reset) {
                prior=new double[2][]; priorVelocity=new double[2][];
                lifted=new boolean[2]; lastSide=-1;
            }
            var pose=FootPlantingCheck.neutral();
            pose.get("root")[4]=(float)(.35*Math.pow(Math.sin(tick/5),2));
            solver.apply(tick,m.x,0,m.z,m.yaw,0,true,pose,(x,z)->0);
            for(int side=0;side<2;side++) {
                double[] sole=FootPlantingCheck.corner(pose,side,0,m.x,0,m.z,m.yaw);
                boolean up=sole[1]>.01;
                if(up&&!lifted[side]) {
                    lifts++;
                    if(side==lastSide) repeats++;
                    lastSide=side;
                    lastLift=tick;
                    if(tick>40) {
                        if(lastEstablishedLift>=0) {
                            double interval=tick-lastEstablishedLift;
                            minInterval=Math.min(minInterval,interval);
                            totalIntervals+=interval; intervals++;
                        }
                        lastEstablishedLift=tick;
                    }
                }
                if(prior[side]!=null) {
                    double[] velocity={(sole[0]-prior[side][0])/dt,(sole[1]-prior[side][1])/dt,(sole[2]-prior[side][2])/dt};
                    if(sole[1]<.00001 && prior[side][1]<.00001)
                        maxGroundDrift=Math.max(maxGroundDrift,Math.hypot(sole[0]-prior[side][0],sole[2]-prior[side][2]));
                    // Ignore takeoff/landing boundary; inspect the middle of each swing.
                    if(sole[1]>.12 && prior[side][1]>.12 && priorVelocity[side]!=null)
                        maxAirVelocityChange=Math.max(maxAirVelocityChange,Math.hypot(velocity[0]-priorVelocity[side][0],velocity[2]-priorVelocity[side][2])/dt);
                    priorVelocity[side]=velocity;
                }
                require(sole[1]>-.002,name+" sole penetrates floor");
                prior[side]=sole;
                lifted[side]=up;
            }
            priorMotion=m;
        }
        double average=intervals==0?0:totalIntervals/intervals;
        System.out.printf(Locale.ROOT,"%s fps=%d lifts=%d repeated=%d averageInterval=%.4f minInterval=%.4f maxAirAcceleration=%.5f drift=%.8f%n",name,fps,lifts,repeats,average,minInterval,maxAirVelocityChange,maxGroundDrift);
        require(maxGroundDrift<.00005,name+" grounded sole drift");
        require(lifts>2,name+" missing visible strides");
        if(straight) {
            require(repeats==0,name+" repeats the same foot in steady walking");
            require(average*speed>=.95,name+" overly rapid short-step cadence: "+average*speed+" blocks per step");
            require(maxAirVelocityChange<1.4,name+" airborne horizontal velocity has a mid-swing hitch");
        }
        return maxAirVelocityChange;
    }
    public static void main(String[] args) throws Exception {
        reportOnly=Arrays.asList(args).contains("--report");
        for(double speed:new double[]{.12,.156,.24,.32}) {
            double at60=0;
            for(int fps:new int[]{30,60,144}) {
                double acceleration=run("straight-"+speed,fps,speed,t->new Motion(0,t*speed,0),160,true);
                if(fps==60) at60=acceleration;
                if(fps==144) require(acceleration<at60*1.25,"mid-swing acceleration grows with frame rate at speed="+speed);
            }
        }
        for(int fps:new int[]{30,60,144}) {
            double speed=.16,rate=Math.toRadians(2.5);
            run("smooth-turn",fps,speed,t->new Motion(speed*(Math.cos(rate*t)-1)/rate,speed*Math.sin(rate*t)/rate,Math.toDegrees(rate*t)),160,false);
        }
        for(String arg:args) if(arg.endsWith(".csv")) {
            var rows=Files.readAllLines(Path.of(arg));
            List<double[]> values=new ArrayList<>();
            for(String line:rows.subList(1,rows.size())) {
                String[] fields=line.replace("\"","").split(",");
                values.add(new double[]{Double.parseDouble(fields[0]),Double.parseDouble(fields[2]),Double.parseDouble(fields[4]),Double.parseDouble(fields[5])});
            }
            double first=values.get(0)[0],last=values.get(values.size()-1)[0];
            for(int fps:new int[]{30,60,144}) run("recorded-flat-horizontal",fps,0,t->{
                double absolute=t+first;
                int index=Math.min(values.size()-2,Math.max(0,(int)((absolute-first)/5)));
                double[] a=values.get(index),b=values.get(index+1);
                double u=Math.max(0,Math.min(1,(absolute-a[0])/(b[0]-a[0])));
                return new Motion(a[1]+u*(b[1]-a[1]),a[2]+u*(b[2]-a[2]),a[3]+u*wrap(b[3]-a[3]));
            },last-first,false);
        }
    }
}

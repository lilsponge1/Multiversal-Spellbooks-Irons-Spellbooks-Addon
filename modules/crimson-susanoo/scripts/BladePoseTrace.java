package com.crimson_susanoo.client;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Drives production pose layers with actual full-rig animation samples. */
public final class BladePoseTrace {
    public static void main(String[] args) throws Exception {
        boolean baseline=args.length>2 && args[2].equals("baseline");
        var finish=baseline?null:FootPlanting.class.getDeclaredMethod("supportWeapon",Map.class);
        try(var input=Files.newBufferedReader(Path.of(args[0]));
            var output=Files.newBufferedWriter(Path.of(args[1]))) {
            String header=input.readLine();
            String[] names=header.split("\t");
            output.write(header); output.newLine();
            String scenario=""; FootPlanting feet=null; AttackPoseBlend blend=null; LocomotionPose movement=null;
            for(String line;(line=input.readLine())!=null;) {
                String[] f=line.split("\t");
                if(!f[0].equals(scenario)) {
                    scenario=f[0]; feet=new FootPlanting(); blend=new AttackPoseBlend(); movement=new LocomotionPose();
                }
                double frame=Double.parseDouble(f[1]),y=Double.parseDouble(f[2]),z=Double.parseDouble(f[3]);
                double yaw=Double.parseDouble(f[4]),phase=Double.parseDouble(f[6]); int action=Integer.parseInt(f[5]);
                Map<String,float[]> authored=new HashMap<>(); int n=7;
                for(String name:names) {
                    float[] values=new float[6]; for(int j=0;j<6;j++)values[j]=Float.parseFloat(f[n++]);
                    authored.put(name,values);
                }
                authored.get("pelvis")[4]=0;
                float[] arm=authored.get("right_arm"); arm[3]=arm[4]=arm[5]=0;
                var pose=blend.apply(frame,action,phase,true,authored);
                feet.apply(frame,0,y,z,yaw,action,true,pose,(x,zz)->x<0?y:0);
                movement.apply(frame,0,z,yaw,1.71,action==0,pose);
                if(finish!=null)finish.invoke(null,pose);
                blend.remember(pose);
                for(int i=0;i<7;i++){if(i>0)output.write('\t');output.write(f[i]);}
                for(String name:names)for(float value:pose.get(name)){output.write('\t');output.write(Float.toString(value));}
                output.newLine();
            }
        }
    }
}
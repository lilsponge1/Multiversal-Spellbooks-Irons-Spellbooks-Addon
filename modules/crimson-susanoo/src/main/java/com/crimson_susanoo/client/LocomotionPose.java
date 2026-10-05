package com.crimson_susanoo.client;

import java.util.HashMap;
import java.util.Map;

/** Quiet idle motion and upper-body counterbalance driven by the solved legs. */
final class LocomotionPose {
    private static final String[] BONES = {"waist", "head", "left_arm", "right_arm",
            "left_forearm", "right_forearm", "left_hand", "right_hand"};
    private final Map<String, float[]> previous = new HashMap<>();
    private double lastFrame = Double.NEGATIVE_INFINITY;
    private double lastX, lastZ, lastYaw;

    void apply(double frame, double x, double z, double yaw, double idlePhase,
               boolean eligible, Map<String, float[]> pose) {
        if (frame == lastFrame) {
            previous.forEach((name, value) -> pose.put(name, value.clone()));
            return;
        }
        double dt = frame - lastFrame;
        boolean fresh = dt > 0 && dt <= 2 && Math.hypot(x-lastX,z-lastZ) < 2.5;
        if (eligible) {
            double speed = fresh ? Math.hypot(x-lastX,z-lastZ)/dt : 0;
            double turn = fresh ? wrap(yaw-lastYaw)/dt : 0;
            double amount = Math.min(1, speed/.16 + Math.abs(turn)/24);
            float[] left = pose.get("left_leg"), right = pose.get("right_leg");
            // Read the actual planted stance, so arms do not run on an unrelated walk timer.
            double stride = left == null || right == null ? 0
                    : clamp((left[0]-right[0])/.7,-1,1)*amount;
            double breath = Math.sin(frame*.075 + idlePhase);
            double scan = Math.sin(frame*.023 + idlePhase)*(1-amount);
            double lean = clamp(turn/18,-1,1)*1.2;
            Map<String, float[]> target = new HashMap<>();
            rotation(target,"waist", .55*breath, 3.4*stride, lean);
            rotation(target,"head", -.4*breath, -2.2*stride+3*scan, -.5*lean);
            rotation(target,"left_arm", -10*stride+.65*breath, 0, .8*breath);
            rotation(target,"right_arm", 4*stride+.45*breath, 0, -.5*breath);
            rotation(target,"left_forearm", 7-3*stride+.5*breath, 0, 0);
            rotation(target,"right_forearm", 8+2*stride+.4*breath, 0, 0);
            rotation(target,"left_hand", -1.2*stride, 0, 0);
            rotation(target,"right_hand", 42+1.5*stride+.7*breath, 0, 3.5);
            // Same accepted counterbalance, reflected with the right-handed rig.
            target.values().forEach(p -> { p[1] = -p[1]; p[2] = -p[2]; });
            // Move the upper torso only; foot contacts and lower-body geometry are unaffected.
            target.get("waist")[4] = (float)(.12*(1+breath));
            double blend = fresh ? 1-Math.exp(-dt/2) : 1;
            target.forEach((name, desired) -> {
                float[] current = pose.get(name);
                if (current == null) return;
                float[] old = previous.get(name);
                for (int i=0; i<6; i++) {
                    double value = desired[i];
                    if (fresh && old != null) {
                        double delta = value-old[i];
                        if (i<3) delta = Math.atan2(Math.sin(delta),Math.cos(delta));
                        value = old[i]+delta*blend;
                    }
                    current[i] = (float)value;
                }
            });
        }
        // Observe authored attacks/hurt too, so recovery starts at their visible pose.
        previous.clear();
        for (String name:BONES) if (pose.containsKey(name)) previous.put(name,pose.get(name).clone());
        lastFrame=frame; lastX=x; lastZ=z; lastYaw=yaw;
    }

    private static void rotation(Map<String,float[]> target,String bone,double x,double y,double z) {
        target.put(bone,new float[]{(float)Math.toRadians(x),(float)Math.toRadians(y),
                (float)Math.toRadians(z),0,0,0});
    }
    private static double clamp(double value,double min,double max) { return Math.max(min,Math.min(max,value)); }
    private static double wrap(double degrees) { return degrees-360*Math.floor((degrees+180)/360); }
}

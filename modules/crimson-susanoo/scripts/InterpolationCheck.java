package com.crimson_susanoo.animation;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.keyframe.*;
import software.bernie.geckolib.core.molang.expressions.MolangValue;
import java.nio.file.*;
import java.util.*;

/** Exercise the production easing through GeckoLib's actual AnimationPoint API. */
public final class InterpolationCheck {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static double at(SmoothKeyframeEasing easing, Keyframe<IValue> key, double u) {
        return easing.apply(new AnimationPoint(key, key.length()*u, key.length(),
                key.startValue().get(), key.endValue().get()));
    }
    private static IValue value(double number, boolean position) {
        Constant constant = new Constant(number);
        return position ? new MolangValue(constant) : constant;
    }
    public static void main(String[] args) throws Exception {
        Map<String,List<double[]>> tracks = new LinkedHashMap<>();
        for (String row : Files.readAllLines(Path.of(args[0]))) {
            String[] parts = row.split("\t");
            tracks.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(
                    new double[]{Double.parseDouble(parts[1]), Double.parseDouble(parts[2])});
        }
        for (var entry : tracks.entrySet()) {
            var nodes = entry.getValue();
            boolean position = entry.getKey().contains("/position/");
            List<Keyframe<IValue>> keys = new ArrayList<>();
            keys.add(new Keyframe<>(0, value(nodes.get(0)[1],position), value(nodes.get(0)[1],position), EasingType.LINEAR));
            for (int i=1; i<nodes.size(); i++) keys.add(new Keyframe<>(
                    20*(nodes.get(i)[0]-nodes.get(i-1)[0]),
                    value(nodes.get(i-1)[1],position), value(nodes.get(i)[1],position), EasingType.LINEAR));
            var stack = new KeyframeStack<Keyframe<IValue>>(keys,List.of(),List.of());
            var animation = new Animation(entry.getKey(), 60, Animation.LoopType.PLAY_ONCE,
                    new BoneAnimation[]{new BoneAnimation("test",stack,new KeyframeStack<>(),new KeyframeStack<>())},
                    new Animation.Keyframes(new software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData[0],
                            new software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData[0],
                            new software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData[0]));
            var easing = new SmoothKeyframeEasing();
            easing.prepare(new AnimationProcessor.QueuedAnimation(animation,Animation.LoopType.PLAY_ONCE));
            for (int i=1; i<keys.size(); i++) {
                var key=keys.get(i); double a=key.startValue().get(), b=key.endValue().get();
                check(at(easing,key,0)==a && at(easing,key,1)==b,entry.getKey()+" changed a key pose");
                for (int fps : new int[]{30,60,144}) {
                    for (int frame=0; frame<=Math.ceil(key.length()*fps/20); frame++) {
                        double u=Math.min(1,frame*20.0/(fps*key.length())), value=at(easing,key,u);
                        check(Double.isFinite(value) && value>=Math.min(a,b)-1e-9 && value<=Math.max(a,b)+1e-9,
                                entry.getKey()+" overshoot or non-finite pose");
                    }
                }
                if(i+1<keys.size()) {
                    var next=keys.get(i+1); double epsilon=1e-5;
                    double before=(b-at(easing,key,1-epsilon))/(key.length()*epsilon);
                    double after=(at(easing,next,epsilon)-b)/(next.length()*epsilon);
                    check(Math.abs(before-after)<.003*Math.max(1,Math.max(Math.abs(before),Math.abs(after))),
                            entry.getKey()+" velocity discontinuity at key "+i+": "+before+" -> "+after);
                }
            }
            double transition=EasingType.EASE_IN_OUT_SINE.apply(new AnimationPoint(null,1,3,5,11));
            check(Math.abs(easing.apply(new AnimationPoint(null,1,3,5,11))-transition)<1e-10,"transition easing");
        }
        // The JSON loader keeps numeric translations in MolangValue wrappers.
        // This deliberately uneven track distinguishes cubic easing from linear.
        var wrappedKeys = List.of(
                new Keyframe<IValue>(0,value(0,true),value(0,true),EasingType.LINEAR),
                new Keyframe<IValue>(1,value(0,true),value(1,true),EasingType.LINEAR),
                new Keyframe<IValue>(1,value(1,true),value(4,true),EasingType.LINEAR));
        var wrappedAnimation = new Animation("position-wrapper",2,Animation.LoopType.PLAY_ONCE,
                new BoneAnimation[]{new BoneAnimation("test",new KeyframeStack<>(),
                        new KeyframeStack<>(wrappedKeys,List.of(),List.of()),new KeyframeStack<>())},
                new Animation.Keyframes(new software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData[0],
                        new software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData[0],
                        new software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData[0]));
        var wrappedEasing = new SmoothKeyframeEasing();
        wrappedEasing.prepare(new AnimationProcessor.QueuedAnimation(wrappedAnimation,Animation.LoopType.PLAY_ONCE));
        check(Math.abs(at(wrappedEasing,wrappedKeys.get(1),.5)-.3125)<1e-10,
                "numeric Molang position wrappers bypassed cubic interpolation");
        var first=new ClientAnimationClock(); var second=new ClientAnimationClock();
        check(first.elapsed(100,108,21,.5)==8.5,"late receipt preserves server phase");
        check(second.elapsed(100,108,0,.5)==8.5,"different entity ages share receipt phase");
        check(first.elapsed(100,80,22,.5)==9.5,"backward world-time sync caused a pose jump");
        check(first.elapsed(100,1000,23,.5)==10.5,"forward world-time sync caused a pose jump");
        check(first.elapsed(1001,1003,24,.5)==2.5,"new attack timestamp was not observed");
        check(first.elapsed(1001,1004,25,.5)==3.5,"new attack did not advance continuously");
        check(second.elapsed(2000,1999,1,-1)==0,"negative time/partial clamp");
        System.out.println("Interpolation/clock: "+tracks.size()+" exported axis tracks; "+checks
                +" checks passed through GeckoLib's production easing API (30/60/144 fps, exact keys, bounded curves, continuous velocity, clock corrections).");
    }
}

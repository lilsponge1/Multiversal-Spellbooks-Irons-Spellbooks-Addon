package com.crimson_susanoo.animation;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import software.bernie.geckolib.core.molang.expressions.MolangValue;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Shape-preserving cubic interpolation of the existing poses, at render time. */
public final class SmoothKeyframeEasing implements EasingType {
    private Animation animation;
    private final Map<Keyframe<?>, double[]> tangents = new IdentityHashMap<>();

    public EasingType prepare(AnimationProcessor.QueuedAnimation queued) {
        Animation next = queued == null ? null : queued.animation();
        if (next != animation) {
            animation = next;
            tangents.clear();
            if (next != null) {
                for (var bone : next.boneAnimations()) {
                    prepare(bone.rotationKeyFrames());
                    prepare(bone.positionKeyFrames());
                }
            }
        }
        return this;
    }

    private void prepare(KeyframeStack<Keyframe<IValue>> stack) {
        prepare(stack.xKeyframes());
        prepare(stack.yKeyframes());
        prepare(stack.zKeyframes());
    }

    private void prepare(List<Keyframe<IValue>> track) {
        // Only constant poses are cached. Resource packs using Molang retain
        // GeckoLib's own easing rather than freezing an evaluated expression.
        // GeckoLib unwraps numeric rotations into Constant, but numeric position
        // values retain their constant MolangValue wrapper after JSON loading.
        if (track.stream().anyMatch(k -> !isConstant(k.startValue())
                || !isConstant(k.endValue()))) return;
        for (int i = 0; i < track.size(); i++) {
            var key = track.get(i);
            double h = key.length();
            if (h <= 0) continue;
            double s = (key.endValue().get() - key.startValue().get()) / h;
            double m0 = 0, m1 = 0;
            if (i > 0 && track.get(i - 1).length() > 0) {
                var previous = track.get(i - 1);
                m0 = tangent((previous.endValue().get() - previous.startValue().get())
                        / previous.length(), s, previous.length(), h);
            }
            if (i + 1 < track.size() && track.get(i + 1).length() > 0) {
                var following = track.get(i + 1);
                m1 = tangent(s, (following.endValue().get() - following.startValue().get())
                        / following.length(), h, following.length());
            }
            tangents.put(key, new double[]{m0, m1});
        }
    }

    private static boolean isConstant(IValue value) {
        return value instanceof Constant || value instanceof MolangValue molang && molang.isConstant();
    }

    public static double tangent(double previous, double next, double previousLength, double nextLength) {
        if (previous * next <= 0) return 0;
        double a = 2 * nextLength + previousLength, b = nextLength + 2 * previousLength;
        return (a + b) / (a / previous + b / next);
    }

    public static double interpolate(double start, double end, double m0, double m1, double length, double u) {
        u = Math.max(0, Math.min(1, u));
        double u2 = u * u, u3 = u2 * u;
        return (2 * u3 - 3 * u2 + 1) * start + (u3 - 2 * u2 + u) * length * m0
                + (-2 * u3 + 3 * u2) * end + (u3 - u2) * length * m1;
    }

    @Override public Double2DoubleFunction buildTransformer(Double argument) { return u -> u; }

    @Override public double apply(AnimationPoint point, Double argument, double u) {
        if (point.keyFrame() == null || Math.abs(point.transitionLength() - point.keyFrame().length()) > .000001)
            return EasingType.EASE_IN_OUT_SINE.apply(point, argument, u);
        double[] slopes = tangents.get(point.keyFrame());
        if (slopes == null) return point.keyFrame().easingType().apply(point, argument, u);
        return interpolate(point.animationStartValue(), point.animationEndValue(),
                slopes[0], slopes[1], point.transitionLength(), u);
    }
}

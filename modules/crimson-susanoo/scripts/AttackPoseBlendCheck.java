package com.crimson_susanoo.client;

import java.util.Map;

/** Runs without Minecraft or desktop input; checks visual state isolation/timing. */
public final class AttackPoseBlendCheck {
    private static Map<String, float[]> pose(float angle) {
        return Map.of("right_arm", new float[] {angle, 0, 0, 0, 0, 0});
    }
    private static void near(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > 0.00001f) throw new AssertionError(message + ": " + actual);
    }
    public static void main(String[] args) {
        AttackPoseBlend blend = new AttackPoseBlend();
        blend.apply(10, 0, 0, true, pose(.4f));
        near(blend.apply(10.5, 1, .5, true, pose(0)).get("right_arm")[0], .4f, "entry continuity");
        near(blend.apply(12.5, 1, 2.5, true, pose(.1f)).get("right_arm")[0], .3f, "moving clip plus decaying offset");
        near(blend.apply(14.5, 1, 4.5, true, pose(.2f)).get("right_arm")[0], .2f, "authored pose restored before strike");
        near(blend.apply(14.5, 1, 4.5, true, pose(.2f)).get("right_arm")[0], .2f, "repeat render is idempotent");
        near(new AttackPoseBlend().apply(15, 1, 5, true, pose(.7f)).get("right_arm")[0], .7f, "late tracker has no foreign history");
        blend.apply(16, 0, 0, true, pose(.4f));
        near(blend.apply(30, 1, 1, true, pose(.8f)).get("right_arm")[0], .8f, "offscreen gap discards stale pose");
        blend.apply(31, 0, 0, true, pose(.4f));
        blend.apply(31.5, 1, .5, true, pose(0));
        near(blend.apply(32, 1, 1, false, pose(1)).get("right_arm")[0], 1, "ending bypasses blend");
        AttackPoseBlend feet = new AttackPoseBlend();
        for (int step = 0; step <= 40; step++) {
            double frame = step / 10.0;
            float hip = step == 0 ? .4f : -.5f;
            float knee = step == 0 ? .3f : .5f;
            float y = (float) -(36 - 20 * Math.cos(hip) - 16 * Math.cos(hip + knee));
            var result = feet.apply(frame, step == 0 ? 0 : 1, frame, true, Map.of(
                    "left_leg", new float[] {hip, 0, 0, 0, y, 0},
                    "left_shin", new float[] {knee, 0, 0, 0, 0, 0},
                    "left_foot", new float[] {-hip-knee, 0, 0, 0, 0, 0}));
            float[] h = result.get("left_leg"), k = result.get("left_shin");
            double sole = h[4] + 36 - 20 * Math.cos(h[0]) - 16 * Math.cos(h[0]+k[0]);
            if (sole < -.00001) throw new AssertionError("transition sole below floor: " + sole);
        }
        System.out.println("Pose blend: continuity, evolving clip, exact recovery, repeat render, late tracking, stale history, ending bypass and grounded transition passed.");
    }
}

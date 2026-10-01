package com.crimson_susanoo.client;

import java.util.HashMap;
import java.util.Map;

/** Visual-only inertia at attack entry; never retimes the authoritative clip. */
final class AttackPoseBlend {
    private Map<String, float[]> previous = Map.of();
    private Map<String, float[]> offset = Map.of();
    private double lastFrame = Double.NEGATIVE_INFINITY;
    private double lastPhase;
    private double start;
    private int lastAction;
    private boolean wasEligible;

    void remember(Map<String, float[]> rendered) { previous = rendered; }

    Map<String, float[]> apply(double frame, int action, double phase, boolean eligible,
                               Map<String, float[]> authored) {
        if (frame == lastFrame) return previous;
        boolean fresh = frame > lastFrame && frame - lastFrame <= 2;
        boolean entering = action != 0 && (action != lastAction || phase < lastPhase);
        if (!eligible || !fresh || action == 0) offset = Map.of();
        if (eligible && wasEligible && fresh && entering && phase < 2) {
            offset = new HashMap<>();
            start = frame;
            authored.forEach((name, pose) -> {
                float[] old = previous.get(name);
                if (old == null) return;
                float[] delta = new float[6];
                for (int i = 0; i < 6; i++) {
                    double difference = old[i] - pose[i];
                    delta[i] = (float) (i < 3
                            ? Math.atan2(Math.sin(difference), Math.cos(difference)) : difference);
                }
                offset.put(name, delta);
            });
        }
        double u = Math.max(0, Math.min(1, (frame - start) / 4));
        // Quintic easing removes the offset with zero velocity/acceleration at both ends.
        float weight = (float) (1 - u * u * u * (10 + u * (-15 + 6 * u)));
        Map<String, float[]> result = new HashMap<>();
        authored.forEach((name, pose) -> {
            float[] blended = pose.clone();
            float[] delta = offset.get(name);
            if (delta != null) for (int i = 0; i < 6; i++) blended[i] += delta[i] * weight;
            result.put(name, blended);
        });
        if (!offset.isEmpty() && weight > 0) {
            // Interpolating two grounded joint poses can shorten a leg between them.
            // Preserve the rig's level sole and lift the hip offset only if needed.
            for (String side : new String[] {"left", "right"}) {
                float[] hip = result.get(side + "_leg");
                float[] knee = result.get(side + "_shin");
                float[] foot = result.get(side + "_foot");
                if (hip == null || knee == null || foot == null) continue;
                foot[0] = -hip[0] - knee[0];
                double sole = hip[4] + 36 - 20 * Math.cos(hip[0])
                        - 16 * Math.cos(hip[0] + knee[0]);
                if (sole < 0) hip[4] -= (float) sole;
            }
        }
        if (u >= 1) offset = Map.of();
        previous = result;
        lastFrame = frame;
        lastAction = action;
        lastPhase = phase;
        wasEligible = eligible;
        return result;
    }
}

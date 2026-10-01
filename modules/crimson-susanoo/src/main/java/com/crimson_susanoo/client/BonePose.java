package com.crimson_susanoo.client;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

/** Write a render-only pose without claiming animation channels on the next frame. */
final class BonePose {
    private BonePose() {}

    static void write(CoreGeoBone bone, float[] pose) {
        bone.updateRotation(pose[0], pose[1], pose[2]);
        bone.updatePosition(pose[3], pose[4], pose[5]);
        // GeckoLib clears these markers before setCustomAnimations. Leaving our
        // writes marked would suppress its next per-entity snapshot reset for
        // unkeyed channels, retaining a previous summon's death/terrain pose.
        // This clears bookkeeping only; the current render keeps its transform.
        bone.resetStateChanges();
    }
}

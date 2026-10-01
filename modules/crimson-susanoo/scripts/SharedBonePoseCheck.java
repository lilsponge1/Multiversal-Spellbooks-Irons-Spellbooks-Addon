package com.crimson_susanoo.client;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.*;

/** Uses installed GeckoLib's real per-entity snapshot/reset path, without Minecraft. */
public final class SharedBonePoseCheck {
    private static int checks;
    private static boolean baseline;
    private static final class Summon implements GeoAnimatable {
        public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        public double getTick(Object context) { return 0; }
    }
    private static final class Model implements CoreGeoModel<Summon> {
        final AnimationProcessor<Summon> processor = new AnimationProcessor<>(this);
        public CoreBakedGeoModel getBakedGeoModel(String name) { return null; }
        public AnimationProcessor<Summon> getAnimationProcessor() { return processor; }
        public Animation getAnimation(Summon summon, String name) { return null; }
        public void handleAnimations(Summon summon, long id, AnimationState<Summon> state) {}
    }
    private static void near(float actual, float expected, String label) {
        checks++;
        if (Math.abs(actual-expected)>1.0e-5F) throw new AssertionError(label+": "+actual+" expected "+expected);
    }
    private static void render(GeoBone bone, float[] pose) {
        if (baseline) {
            bone.updateRotation(pose[0],pose[1],pose[2]);
            bone.updatePosition(pose[3],pose[4],pose[5]);
        } else BonePose.write(bone,pose);
    }
    private static void evaluate(Model model, Summon summon, AnimatableManager<Summon> manager, double tick) {
        model.processor.tickAnimation(summon,model,manager,tick,new AnimationState<>(summon,0,0,0,false),true);
    }
    public static void main(String[] args) {
        baseline = args.length>0 && args[0].equals("--baseline");
        Model model = new Model();
        GeoBone bone = new GeoBone(null,"root",false,0.0,false,false);
        model.processor.registerGeoBone(bone);
        Summon old = new Summon(), fresh = new Summon();
        AnimatableManager<Summon> oldManager = new AnimatableManager<>(old);
        AnimatableManager<Summon> freshManager = new AnimatableManager<>(fresh);
        evaluate(model,old,oldManager,100);
        float[] fallen = {0,0,(float)Math.toRadians(80),0,-8,0};
        oldManager.getBoneSnapshotCollection().get("root").updateRotation(0,0,fallen[2]);
        oldManager.getBoneSnapshotCollection().get("root").updateOffset(0,-8,0);
        bone.setScaleX(.2F); bone.setScaleY(.2F); bone.setScaleZ(.2F);
        render(bone,fallen);
        near(bone.getRotZ(),fallen[2],"Current ending rotation preserved");
        near(bone.getPosY(),-8,"Current ending position preserved");
        near(bone.getScaleX(),.2F,"Current ending scale preserved");
        evaluate(model,fresh,freshManager,101);
        near(bone.getRotZ(),0,"New summon does not inherit death rotation");
        near(bone.getPosY(),0,"New summon does not inherit ground compensation");
        near(bone.getScaleX(),1,"New summon restores full scale");
        render(bone,new float[]{.1F,.2F,0,1,-3,2});
        evaluate(model,fresh,freshManager,102);
        near(bone.getRotX(),0,"Unkeyed rotation resets on the same summon");
        near(bone.getPosY(),0,"Unkeyed position resets on the same summon");
        // Alternate renders with two existing managers, each with different snapshots.
        oldManager.getBoneSnapshotCollection().get("root").updateRotation(.4F,0,0);
        oldManager.getBoneSnapshotCollection().get("root").stopRotAnim(200);
        for (int i=0;i<20;i++) {
            evaluate(model,old,oldManager,200);
            near(bone.getRotX(),.4F,"First owner's snapshot restored");
            render(bone,new float[]{.4F,0,0,0,-4,0});
            evaluate(model,fresh,freshManager,200);
            near(bone.getRotX(),0,"Second owner's snapshot remains isolated");
            near(bone.getPosY(),0,"Second owner's support remains isolated");
            render(bone,new float[]{0,0,0,0,2,0});
        }
        System.out.println("Shared bone pose: "+checks+" assertions passed using GeckoLib 4.8.4");
    }
}

package com.crimson_susanoo.client;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.entity.CrimsonEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class CrimsonModel extends GeoModel<CrimsonEntity> {
    // A renderer/model is shared; pose history must belong to each summon.
    private final Map<CrimsonEntity, AttackPoseBlend> poseBlends = new WeakHashMap<>();
    private final Map<CrimsonEntity, FootPlanting> footPlants = new WeakHashMap<>();
    private final Map<CrimsonEntity, LocomotionPose> locomotionPoses = new WeakHashMap<>();
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "geo/guardian.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "textures/entity/guardian.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "animations/guardian.animation.json");

    @Override public ResourceLocation getModelResource(CrimsonEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(CrimsonEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(CrimsonEntity entity) { return ANIMATIONS; }
    @Override public void setCustomAnimations(CrimsonEntity entity, long instanceId,
                                              AnimationState<CrimsonEntity> state) {
        Map<String, float[]> authored = new HashMap<>();
        var bones = getAnimationProcessor().getRegisteredBones();
        for (var bone : bones) authored.put(bone.getName(), new float[] {
                bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                bone.getPosX(), bone.getPosY(), bone.getPosZ() });
        // Pelvis translation belongs to per-entity ground support, not the shared
        // model's previous render. None of the authored clips translates this bone.
        if (authored.containsKey("pelvis")) authored.get("pelvis")[4] = 0;
        // No authored clip translates the sword arm; stair support owns it.
        if (authored.containsKey("right_arm")) {
            float[] arm = authored.get("right_arm");
            arm[3] = arm[4] = arm[5] = 0;
        }
        float partial = state.getPartialTick();
        double frame = entity.tickCount + partial;
        boolean active = !entity.isManifesting() && !entity.isDeadOrDying() && entity.getFade() == 0;
        var blend = poseBlends.computeIfAbsent(entity, ignored -> new AttackPoseBlend());
        var blended = blend.apply(frame, entity.getAction(), entity.getActionAnimationTick(partial), active, authored);
        double x = Mth.lerp(partial, entity.xOld, entity.getX());
        double y = Mth.lerp(partial, entity.yOld, entity.getY());
        double z = Mth.lerp(partial, entity.zOld, entity.getZ());
        double yaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        footPlants.computeIfAbsent(entity, ignored -> new FootPlanting()).apply(frame,x,y,z,yaw,
                entity.getAction(),active && entity.isVisuallyGrounded() && !entity.isPassenger(),blended,(fx,fz) -> {
                    var hit = entity.level().clip(new ClipContext(new Vec3(fx,y+.6,fz),
                            new Vec3(fx,y-1.1,fz),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
                    return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : y;
                });
        locomotionPoses.computeIfAbsent(entity, ignored -> new LocomotionPose()).apply(frame,x,z,yaw,
                entity.getId()*1.71,active && entity.getAction() == 0 && entity.hurtTime == 0
                        && entity.isVisuallyGrounded() && !entity.isPassenger(),blended);
        // Counterbalance can change arm translation and parent rotation. Apply
        // terrain weapon support last so both layers share the same final frame.
        FootPlanting.supportWeapon(blended);
        // The next attack blends from the actual planted and counterbalanced pose.
        blend.remember(blended);
        for (var bone : bones) {
            float[] pose = blended.get(bone.getName());
            if (pose == null) continue;
            BonePose.write(bone, pose);
        }
    }
    @Override public RenderType getRenderType(CrimsonEntity entity, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}

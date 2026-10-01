package com.crimson_susanoo.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/** Navigation owns walking turns; the attack owns its planted facing. */
public final class CrimsonMoveControl extends MoveControl {
    private final CrimsonEntity guardian;

    public CrimsonMoveControl(CrimsonEntity guardian) {
        super(guardian);
        this.guardian = guardian;
    }

    @Override
    public void tick() {
        if (guardian.getAction() != 0 || guardian.isManifesting()
                || guardian.isDismissing() || guardian.isDeadOrDying()) {
            operation = Operation.WAIT;
            guardian.setSpeed(0);
            guardian.setZza(0);
            guardian.setXxa(0);
            return;
        }
        boolean walking = operation == Operation.MOVE_TO;
        if (walking) walkTowardWaypoint();
        else super.tick();
        if (walking) {
            double dx = wantedX - guardian.getX();
            double dz = wantedZ - guardian.getZ();
            if (dx * dx + dz * dz > .01) {
                float desired = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float error = Mth.wrapDegrees(desired - guardian.getYRot());
                // Keep moving through a turn, but avoid running past the waypoint sideways.
                float alignment = Math.max(0, (float) Math.cos(Math.toRadians(error)));
                guardian.setSpeed(guardian.getSpeed() * (.35F + .65F * alignment));
            }
        }
    }

    private void walkTowardWaypoint() {
        operation = Operation.WAIT;
        double dx = wantedX - guardian.getX(), dz = wantedZ - guardian.getZ();
        double dy = wantedY - guardian.getY();
        if (dx * dx + dy * dy + dz * dz < MIN_SPEED_SQR) {
            guardian.setZza(0);
            return;
        }
        float desired = (float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90;
        guardian.setYRot(rotlerp(guardian.getYRot(), desired, 90));
        guardian.setSpeed((float) (speedModifier * guardian.getAttributeValue(Attributes.MOVEMENT_SPEED)));
        // Vanilla starts a wide mob's jump metres before a high waypoint. Let
        // collision physics step over each actual stair/slab riser first. Full
        // blocks still request the ordinary jump when they obstruct the next step.
        if (guardian.onGround() && dy > guardian.getStepHeight()
                && dx * dx + dz * dz > MIN_SPEED_SQR && !canTakeGroundedStep(dx, dz)) {
            guardian.getJumpControl().jump();
            operation = Operation.JUMPING;
        }
    }

    private boolean canTakeGroundedStep(double dx, double dz) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        double probe = Math.min(distance, .35);
        AABB next = guardian.getBoundingBox().move(dx / distance * probe, 0, dz / distance * probe)
                .deflate(1.0E-5);
        double rise = 0;
        for (var shape : guardian.level().getBlockCollisions(guardian, next)) {
            for (AABB box : shape.toAabbs()) {
                if (box.intersects(next)) rise = Math.max(rise, box.maxY - guardian.getY());
            }
        }
        if (rise > guardian.getStepHeight() + 1.0E-5) return false;
        return !guardian.level().getBlockCollisions(guardian, next.move(0, rise, 0)).iterator().hasNext();
    }

    @Override
    protected float rotlerp(float current, float desired, float maximum) {
        return super.rotlerp(current, desired, Math.min(maximum, 18));
    }
}

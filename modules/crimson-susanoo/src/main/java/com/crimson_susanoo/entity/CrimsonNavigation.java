package com.crimson_susanoo.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Skip redundant path prefixes only across an entirely clear, supported corridor. */
public final class CrimsonNavigation extends GroundPathNavigation {
    public CrimsonNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected net.minecraft.world.level.pathfinder.PathFinder createPathFinder(int maximumVisitedNodes) {
        nodeEvaluator = new SupportedWalkNodeEvaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new net.minecraft.world.level.pathfinder.PathFinder(nodeEvaluator, maximumVisitedNodes);
    }

    private static final class SupportedWalkNodeEvaluator extends net.minecraft.world.level.pathfinder.WalkNodeEvaluator {
        @Override
        public net.minecraft.world.level.pathfinder.BlockPathTypes getBlockPathType(
                net.minecraft.world.level.BlockGetter level, int x, int y, int z, Mob mob) {
            var type = super.getBlockPathType(level, x, y, z, mob);
            if (type != net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE) return type;
            // Vanilla wide-mob nodes may be WALKABLE when only an outer edge has
            // support. The model's narrower feet would then hang over a pit.
            // Match Path.getEntityPosAtNode's center and reserve the central 3x3
            // floor columns for grounded steps, retaining vanilla stairs/jumps.
            int centerOffset = (int) (mob.getBbWidth() + 1) / 2;
            BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean supported = false;
                // A stair transition can span two floor heights inside one footprint.
                for (int drop = 1; drop <= 2; drop++) {
                    floor.set(x + centerOffset + dx, y - drop, z + centerOffset + dz);
                    if (!level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) return net.minecraft.world.level.pathfinder.BlockPathTypes.BLOCKED;
            }
            return type;
        }
    }

    @Override
    protected double getGroundY(Vec3 destination) {
        // A wide node may be supported by slabs/stairs at the footprint's edge
        // while the column at its center is air. Vanilla then returns the integer
        // node Y, requesting a full jump for an actual half-block surface.
        double halfWidth = mob.getBbWidth() * .5;
        AABB support = new AABB(destination.x - halfWidth + 1.0E-5, destination.y - 1.00001,
                destination.z - halfWidth + 1.0E-5, destination.x + halfWidth - 1.0E-5,
                destination.y + 1.0E-5, destination.z + halfWidth - 1.0E-5);
        double surface = Double.NEGATIVE_INFINITY;
        for (var shape : level.getBlockCollisions(mob, support)) {
            for (AABB box : shape.toAabbs()) {
                if (box.maxY <= destination.y + 1.0E-5
                        && box.maxX > support.minX && box.minX < support.maxX
                        && box.maxZ > support.minZ && box.minZ < support.maxZ) {
                    surface = Math.max(surface, box.maxY);
                }
            }
        }
        return Double.isFinite(surface) ? surface : super.getGroundY(destination);
    }

    @Override
    protected void followThePath() {
        if (path != null && !path.isDone()) {
            int current = path.getNextNodeIndex();
            // Bounded lookahead avoids repeatedly turning toward a new path's offset start.
            // Keep elevation transitions on vanilla navigation.
            int last = current;
            for (int i = current + 1; i < Math.min(path.getNodeCount(), current + 9); i++) {
                if (Math.abs(path.getNode(i).y - mob.getY()) > .05) break;
                last = i;
            }
            for (int i = last; i > current; i--) {
                Vec3 next = path.getEntityPosAtNode(mob, i);
                if (canTraverseCorridor(next)) {
                    path.setNextNodeIndex(i);
                    break;
                }
            }
            Vec3 endpoint = path.getNextEntityPos(mob);
            // Vanilla's width-based tolerance can consume this wide mob's last
            // waypoint while it is still 6.5 blocks from a diagonal target.
            // Finish the clear, level approach before handing back to melee.
            // Retain vanilla handling for elevation changes or blocked corridors.
            if (path.getNextNodeIndex() == path.getNodeCount() - 1
                    && mob.position().distanceToSqr(endpoint) > .75 * .75
                    && canTraverseCorridor(endpoint)) {
                doStuckDetection(getTempMobPos());
                return;
            }
        }
        super.followThePath();
    }

    public boolean canTraverseCorridor(Vec3 destination) {
        Vec3 start = mob.position();
        if (!mob.onGround() || Math.abs(destination.y - start.y) > .05
                || start.distanceToSqr(destination) > 36) return false;
        // The enclosing rectangle is deliberately conservative on diagonals: it never
        // clips a corner. Check blocks rather than transient entities such as the owner.
        AABB corridor = mob.getBoundingBox().minmax(mob.getBoundingBox().move(destination.subtract(start)));
        if (!level.hasChunksAt(BlockPos.containing(corridor.minX, corridor.minY - 1, corridor.minZ),
                BlockPos.containing(corridor.maxX, corridor.maxY, corridor.maxZ))) return false;
        if (level.getBlockCollisions(mob, corridor.deflate(1.0E-5)).iterator().hasNext()
                || level.containsAnyLiquid(corridor)) return false;
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
        int y = (int) Math.floor(corridor.minY - .01);
        for (int x = (int) Math.floor(corridor.minX); x <= (int) Math.floor(corridor.maxX - 1.0E-5); x++) {
            for (int z = (int) Math.floor(corridor.minZ); z <= (int) Math.floor(corridor.maxZ - 1.0E-5); z++) {
                floor.set(x, y, z);
                var state = level.getBlockState(floor);
                if (!state.getFluidState().isEmpty() || !state.isFaceSturdy(level, floor, Direction.UP)) return false;
            }
        }
        return true;
    }
}

package com.crimson_susanoo.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Checks the guardian's entire eight-block volume before placing or teleporting it. */
public final class SafePlacement {
    private SafePlacement() {}

    public static boolean isClear(ServerLevel level, Entity entity, Vec3 position) {
        AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(position);
        if (!level.noCollision(entity, box)) return false;

        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX - 1.0E-6);
        int maxY = (int) Math.floor(box.maxY - 1.0E-6);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-6);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.getFluidState(cursor.set(x, y, z)).isEmpty()) return false;
                }
            }
        }
        return true;
    }
}

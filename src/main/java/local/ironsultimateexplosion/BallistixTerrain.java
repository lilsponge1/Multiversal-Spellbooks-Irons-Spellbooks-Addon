package local.ironsultimateexplosion;

import ballistix.common.blast.util.Blast;
import ballistix.common.blast.util.thread.raycast.ThreadDynamicRaycastBlast;
import ballistix.common.packet.NetworkHandler;
import ballistix.common.packet.type.client.particle.BlastParticleSpawnType;
import ballistix.common.packet.type.client.particle.PacketSpawnBlastParticle;
import ballistix.common.settings.BallistixConstants;
import ballistix.registers.BallistixSounds;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkDirection;

/** Ballistix's nuclear terrain raycast and block presentation, without its radiation phase. */
final class BallistixTerrain {
    static Supplier<Set<BlockPos>> calculate(ServerLevel level, BlockPos center) {
        ThreadDynamicRaycastBlast ray = new ThreadDynamicRaycastBlast(level, center,
                (int)BallistixConstants.EXPLOSIVE_NUCLEAR_SIZE,
                (float)BallistixConstants.EXPLOSIVE_NUCLEAR_ENERGY, null);
        ray.setDaemon(true);
        ray.setUncaughtExceptionHandler((thread, error) ->
                System.err.println("Grand Explosion nuclear terrain calculation failed: " + error));
        if (BallistixConstants.SHOULD_MULTITHREAD_RAYTRACING) ray.start();
        else ray.run();
        return () -> {
            if (ray.isAlive()) return null;
            if (!ray.isComplete) {
                System.err.println("Grand Explosion nuclear terrain calculation did not complete");
                return Set.of();
            }
            return ray.finishedBlocks;
        };
    }

    static int blocksPerPass() {
        return Math.max(1, (int)(864000.0 / Math.max(1.0, BallistixConstants.EXPLOSIVE_NUCLEAR_DURATION)));
    }

    static Explosion context(ServerLevel level, BlockPos center) {
        return new Explosion(level, null, null, null, center.m_123341_(), center.m_123342_(), center.m_123343_(),
                (float)(BallistixConstants.EXPLOSIVE_NUCLEAR_SIZE * 3), false, Explosion.BlockInteraction.DESTROY);
    }

    static boolean destroy(ServerLevel level, ServerPlayer caster, BlockPos center, BlockPos pos,
                           Set<BlockPos> raycastBlocks, Explosion context) {
        BlockState state = level.m_8055_(pos);
        if (!Blast.canBreakBlockState(level, state, pos, caster)) return false;
        BlockState replacement = Blocks.f_50016_.m_49966_();
        double dx = pos.m_123341_() - center.m_123341_();
        double dz = pos.m_123343_() - center.m_123343_();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (level.f_46441_.m_188501_() < 1.0 / (3.0 * Math.sqrt(horizontal))
                && !raycastBlocks.contains(pos.m_121945_(Direction.DOWN)))
            replacement = Blocks.f_50083_.m_49966_();
        state.m_60734_().m_7592_(level, pos, context);
        level.m_7731_(pos, replacement, 35);
        if (level.f_46441_.m_188501_() < 0.05f) {
            PacketSpawnBlastParticle debris = new PacketSpawnBlastParticle(pos, BlastParticleSpawnType.EXPLOSIVE_BLOCK_BREAK);
            for (ServerPlayer viewer : level.m_7726_().f_8325_.m_183262_(new ChunkPos(pos), false))
                NetworkHandler.CHANNEL.sendTo(debris, viewer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
        return true;
    }

    static void playNearbyBoom(ServerLevel level, BlockPos firstDestroyed) {
        for (ServerPlayer viewer : level.m_7726_().f_8325_.m_183262_(new ChunkPos(firstDestroyed), false))
            level.m_6263_(null, viewer.m_20185_(), viewer.m_20186_(), viewer.m_20189_(),
                    BallistixSounds.SOUND_NUCLEAREXPLOSION.get(), SoundSource.PLAYERS, 25.0f, 1.0f);
    }

    private BallistixTerrain() {}
}

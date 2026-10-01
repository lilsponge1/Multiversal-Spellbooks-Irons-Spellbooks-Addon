package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.config.ServerConfigs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public final class CraterManager {
    private static final Deque<Job> JOBS = new ArrayDeque<>();
    private static final int FALLBACK_BLOCK_BUDGET_PER_TICK = 128;

    static void enqueue(ServerLevel level, ServerPlayer player, Vec3 origin, long seed) {
        if (!ExplosionConfig.TERRAIN_DAMAGE.get() || !ServerConfigs.SPELL_GREIFING.get()) return;
        if (JOBS.size() >= 2) return;
        BlockPos center = new BlockPos((int)Math.floor(origin.f_82479_), (int)Math.floor(origin.f_82480_),
                (int)Math.floor(origin.f_82481_));
        if (ModList.get().isLoaded("ballistix")) {
            JOBS.addLast(new Job(level, player, center, BallistixTerrain.calculate(level, center), true));
            return;
        }
        int radius = ExplosionConfig.CRATER_RADIUS.get();
        if (radius == 0) return;
        Random random = new Random(seed);
        List<BlockPos> positions = new ArrayList<>();
        for (int y = -radius; y <= 0; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double irregular = radius + (random.nextDouble() - 0.5) * 1.5;
                    if (x*x + y*y + z*z <= irregular * irregular)
                        positions.add(center.m_7918_(x, y, z));
                }
            }
        }
        positions.sort(Comparator.comparingDouble(p -> p.m_123331_(center)));
        Set<BlockPos> ordered = new LinkedHashSet<>(positions);
        JOBS.addLast(new Job(level, player, center, () -> ordered, false));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Job> jobs = JOBS.iterator();
        while (jobs.hasNext()) {
            Job job = jobs.next();
            if (job.positions == null) {
                Set<BlockPos> calculated = job.calculation.get();
                if (calculated == null) continue;
                job.all = calculated;
                job.positions = calculated.iterator();
                if (job.ballistix) job.context = BallistixTerrain.context(job.level, job.center);
            }
            int budget = job.ballistix
                    ? (job.level.m_46467_() % 2 == 0 ? BallistixTerrain.blocksPerPass() : 0)
                    : FALLBACK_BLOCK_BUDGET_PER_TICK;
            while (budget-- > 0 && job.positions.hasNext()) {
                BlockPos pos = job.positions.next();
                if (job.ballistix) {
                    if (BallistixTerrain.destroy(job.level, job.player, job.center, pos, job.all, job.context)
                            && !job.sounded) {
                        BallistixTerrain.playNearbyBoom(job.level, pos);
                        job.sounded = true;
                    }
                } else {
                    if (!job.level.m_46749_(pos)) continue;
                    BlockState state = job.level.m_8055_(pos);
                    if (state.m_60795_() || state.m_60800_(job.level, pos) < 0 || job.level.m_7702_(pos) != null) continue;
                    if (ForgeHooks.onBlockBreakEvent(job.level, job.player.f_8941_.m_9290_(), job.player, pos) < 0) continue;
                    job.level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2);
                }
            }
            if (!job.positions.hasNext()) jobs.remove();
        }
    }

    private static final class Job {
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos center;
        final Supplier<Set<BlockPos>> calculation;
        final boolean ballistix;
        Set<BlockPos> all;
        Iterator<BlockPos> positions;
        Explosion context;
        boolean sounded;

        Job(ServerLevel level, ServerPlayer player, BlockPos center,
            Supplier<Set<BlockPos>> calculation, boolean ballistix) {
            this.level = level;
            this.player = player;
            this.center = center;
            this.calculation = calculation;
            this.ballistix = ballistix;
        }
    }

    private CraterManager() {}
}

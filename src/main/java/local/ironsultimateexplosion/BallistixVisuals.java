package local.ironsultimateexplosion;

import ballistix.common.blast.tier3.BlastNuclear;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/** Runs Ballistix's installed nuclear particle presentation without invoking its blast gameplay. */
final class BallistixVisuals {
    private final BlastNuclear nuclear;

    BallistixVisuals(ClientLevel level, ExplosionPacket packet) {
        BlockPos center = new BlockPos((int)Math.floor(packet.x()), (int)Math.floor(packet.y()),
                (int)Math.floor(packet.z()));
        nuclear = new BlastNuclear(level, center, null, null);
    }

    void tick(int age) {
        nuclear.ticksSinceBlastStart = age - 1;
        nuclear.produceParticles();
    }
}

package local.ironsultimateexplosion;

import com.gametechbc.traveloptics.entity.misc.TOScreenFlashEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Loaded only when Traveloptics is installed. Invokes its visual-only flash entity. */
final class TravelopticsFlash {
    static void show(ServerLevel level, Vec3 point) {
        TOScreenFlashEntity.createWhiteFlash(level, point, 48.0f, 0.8f, 2, 4, 14, false);
    }

    private TravelopticsFlash() {}
}

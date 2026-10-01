package local.ironsultimateexplosion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Keeps client classes behind a physical-side gate in common mixins. */
public final class ThundercrashHooks {
    public static boolean travel(LivingEntity e) {
        if (!e.m_9236_().f_46443_) return ThundercrashManager.owns(e);
        return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ThundercrashClient.travel(e)));
    }
    private ThundercrashHooks() {}
}

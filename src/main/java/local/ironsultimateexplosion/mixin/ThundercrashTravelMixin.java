package local.ironsultimateexplosion.mixin;
import local.ironsultimateexplosion.ThundercrashHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ThundercrashTravelMixin {
    @Inject(method = "m_7023_", at = @At("HEAD"), cancellable = true, remap = false)
    private void thundercrashTravel(Vec3 input, CallbackInfo ci) {
        if (ThundercrashHooks.travel((LivingEntity)(Object)this)) ci.cancel();
    }
}

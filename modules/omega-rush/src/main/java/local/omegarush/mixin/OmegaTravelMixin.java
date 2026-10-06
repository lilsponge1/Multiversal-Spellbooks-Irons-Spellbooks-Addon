package local.omegarush.mixin;
import local.omegarush.OmegaHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class OmegaTravelMixin {
    @Inject(method = "m_7023_", at = @At("HEAD"), cancellable = true, remap = false)
    private void omegaTravel(Vec3 input, CallbackInfo ci) {
        if (OmegaHooks.travel((LivingEntity)(Object)this)) ci.cancel();
    }
}

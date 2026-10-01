package local.ironsultimateexplosion.mixin;
import local.ironsultimateexplosion.ThundercrashClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ThundercrashLocalPlayerMixin {
    @Inject(method = "m_108640_", at = @At("HEAD"), cancellable = true, remap = false)
    private void thundercrashInput(CallbackInfo ci) {
        if (ThundercrashClient.sendInput((LocalPlayer)(Object)this)) ci.cancel();
    }
}

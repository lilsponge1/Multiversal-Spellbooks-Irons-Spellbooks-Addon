package local.omegarush.mixin;
import local.omegarush.OmegaClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class OmegaLocalPlayerMixin {
    @Inject(method = "m_108640_", at = @At("HEAD"), cancellable = true, remap = false)
    private void omegaInput(CallbackInfo ci) {
        if (OmegaClient.sendInput((LocalPlayer)(Object)this)) ci.cancel();
    }
}

package local.ironsultimateexplosion.mixin;

import io.redspace.ironsspellbooks.player.*;
import local.ironsultimateexplosion.SaiyanClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Route only Saiyan activations into the release-aware controller; preserve spell wheel and other spells. */
@Mixin(value = ClientInputEvents.class, remap = false)
public abstract class SaiyanCastInputMixin {
    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ExtendedKeyMapping;consume()Z"), remap = false)
    private static boolean saiyan$consume(ExtendedKeyMapping key) {
        boolean consumed = key.consume();
        return consumed && !SaiyanClient.keyConsumed(key);
    }
}

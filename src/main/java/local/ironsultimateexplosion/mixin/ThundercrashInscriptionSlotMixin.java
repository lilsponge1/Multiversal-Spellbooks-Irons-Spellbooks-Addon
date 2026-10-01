package local.ironsultimateexplosion.mixin;

import local.ironsultimateexplosion.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Iron's 3.16.3 slot accepts its exact scroll item; also admit our native Scroll subclass. */
@Mixin(targets = "io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu$4", remap = false)
public abstract class ThundercrashInscriptionSlotMixin {
    @Inject(method = "m_5857_", at = @At("RETURN"), cancellable = true, remap = false)
    private void thundercrashAcceptScroll(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (stack.m_150930_(ModItems.THUNDERCRASH_SCROLL.get())) ci.setReturnValue(true);
    }
}

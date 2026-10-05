package local.ignisarmorcompat.mixin;

import local.ignisarmorcompat.IgnisArmorPolicy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemEnchantabilityMixin {
    // Forge 1.20.1's official development name and production SRG name.
    // At least one must resolve: fail visibly on an unsupported runtime.
    @Inject(method = {"isEnchantable(Lnet/minecraft/world/item/ItemStack;)Z",
            "m_8120_(Lnet/minecraft/world/item/ItemStack;)Z"},
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void ignisArmorCompat$enchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Item item = (Item) (Object) this;
        if (IgnisArmorPolicy.appliesTo(item))
            cir.setReturnValue(item.getMaxStackSize(stack) == 1 && item.getEnchantmentValue(stack) > 0);
    }
}

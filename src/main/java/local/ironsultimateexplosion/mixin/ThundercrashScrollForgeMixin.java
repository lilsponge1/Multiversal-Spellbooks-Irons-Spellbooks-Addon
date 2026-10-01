package local.ironsultimateexplosion.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import local.ironsultimateexplosion.ModItems;
import local.ironsultimateexplosion.ModSpells;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep Iron's recipe validation and replace only a successfully produced Thundercrash scroll. */
@Mixin(value = ScrollForgeMenu.class, remap = false)
public abstract class ThundercrashScrollForgeMixin {
    @Inject(method = "setupResultSlot", at = @At("RETURN"), remap = false)
    private void thundercrashNamedScroll(AbstractSpell spell, CallbackInfo ci) {
        if (spell != ModSpells.THUNDERCRASH.get()) return;
        var slot = ((ScrollForgeMenu)(Object)this).getResultSlot();
        if (slot == null) return;
        ItemStack original = slot.m_7993_();
        if (original.m_41619_() || original.m_150930_(ModItems.THUNDERCRASH_SCROLL.get())) return;
        var data = ISpellContainer.get(original);
        if (data == null || data.getSpellAtIndex(0).getSpell() != spell) return;
        ItemStack named = new ItemStack(ModItems.THUNDERCRASH_SCROLL.get(), original.m_41613_());
        if (original.m_41783_() != null) named.m_41751_(original.m_41783_().m_6426_());
        slot.m_5852_(named);
    }
}

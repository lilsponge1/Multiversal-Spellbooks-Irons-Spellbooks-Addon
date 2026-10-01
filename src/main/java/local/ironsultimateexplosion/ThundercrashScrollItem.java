package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A registered item ID for /give; still an ordinary, inscribable Iron's scroll. */
public final class ThundercrashScrollItem extends Scroll implements IPresetSpellContainer {
    public ThundercrashScrollItem(Properties properties) { super(properties); }

    @Override public void initializeSpellContainer(ItemStack stack) { initialize(stack); }

    public static void initialize(ItemStack stack) {
        if (ModSpells.THUNDERCRASH.isPresent() && !ISpellContainer.isSpellContainer(stack))
            ISpellContainer.createScrollContainer(ModSpells.THUNDERCRASH.get(), 1, stack);
    }

    @Override public void m_6883_(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        initialize(stack);
        super.m_6883_(stack, level, holder, slot, selected);
    }

    @Override public void m_7373_(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flags) {
        initialize(stack);
        super.m_7373_(stack, level, tooltip, flags);
        tooltip.add(Component.m_237115_("item.irons_ultimate_explosion.thundercrash_scroll.forge"));
    }
}

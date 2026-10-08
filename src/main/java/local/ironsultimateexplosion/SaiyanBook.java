package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.item.UniqueSpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class SaiyanBook extends UniqueSpellBook {
    public SaiyanBook() {
        super(SpellDataRegistryHolder.of(new SpellDataRegistryHolder(ModSpells.SAIYAN_ASCENSION, 1)), 0,
                new Item.Properties().m_41487_(1).m_41497_(Rarity.EPIC));
    }
    @Override public void m_7373_(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.m_7373_(stack, level, tooltip, flag);
        tooltip.add(Component.m_237115_("tooltip.irons_ultimate_explosion.saiyan_book").m_130940_(ChatFormatting.GOLD));
        tooltip.add(Component.m_237110_("ui.irons_ultimate_explosion.saiyan.controls", SaiyanConfig.HOLD.get() / 20.0));
    }
}

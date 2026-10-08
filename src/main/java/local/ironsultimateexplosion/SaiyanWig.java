package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.item.UniqueSpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

/** A locked spell container worn in Curios Head, leaving armor and spellbook slots free. */
public final class SaiyanWig extends UniqueSpellBook {
    static boolean worn(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve().map(h -> h.findCurios("head").stream()
                .anyMatch(r -> r.stack().m_150930_(ModItems.SAIYAN_WIG.get()))).orElse(false);
    }
    public SaiyanWig() {
        super(SpellDataRegistryHolder.of(new SpellDataRegistryHolder(ModSpells.SAIYAN_ASCENSION, 1)), 0,
                new Item.Properties().m_41487_(1).m_41497_(Rarity.EPIC));
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) { return "head".equals(context.identifier()); }
    @Override public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) { initializeSpellContainer(stack); }
    @Override public void m_7373_(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.m_7373_(stack, level, tooltip, flag);
        tooltip.add(Component.m_237115_("tooltip.irons_ultimate_explosion.saiyan_wig").m_130940_(ChatFormatting.GOLD));
        tooltip.add(Component.m_237110_("ui.irons_ultimate_explosion.saiyan.controls", SaiyanConfig.HOLD.get() / 20.0));
    }
}

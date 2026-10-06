package local.omegarush;
import io.redspace.ironsspellbooks.item.curios.CurioBaseItem;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import top.theillusivec4.curios.api.*;
import java.util.*;
public final class FloweryScarf extends CurioBaseItem {
    public FloweryScarf() { super(new Item.Properties().m_41487_(1).m_41497_(Rarity.EPIC)); }
    public static void prepare(ItemStack stack) {
        if(!ModSpells.OMEGA_FORM.isPresent()) return;
        var data=ISpellContainer.get(stack);
        if(data==null||data.getActiveSpellCount()!=1||data.getSpellAtIndex(0).getSpell()!=ModSpells.OMEGA_FORM.get()||!data.mustEquip()) {
            var mutable=ISpellContainer.create(1,true,true).mutableCopy();
            mutable.addSpell(ModSpells.OMEGA_FORM.get(),1,true);
            ISpellContainer.set(stack,mutable.toImmutable());
        }
    }
    @Override public ItemStack m_7968_() { ItemStack s=super.m_7968_(); prepare(s); return s; }
    @Override public void m_6883_(ItemStack s,Level l,Entity e,int slot,boolean selected) { if(!l.f_46443_) prepare(s); }
    @Override public void curioTick(SlotContext c,ItemStack s) { if(!c.cosmetic()&&!c.entity().m_9236_().f_46443_) prepare(s); }
    @Override public boolean canEquip(SlotContext c,ItemStack s) { return "charm".equals(c.identifier()); }
    @Override public void onEquip(SlotContext c,ItemStack s,ItemStack previous) { prepare(s); }
    @Override public void onUnequip(SlotContext c,ItemStack s,ItemStack next) {
        if(!c.cosmetic()&&c.entity() instanceof net.minecraft.server.level.ServerPlayer p&&!next.m_150930_(OmegaItems.SCARF.get())) OmegaFormManager.end(p,true);
    }
    public static boolean equipped(LivingEntity e) {
        return CuriosApi.getCuriosInventory(e).map(inv->inv.findCurios(s->s.m_150930_(OmegaItems.SCARF.get())).stream()
            .anyMatch(r->"charm".equals(r.slotContext().identifier())&&!r.slotContext().cosmetic())).orElse(false);
    }
    public static boolean visible(LivingEntity e){
        return CuriosApi.getCuriosInventory(e).map(inv->inv.findCurios(s->s.m_150930_(OmegaItems.SCARF.get())).stream()
            .anyMatch(r->"charm".equals(r.slotContext().identifier())&&!r.slotContext().cosmetic()&&r.slotContext().visible())).orElse(false);
    }
    @Override public void m_7373_(ItemStack s,Level l,List<Component> lines,TooltipFlag flag) {
        super.m_7373_(s,l,lines,flag);
        lines.add(Component.m_237115_("tooltip.irons_omega_rush.scarf"));
    }
}

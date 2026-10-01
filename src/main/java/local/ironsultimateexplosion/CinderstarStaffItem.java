package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CinderstarStaffItem extends StaffItem {
    public CinderstarStaffItem(Properties properties, double attackDamage, double attackSpeed,
                               Map<Attribute, AttributeModifier> attributes) {
        super(properties, attackDamage, attackSpeed, attributes);
    }

    @Override
    public void m_6883_(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.m_6883_(stack, level, holder, slot, selected);
        if (!level.m_5776_() && !ISpellContainer.isSpellContainer(stack))
            ISpellContainer.createImbuedContainer(ModSpells.GRAND_EXPLOSION.get(), 5, stack);
    }
}

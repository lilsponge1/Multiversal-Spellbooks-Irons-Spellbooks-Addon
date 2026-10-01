package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.armor.ExtendedArmorMaterials;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Pyromancer-grade caster armor with an original Cinderstar model. */
public final class CinderstarArmorItem extends ImbuableChestplateArmorItem {
    public CinderstarArmorItem(ArmorItem.Type type, Properties properties) {
        super(ExtendedArmorMaterials.PYROMANCER, type, properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(new CinderstarArmorModel());
    }

    public static boolean hasFullSet(LivingEntity wearer) {
        return wearer.m_6844_(EquipmentSlot.HEAD).m_150930_(ModItems.CINDERSTAR_HAT.get())
                && wearer.m_6844_(EquipmentSlot.CHEST).m_150930_(ModItems.CINDERSTAR_ROBE.get())
                && wearer.m_6844_(EquipmentSlot.LEGS).m_150930_(ModItems.CINDERSTAR_LEGGINGS.get())
                && wearer.m_6844_(EquipmentSlot.FEET).m_150930_(ModItems.CINDERSTAR_BOOTS.get());
    }
}

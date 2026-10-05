package local.ironsultimateexplosion;

import com.gametechbc.traveloptics.item.UnbreakableImbueableArmor;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Riptide-grade chest armor with exactly one Thundercrash-specific perk. */
public final class ThunderstarArmorItem extends UnbreakableImbueableArmor {
    public static final double THUNDERCRASH_DAMAGE_MULTIPLIER = 1.20;
    public ThunderstarArmorItem(Properties properties) {
        super(ThunderstarArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, properties);
    }
    @Override protected Set<ArmorItem.Type> getImbuableArmorTypes() { return Set.of(ArmorItem.Type.CHESTPLATE); }
    @Override protected Map<ArmorItem.Type, Integer> getMaxSpellSlots() { return Map.of(ArmorItem.Type.CHESTPLATE, 1); }
    // Do not inherit Travel Optics' full-suit potion effects or Riptide Stormline gameplay.
    @Override public void onArmorTick(ItemStack stack, Level level, Player player) {}
    @Override public String getArmorTexture(ItemStack stack, Entity wearer, EquipmentSlot slot, String layer) {
        // Forge forwards this path to the custom armor renderer as its texture override.
        return GrandExplosionMod.ID + ":textures/models/armor/thunderstar.png";
    }
    @Override public void m_7373_(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.m_7373_(stack, level, tooltip, flag);
        tooltip.add(Component.m_237115_("tooltip.irons_ultimate_explosion.thunderstar.perk").m_130940_(ChatFormatting.GOLD));
        tooltip.add(Component.m_237110_("tooltip.irons_ultimate_explosion.thunderstar.damage",
                Math.round((THUNDERCRASH_DAMAGE_MULTIPLIER - 1) * 100)).m_130940_(ChatFormatting.AQUA));
    }
    public static double thundercrashMultiplier(LivingEntity wearer) {
        return wearer.m_6844_(EquipmentSlot.CHEST).m_150930_(ModItems.THUNDERSTAR_CUIRASS.get())
                ? THUNDERCRASH_DAMAGE_MULTIPLIER : 1.0;
    }
    @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer() {
        GenericCustomArmorRenderer<ThunderstarArmorItem> renderer = new GenericCustomArmorRenderer<>(new ThunderstarArmorModel());
        renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
        return renderer;
    }
}

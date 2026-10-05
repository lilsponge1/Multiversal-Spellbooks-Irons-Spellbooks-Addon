package local.ironsultimateexplosion;

import com.gametechbc.traveloptics.api.item.armor.TravelopticsArmorMaterial;
import com.gametechbc.traveloptics.api.init.TravelopticsAttributes;
import com.gametechbc.traveloptics.item.TravelopticsArmorMaterials;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;

/** Delegates normal stats to the installed Riptide material, including pack configuration. */
public final class ThunderstarArmorMaterial implements TravelopticsArmorMaterial {
    public static final ThunderstarArmorMaterial INSTANCE = new ThunderstarArmorMaterial();
    private static TravelopticsArmorMaterials base() { return TravelopticsArmorMaterials.RIPTIDE_SOVEREIGN; }
    private ThunderstarArmorMaterial() {}
    @Override public int m_266425_(ArmorItem.Type type) { return base().m_266425_(type); }
    @Override public int m_7366_(ArmorItem.Type type) { return base().m_7366_(type); }
    @Override public int m_6646_() { return base().m_6646_(); }
    @Override public SoundEvent m_7344_() { return base().m_7344_(); }
    @Override public Ingredient m_6230_() { return base().m_6230_(); }
    @Override public String m_6082_() { return GrandExplosionMod.ID + ":thunderstar"; }
    @Override public float m_6651_() { return base().m_6651_(); }
    @Override public float m_6649_() { return base().m_6649_(); }
    @Override public Map<Attribute, AttributeModifier> getAdditionalAttributes() {
        Map<Attribute, AttributeModifier> attributes = new LinkedHashMap<>(base().getAdditionalAttributes());
        attributes.put(AttributeRegistry.LIGHTNING_SPELL_POWER.get(), new AttributeModifier(
                UUID.fromString("37b3209a-8d55-4a5e-a7c7-434dd1e851e8"), "Thunderstar lightning power", 0.10, AttributeModifier.Operation.MULTIPLY_BASE));
        attributes.put(TravelopticsAttributes.AQUA_SPELL_POWER.get(), new AttributeModifier(
                UUID.fromString("b321c10f-082a-4e91-809b-6f0442b44f58"), "Thunderstar aqua power", 0.05, AttributeModifier.Operation.MULTIPLY_BASE));
        // GeoArmorItem converts every entry to its established equipment-slot UUID.
        return Map.copyOf(attributes);
    }
}

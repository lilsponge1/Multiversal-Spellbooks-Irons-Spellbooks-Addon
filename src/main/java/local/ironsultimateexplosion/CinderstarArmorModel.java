package local.ironsultimateexplosion;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CinderstarArmorModel extends GeoModel<CinderstarArmorItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(GrandExplosionMod.ID, "geo/cinderstar_armor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandExplosionMod.ID, "textures/models/armor/cinderstar.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("irons_spellbooks", "animations/wizard_armor_animation.json");

    @Override public ResourceLocation getModelResource(CinderstarArmorItem item) { return MODEL; }
    @Override public ResourceLocation getTextureResource(CinderstarArmorItem item) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(CinderstarArmorItem item) { return ANIMATION; }
}

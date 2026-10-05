package local.ironsultimateexplosion;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ThunderstarArmorModel extends GeoModel<ThunderstarArmorItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(GrandExplosionMod.ID, "geo/thunderstar_armor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandExplosionMod.ID, "textures/models/armor/thunderstar.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(GrandExplosionMod.ID, "animations/thunderstar_armor.animation.json");
    @Override public ResourceLocation getModelResource(ThunderstarArmorItem item) { return MODEL; }
    @Override public ResourceLocation getTextureResource(ThunderstarArmorItem item) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(ThunderstarArmorItem item) { return ANIMATION; }
}

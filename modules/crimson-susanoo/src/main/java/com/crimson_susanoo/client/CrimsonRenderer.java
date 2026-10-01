package com.crimson_susanoo.client;

import com.crimson_susanoo.entity.CrimsonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CrimsonRenderer extends GeoEntityRenderer<CrimsonEntity> {
    public CrimsonRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new CrimsonFireMantle(this));
        shadowRadius = 1.7F;
    }

}

package com.crimson_susanoo.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Emissive fire with depth writes, so shader sky passes preserve its silhouette. */
final class CrimsonFireRenderType extends RenderType {
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private CrimsonFireRenderType() {
        super("crimson_fire", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                256, false, true, () -> {}, () -> {});
    }

    static RenderType fire(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, key -> create("crimson_fire",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)));
    }
}

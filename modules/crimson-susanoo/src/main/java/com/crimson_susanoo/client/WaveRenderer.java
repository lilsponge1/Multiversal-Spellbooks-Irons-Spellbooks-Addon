package com.crimson_susanoo.client;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.entity.CrimsonWave;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class WaveRenderer extends EntityRenderer<CrimsonWave> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "textures/entity/guardian.png");
    public WaveRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(CrimsonWave entity) { return TEXTURE; }
    @Override public void render(CrimsonWave entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        // The traveling blade is drawn with particles; the entity is its server-side hit volume.
    }
}

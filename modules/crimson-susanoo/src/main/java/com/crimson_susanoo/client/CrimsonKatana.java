package com.crimson_susanoo.client;

import com.crimson_susanoo.CrimsonSusanoo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Continuous curved steel blade with a narrow animated molten temper line. */
final class CrimsonKatana {
    private static final ResourceLocation MATERIAL = ResourceLocation.fromNamespaceAndPath(
            CrimsonSusanoo.MODID, "textures/entity/guardian_fire.png");
    private CrimsonKatana() { }

    static void render(MultiBufferSource buffers, Matrix4f pose, double time, int action, double actionTick) {
        VertexConsumer steel = buffers.getBuffer(RenderType.entityTranslucent(MATERIAL));
        for (int slice = 0; slice < 48; slice++) {
            float a = slice / 48F, b = (slice + 1) / 48F;
            for (int face = 0; face < 4; face++) {
                point(steel, pose, a, face, time);
                point(steel, pose, a, (face + 1) % 4, time);
                point(steel, pose, b, (face + 1) % 4, time);
                point(steel, pose, b, face, time);
            }
        }
        VertexConsumer glow = buffers.getBuffer(CrimsonFireRenderType.fire(MATERIAL));
        for (int side = -1; side <= 1; side += 2) for (int slice = 0; slice < 48; slice++) {
            float a = slice / 48F, b = (slice + 1) / 48F;
            seam(glow, pose, a, -1, side, time);
            seam(glow, pose, a, 1, side, time);
            seam(glow, pose, b, 1, side, time);
            seam(glow, pose, b, -1, side, time);
        }
        // Continuous attached tongues travel toward the tip. A smooth pulse
        // broadens them through the committed swing without obscuring the steel.
        double hit = action == 1 ? 11 : action == 2 ? 14 : 20;
        float surge = action == 0 ? 0 : (float)Math.exp(-Math.pow((actionTick-hit)/4.0, 2));
        for (int plane = 0; plane < 2; plane++) for (int slice = 0; slice < 64; slice++) {
            float a = slice / 64F, b = (slice + 1) / 64F;
            flame(glow, pose, a, 0, plane, time, surge);
            flame(glow, pose, a, 1, plane, time, surge);
            flame(glow, pose, b, 1, plane, time, surge);
            flame(glow, pose, b, 0, plane, time, surge);
        }
    }

    private static float width(float t) {
        return .42F * (1 - .24F * t) * Math.min(1, (1 - t) / .15F);
    }

    private static void point(VertexConsumer out, Matrix4f pose, float t, int edge, double time) {
        float w = width(t), thick = .095F * (1 - t * .7F);
        float cross = edge == 0 ? -1 : edge == 2 ? 1 : 0;
        float vertical = edge == 1 ? 1 : edge == 3 ? -1 : 0;
        float wave = .5F + .5F * (float)Math.sin(t * 25 - time * .11);
        float heat = .18F + wave * .11F;
        boolean cuttingEdge = edge == 2;
        emit(out, pose, -(24 + 4 * t * t) / 16F + cross * w / 2,
                (36 + 3 * t) / 16F + vertical * thick,
                -(18 + 80 * t) / 16F,
                cuttingEdge ? .95F : .32F + heat,
                cuttingEdge ? .54F : .055F + heat * .15F,
                cuttingEdge ? .22F : .065F, 1, t, (cross + 1) / 2, vertical);
    }

    private static void seam(VertexConsumer out, Matrix4f pose, float t, int edge, int side, double time) {
        float cross = .48F + .10F * (float)Math.sin(t * 31 - time * .075) + edge * .065F;
        float wave = .5F + .5F * (float)Math.sin(t * 24 - time * .16);
        float thick = .095F * (1 - t * .7F);
        emit(out, pose, -(24 + 4 * t * t) / 16F + cross * width(t) / 2,
                (36 + 3 * t) / 16F + side * (thick * (1 - cross) + .003F),
                -(18 + 80 * t) / 16F, 1, .24F + .4F * wave, .025F,
                .48F + .18F * wave, t, cross, side);
    }

    private static void flame(VertexConsumer out, Matrix4f pose, float t, int edge, int plane,
                              double time, float surge) {
        float wave = .5F + .5F * (float)Math.sin(t * 38 - time * .19 + plane * 2.4);
        float envelope = (float)Math.sin(Math.PI * t);
        float lift = edge * envelope * (.045F + wave * wave * (.15F + .19F * surge));
        float x = -(24 + 4*t*t)/16F + width(t)/2 + (plane == 0 ? lift : lift*.35F);
        float y = (36+3*t)/16F + (plane == 0 ? .008F : lift);
        emit(out, pose, x, y, -(18+80*t)/16F,
                1, edge == 0 ? .57F : .13F + wave*.24F, .025F,
                envelope * (edge == 0 ? .46F + surge*.15F : .035F),
                t, edge, 1);
    }

    private static void emit(VertexConsumer out, Matrix4f pose, float x, float y, float z,
                             float r, float g, float b, float alpha, float t, float u, float ny) {
        out.vertex(pose, x, y, z).color(r, g, b, alpha).uv(u, t)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, ny == 0 ? 1 : ny, 0).endVertex();
    }
}

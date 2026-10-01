package com.crimson_susanoo.client;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.entity.CrimsonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Bone-attached furnace, collar and eye-corner fire. Dependency textures are referenced. */
public final class CrimsonFireMantle extends GeoRenderLayer<CrimsonEntity> {
    // Opaque neutral texels preserve the full vertex color and opacity, independent
    // of GeckoLib's partially transparent armor glowmask.
    private static final ResourceLocation FIRE = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "textures/entity/guardian_fire.png");
    private static final ResourceLocation[] SWIRLS = new ResourceLocation[11];
    private Matrix4f rootPose;
    private Matrix4f chestPose;
    private Matrix4f swordPose;
    private Matrix4f eyesPose;
    static {
        for (int i = 0; i < SWIRLS.length; i++) SWIRLS[i] = ResourceLocation.fromNamespaceAndPath(
                "irons_spellbooks", "textures/entity/fireball/swirl_" + i + ".png");
    }
    public CrimsonFireMantle(GeoRenderer<CrimsonEntity> renderer) { super(renderer); }

    @Override
    public void renderForBone(PoseStack poses, CrimsonEntity entity, GeoBone bone,
                              RenderType type, MultiBufferSource buffers, VertexConsumer ignored,
                              float partialTick, int light, int overlay) {
        String name = bone.getName();
        // Capture the animated transforms, but draw after all armor and its glow layer.
        // Drawing translucent fire during recursion lets later armor overwrite it.
        if (name.equals("root")) {
            rootPose = new Matrix4f(poses.last().pose());
            chestPose = null;
            swordPose = null;
            eyesPose = null;
        } else if (name.equals("chest_armor")) {
            chestPose = new Matrix4f(poses.last().pose());
        } else if (name.equals("sword")) {
            swordPose = new Matrix4f(poses.last().pose());
        } else if (name.equals("eyes")) {
            eyesPose = new Matrix4f(poses.last().pose());
        }
    }

    @Override
    public void render(PoseStack poses, CrimsonEntity entity, BakedGeoModel model,
                       RenderType type, MultiBufferSource buffers, VertexConsumer ignored,
                       float partialTick, int light, int overlay) {
        // Double precision keeps rolling fire continuous across the world-day boundary.
        double time = entity.level().getGameTime() + (double)partialTick;
        if (swordPose != null) CrimsonKatana.render(buffers, swordPose, time,
                entity.getAction(), entity.getActionAnimationTick(partialTick));
        if (rootPose != null && entity.isManifesting())
            pillar(buffers, rootPose, entity.getManifestAnimationTick(partialTick), time);
        if (chestPose == null) return;
        Matrix4f matrix = chestPose;
        float strength = entity.isManifesting()
                ? (float) Math.max(0, Math.min(1, (entity.getManifestAnimationTick(partialTick) - 20) / 20)) : 1;
        if (entity.getFade() > 0 || entity.isDeadOrDying())
            strength *= (float) Math.max(0, 1 - entity.getEndingAnimationTick(partialTick) / 27);
        if (strength <= 0) return;
        // Standalone flame geometry needs depth writes for Solas's sky/composite
        // pass. The emissive overlay pass only writes color and clips the silhouette.
        // Keep emissive shading as well, so Solas does not light fire as armor.
        VertexConsumer base = buffers.getBuffer(RenderType.entityTranslucent(FIRE));
        mantle(base, matrix, time, strength, false);
        VertexConsumer out = buffers.getBuffer(CrimsonFireRenderType.fire(FIRE));
        if (eyesPose != null) {
            float eyeStrength = strength * (entity.isManifesting()
                    ? (float)Math.max(0,Math.min(1,(entity.getManifestAnimationTick(partialTick)-40)/7)) : 1);
            eyeCorners(out,eyesPose,time,eyeStrength);
        }
        // A few inner tongues glow; making all 50 overlapping tufts emissive
        // overexposes the entire scarf in Solas and loses its dark-red roots.
        mantle(out, matrix, time, strength * .24F, true);
        // Pulsing coal bed behind the rib grille, ahead of the spine, enclosed by rear armor.
        float heat = .82F + .08F * (float) Math.sin(time * .13);
        quad(out, matrix, -.29F, 4.45F, .29F, 5.28F, -.68F, strength, heat);
        for (int i = 0; i < 5; i++)
            tongue(out, matrix, -.24F + i * .12F, 4.48F, -.70F, .64F + .1F * (i % 2), time, i + 30, strength);
    }

    private static void eyeCorners(VertexConsumer out, Matrix4f matrix, double time, float strength) {
        // Short, tapered wisps peel outward from the outer eye sockets. The eyes
        // bone carries head turns and manifestation/ending scale automatically.
        for (int side=-1;side<=1;side+=2) for (int plane=0;plane<2;plane++) for (int row=0;row<10;row++) {
            float a=row/10F,b=(row+1)/10F;
            eyeVertex(out,matrix,side,plane,a,-1,time,strength);
            eyeVertex(out,matrix,side,plane,a,1,time,strength);
            eyeVertex(out,matrix,side,plane,b,1,time,strength);
            eyeVertex(out,matrix,side,plane,b,-1,time,strength);
        }
    }

    private static void eyeVertex(VertexConsumer out, Matrix4f matrix, int side, int plane,
                                  float t, int edge, double time, float strength) {
        double phase=time*(side<0 ? .13 : .15)+side*1.7;
        float curl=.035F*t*(float)Math.sin(t*7-phase);
        float width=.037F*(1-t)*(1+.12F*(float)Math.sin(t*8-phase));
        float x=side*(.59F+.33F*t+curl);
        float y=6.28F+.24F*t+curl+(plane==0 ? edge*width : 0);
        float z=-1.075F-.025F*t+(plane==1 ? edge*width : 0);
        emit(out,matrix,x,y,z,1,.48F-.25F*t,.035F,
                strength*.72F*(1-t),edge<0 ? .02F : .04F,.26F+t*.02F);
    }

    private static void mantle(VertexConsumer out, Matrix4f matrix, double time, float strength, boolean glow) {
        // An open rear horseshoe, not a closed neck ring. Negative Z is the
        // face: all roots stay beside or behind the neck, leaving the throat open.
        for (int layer = 0; layer < 2; layer++) for (int i = 0; i < 25; i++) {
            double angle = Math.PI * i / 24;
            float rear = (float) Math.sin(angle);
            float x = (float) Math.cos(angle) * (2.12F - layer * .20F);
            float z = .05F + rear * (.52F + layer * .08F);
            // Bury the base in the shoulder tops and rear cuirass. The previous
            // raised arc left a visible sky gap when viewed from underneath.
            float y = 5.94F - rear * .48F - layer * .16F;
            int seed = i + layer * 29;
            if (glow && (layer != 0 || seed % 4 != 0)) continue;
            float height = .98F + rear * .90F + .22F * (float) Math.sin(seed * 2.31);
            double phase = time * (.065 + (seed % 7) * .014) + seed * 1.71;
            for (int plane = 0; plane < 2; plane++) for (int row = glow ? 5 : 0; row < 12; row++) {
                float a = row / 12F, b = (row + 1) / 12F;
                mantleVertex(out, matrix, x, y, z, height, angle, phase, seed, plane, a, -1, strength);
                mantleVertex(out, matrix, x, y, z, height, angle, phase, seed, plane, a, 1, strength);
                mantleVertex(out, matrix, x, y, z, height, angle, phase, seed, plane, b, 1, strength);
                mantleVertex(out, matrix, x, y, z, height, angle, phase, seed, plane, b, -1, strength);
            }
        }
    }

    private static void mantleVertex(VertexConsumer out, Matrix4f m, float x, float y, float z,
                                     float height, double angle, double phase, int seed, int plane,
                                     float t, int edge, float strength) {
        // Broad overlapping roots form the fur-like base. Traveling curls roll
        // along the horseshoe and rearward, with independent speeds per tuft.
        float width = .29F * (1 - t) * (1 + .18F * (float) Math.sin(t * 8 - phase));
        float curl = .38F * t * (float) Math.sin(t * 6 - phase);
        float drift = .32F * t * t;
        float px = x - (float) Math.sin(angle) * curl + (plane == 0 ? edge * width : 0);
        float pz = z + (float) Math.cos(angle) * curl + drift + (plane == 1 ? edge * width : 0);
        float py = y + t * height + .12F * t * (float) Math.sin(t * 7 - phase);
        float body = Math.min(1, t / .45F);
        float tip = seed % 7 == 0 ? Math.max(0, (t - .65F) / .35F) : 0;
        emit(out, m, px, py, pz, .30F + .70F * body,
                .018F + .30F * body + .58F * tip, .012F + .025F * body + .35F * tip,
                strength, edge < 0 ? .02F : .04F, .26F + t * .02F);
    }

    private static void pillar(MultiBufferSource buffers, Matrix4f matrix, double phase, double time) {
        float fade = (float) (Math.min(1, phase / 8) * Math.max(0, Math.min(1, (58 - phase) / 16)));
        if (fade <= 0) return;
        ResourceLocation texture = SWIRLS[(int)(Math.floor(time/2) % SWIRLS.length)];
        VertexConsumer out = buffers.getBuffer(CrimsonFireRenderType.fire(texture));
        float height = (float) Math.min(9, 1 + phase * .35);
        // Two continuous helical sheets rise from the ground, then tighten around the forming body.
        for (int strand = 0; strand < 2; strand++) {
            for (int row = 0; row < 48; row++) {
                float a = row / 48F, b = (row + 1) / 48F;
                // Split texture repeats at quad boundaries: the last edge ends at
                // v=1, then the next quad starts at v=0 without reversing a strip.
                float v0 = (row % 16) / 16F, v1 = ((row % 16) + 1) / 16F;
                spiralVertex(out, matrix, a, v0, -1, strand, height, phase, time, fade);
                spiralVertex(out, matrix, a, v0, 1, strand, height, phase, time, fade);
                spiralVertex(out, matrix, b, v1, 1, strand, height, phase, time, fade);
                spiralVertex(out, matrix, b, v1, -1, strand, height, phase, time, fade);
            }
        }
    }

    private static void spiralVertex(VertexConsumer out, Matrix4f matrix, float t, float v, int edge, int strand,
                                     float height, double phase, double time, float fade) {
        double angle = time * .16 + t * Math.PI * 4 + strand * Math.PI + edge * .32;
        double radius = 1.6 + t * .8 - Math.max(0, phase - 32) * .025;
        emit(out, matrix, (float) (Math.cos(angle) * radius), t * height,
                (float) (Math.sin(angle) * radius), 1, .62F, .24F, fade * .8F,
                edge < 0 ? 0 : 1, v);
    }

    private static void quad(VertexConsumer out, Matrix4f m, float left, float bottom, float right,
                             float top, float z, float alpha, float heat) {
        emit(out,m,left,bottom,z,heat,.28F*heat,.02F,alpha,.02F,.26F);
        emit(out,m,right,bottom,z,heat,.28F*heat,.02F,alpha,.04F,.26F);
        emit(out,m,right,top,z,.6F*heat,.08F*heat,.01F,alpha,.04F,.28F);
        emit(out,m,left,top,z,.6F*heat,.08F*heat,.01F,alpha,.02F,.28F);
    }

    private static void tongue(VertexConsumer out, Matrix4f m, float x, float y, float z,
                               float height, double time, int seed, float strength) {
        double phase = time * .14 + seed * 1.71;
        float length = height * (1 + .12F * (float) Math.sin(phase));
        for (int plane = 0; plane < 2; plane++) for (int row = 0; row < 12; row++) {
            float a = row / 12F, b = (row + 1) / 12F;
            vertex(out,m,x,y,z,length,phase,plane,a,-1,strength);
            vertex(out,m,x,y,z,length,phase,plane,a,1,strength);
            vertex(out,m,x,y,z,length,phase,plane,b,1,strength);
            vertex(out,m,x,y,z,length,phase,plane,b,-1,strength);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float z,
                               float height, double phase, int plane, float t, int edge, float strength) {
        float width = .16F * (1-t) * (1 + .16F * (float) Math.sin(t*9-phase));
        float curl = .18F * t*t * (float) Math.sin(t*5-phase);
        emit(out,m,x+(plane==0 ? edge*width : curl*.4F)+curl,y+t*height,
                z+(plane==1 ? edge*width : curl*.4F),1,.9F-.6F*t,.3F*(1-t),
                // Non-degenerate UVs let Oculus/Solas compute a finite surface tangent.
                strength,edge < 0 ? .02F : .04F,.26F + t*.02F);
    }

    private static void emit(VertexConsumer out, Matrix4f m, float x, float y, float z,
                             float r, float g, float b, float a, float u, float v) {
        out.vertex(m,x,y,z).color(r,g,b,a).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880).normal(0,1,0).endVertex();
    }
}


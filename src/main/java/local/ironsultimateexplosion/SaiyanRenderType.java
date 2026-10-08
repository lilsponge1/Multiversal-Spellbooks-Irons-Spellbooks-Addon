package local.ironsultimateexplosion;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native particle shading avoids Solas treating translucent energy as a reflective entity. */
final class SaiyanRenderType extends RenderType {
    private SaiyanRenderType() {
        super("saiyan_energy", DefaultVertexFormat.f_85813_, VertexFormat.Mode.QUADS,
                256, false, true, () -> {}, () -> {});
    }
    static RenderType energy(ResourceLocation texture) {
        return m_173215_("saiyan_energy", DefaultVertexFormat.f_85813_, VertexFormat.Mode.QUADS,
                256, false, true, CompositeState.m_110628_()
                        .m_173292_(new ShaderStateShard(GameRenderer::m_172829_))
                        .m_173290_(new TextureStateShard(texture, false, false))
                        .m_110685_(f_110139_).m_110661_(f_110110_).m_110671_(f_110152_)
                        // The sky is complete at our draw stage; soft sheets test depth but
                        // do not replace it with nearly transparent panel silhouettes.
                        .m_110663_(f_110113_).m_110687_(f_110115_).m_110691_(false));
    }
}

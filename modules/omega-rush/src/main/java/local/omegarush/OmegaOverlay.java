package local.omegarush;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
public final class OmegaOverlay extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE=new ResourceLocation(OmegaMod.ID,"textures/effect/white.png");
    private OmegaOverlay(PlayerRenderer parent) { super(parent); }
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for(String skin:e.getSkins()) { PlayerRenderer renderer=e.getSkin(skin); if(renderer!=null) renderer.m_115326_(new OmegaOverlay(renderer)); }
    }
    @Override public void m_6494_(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(!OmegaConfig.OVERLAY.get()||!OmegaClient.owns(player)||player.m_20145_()) return;
        int rgb=java.awt.Color.HSBtoRGB((age+partial)/30f%1,0.9f,1);
        m_117386_().m_7695_(pose,buffers.m_6299_(RenderType.m_110473_(TEXTURE)),15728880,OverlayTexture.f_118083_,((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,0.22f);
    }
}

package local.omegarush;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
public final class OmegaPresentation extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE=new ResourceLocation(OmegaMod.ID,"textures/effect/white.png");
    OmegaPresentation(PlayerRenderer renderer){super(renderer);}
    @Override public void m_6494_(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,float swing,float amount,float partial,float age,float yaw,float pitch){
        if(p.m_20145_())return;
        if(FloweryScarf.visible(p)){
            pose.m_85836_();m_117386_().f_102810_.m_104299_(pose);var v=buffers.m_6299_(RenderType.m_110452_(WHITE));
            // Collar and folded tails stay attached to the posed upper body.
            box(pose,v,-0.34f,-0.055f,-0.22f,0.34f,0.07f,-0.165f,1f,0.87f,0.42f,light);
            box(pose,v,-0.34f,-0.055f,0.15f,0.34f,0.07f,0.20f,0.73f,0.52f,0.24f,light);
            box(pose,v,-0.34f,-0.055f,-0.165f,-0.27f,0.07f,0.15f,0.91f,0.72f,0.34f,light);
            box(pose,v,0.27f,-0.055f,-0.165f,0.34f,0.07f,0.15f,0.91f,0.72f,0.34f,light);
            box(pose,v,-0.27f,0.03f,-0.245f,-0.13f,0.48f,-0.205f,1f,0.91f,0.52f,light);
            box(pose,v,-0.13f,0.06f,-0.23f,0.02f,0.34f,-0.19f,0.80f,0.61f,0.29f,light);
            pose.m_85849_();
        }
        if(OmegaFormClient.active(p))OmegaEchoes.capture(p,m_117386_(),pose);
    }
    private static void box(PoseStack pose,VertexConsumer out,float x0,float y0,float z0,float x1,float y1,float z1,float r,float g,float b,int light){
        float[][] vertices={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] sides={{1,0,3,2},{4,5,6,7},{0,4,7,3},{5,1,2,6},{0,1,5,4},{3,7,6,2}};
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){var p=vertices[sides[f][i]];var n=normals[f];out.m_252986_(pose.m_85850_().m_252922_(),p[0],p[1],p[2]).m_85950_(r,g,b,1).m_7421_(i==0||i==3?0:1,i<2?0:1).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_252939_(pose.m_85850_().m_252943_(),n[0],n[1],n[2]).m_5752_();}
    }
    public static void arm(PlayerRenderer renderer,PoseStack pose,MultiBufferSource buffers,AbstractClientPlayer p,boolean right){
        if(!OmegaConfig.OVERLAY.get()||!OmegaFormClient.active(p)||p.m_20145_())return;
        int rgb=java.awt.Color.HSBtoRGB(p.f_19797_/30f%1,0.9f,1);var arm=right?renderer.m_7200_().f_102811_:renderer.m_7200_().f_102812_;
        pose.m_85836_();pose.m_85841_(1.008f,1.008f,1.008f);arm.m_104306_(pose,buffers.m_6299_(RenderType.m_110473_(WHITE)),15728880,OverlayTexture.f_118083_,((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,0.24f);pose.m_85849_();
    }
}

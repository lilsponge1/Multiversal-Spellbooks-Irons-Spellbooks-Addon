package local.ironsultimateexplosion;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.*;

/** Sculpted, combed locks following the animated Curios head. */
public final class SaiyanWigRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(ModItems.SAIYAN_WIG.get(), SaiyanWigRenderer::new));
    }
    @Override public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot,
            PoseStack pose, RenderLayerParent<T, M> renderer, MultiBufferSource buffers, int light,
            float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (!(renderer.m_7200_() instanceof HumanoidModel<?> model)) return;
        var visual = SaiyanClient.STATES.get(slot.entity().m_20148_());
        int tier = visual == null ? 0 : visual.packet.form();
        if (visual != null && visual.packet.target() != 0 && visual.charge() > .65f) tier = visual.packet.target();
        if(tier==3) { renderThird(slot,pose,model,buffers,light,limbSwing,limbAmount,age,partialTick,visual); return; }
        pose.m_85836_();
        model.f_102808_.m_104299_(pose);
        pose.m_85841_(1f/16,1f/16,1f/16);
        if (!slot.entity().m_6844_(EquipmentSlot.HEAD).m_41619_()) {
            // Clear both the top and the sides of a standard armor helmet.
            pose.m_85837_(0, -.75, 0);
            pose.m_85841_(1.12f, 1, 1.12f);
        }
        VertexConsumer out = buffers.m_6299_(RenderType.m_110452_(TEXTURE));
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normal = pose.m_85850_().m_252943_();
        Vector3f transformed = new Vector3f();
        int brightness = tier == 0 ? light : 15728880;
        float r = tier == 0 ? .14f : 1, g = tier == 0 ? .15f : tier == 2 ? .80f : .69f, b = tier == 0 ? .18f : .035f;
        for (float[] face : SaiyanHairMesh.faces(tier)) {
            for (int i=0; i<face.length; i+=9) {
                transformed.set(face[i+3],face[i+4],face[i+5]).mul(normal).normalize();
                float shade = face[i+8];
                out.m_252986_(matrix,face[i],face[i+1],face[i+2]).m_85950_(r*shade,g*shade,b*shade,1)
                    .m_7421_(face[i+6],face[i+7]).m_86008_(OverlayTexture.f_118083_).m_85969_(brightness)
                    .m_5601_(transformed.x,transformed.y,transformed.z).m_5752_();
            }
        }
        pose.m_85849_();
    }
    private static void renderThird(SlotContext slot,PoseStack pose,HumanoidModel<?> model,MultiBufferSource buffers,int light,float limbSwing,float limbAmount,float age,float partialTick,SaiyanClient.Visual visual) {
        boolean helmet=!slot.entity().m_6844_(EquipmentSlot.HEAD).m_41619_();
        float reveal=visual.packet.form()==3?1:Math.max(0,Math.min(1,(visual.charge()-.65f)/.28f));
        pose.m_85836_(); model.f_102808_.m_104299_(pose); pose.m_85841_(1f/16,1f/16,1f/16);
        Matrix4f bareHeadPose=new Matrix4f(pose.m_85850_().m_252922_());
        if(helmet) { pose.m_85837_(0,-.75,0); pose.m_85841_(1.12f,1,1.12f); }
        Matrix4f headPose=new Matrix4f(pose.m_85850_().m_252922_());
        drawThird(pose,buffers,SaiyanHairMesh.thirdHead(),0,0,0,1,null);
        if(slot.entity() instanceof net.minecraft.client.player.AbstractClientPlayer player && !helmet) drawBrow(pose,buffers,player);
        pose.m_85849_();
        // Roots follow the crown, gradually blending into a torso-following mane.
        // This keeps one connected hairstyle while the long ends stay behind the body.
        pose.m_85836_(); model.f_102810_.m_104299_(pose); pose.m_85841_(1f/16,1f/16,1f/16);
        if(helmet) { pose.m_85837_(0,-.75,0); pose.m_85841_(1.12f,1,1.12f); }
        Matrix4f inverseBody=new Matrix4f(pose.m_85850_().m_252922_()).invert();
        SaiyanHairPose hairPose=new SaiyanHairPose(new Matrix4f(inverseBody).mul(headPose),
            new Matrix4f(inverseBody).mul(bareHeadPose),helmet);
        float motion=SaiyanConfig.SSJ3_SWAY.get().floatValue();
        float walk=Math.min(1,Math.abs(limbAmount));
        int strand=0;
        for(var tail:SaiyanHairMesh.thirdTails()) {
            hairPose.begin(tail);
            float phase=age*.12f+strand++*.77f;
            float sway=(float)(Math.sin(phase)*.7+Math.sin(limbSwing*.8+phase*.3)*walk*1.9)*motion;
            float ripple=(float)Math.sin(phase*.7)*.5f*motion;
            drawThird(pose,buffers,tail,sway,ripple,walk*1.1f*motion,reveal,hairPose);
        }
        pose.m_85849_();
    }
    private static void drawThird(PoseStack pose,MultiBufferSource buffers,float[][] faces,float sway,float ripple,float lift,float reveal,SaiyanHairPose hairPose) {
        var out=buffers.m_6299_(RenderType.m_110452_(TEXTURE));
        var matrix=pose.m_85850_().m_252922_(); var normal=pose.m_85850_().m_252943_(); var n=new Vector3f();
        for(var face:faces) { if(hairPose!=null)hairPose.face(face,sway,ripple,lift,reveal);
        for(int i=0;i<face.length;i+=9) {
            float x=face[i],y=face[i+1],z=face[i+2],shade=face[i+8];
            if(hairPose!=null) {
                var point=hairPose.point(i/9);x=point.x;y=point.y;z=point.z;
                n.set(hairPose.normal(i/9));
            } else {
                n.set(face[i+3],face[i+4],face[i+5]);
            }
            n.mul(normal).normalize();
            out.m_252986_(matrix,x,y,z)
                .m_85950_(shade,.87f*shade,.07f*shade,1).m_7421_(face[i+6],face[i+7]).m_86008_(OverlayTexture.f_118083_)
                .m_85969_(15728880).m_5601_(n.x,n.y,n.z).m_5752_();
        }
        }
    }
    /** Temporary skin-coloured brow ridges; sample the forehead, never edit the player's skin. */
    private static void drawBrow(PoseStack pose,MultiBufferSource buffers,net.minecraft.client.player.AbstractClientPlayer player) {
        var out=buffers.m_6299_(RenderType.m_110452_(player.m_108560_()));
        var matrix=pose.m_85850_().m_252922_(); var normal=pose.m_85850_().m_252943_();
        var n=new Vector3f(0,0,-1).mul(normal).normalize();
        for(int side:new int[]{-1,1}) {
            // Sloping strips stay above the eye row. The rest of the original face remains visible.
            float inner=side*.6f,outer=side*3.55f;
            float[][] points={{inner,-5.05f},{outer,-5.8f},{outer,-6.5f},{inner,-5.65f}};
            for(int vertex=0;vertex<4;vertex++) {
                int index=side<0?vertex:3-vertex;
                out.m_252986_(matrix,points[index][0],points[index][1],-4.58f).m_85950_(1,1,1,1)
                    .m_7421_(12.5f/64,9.5f/64).m_86008_(OverlayTexture.f_118083_).m_85969_(15728880)
                    .m_5601_(n.x,n.y,n.z).m_5752_();
            }
        }
    }
}

package local.omegarush;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.*;
/** Bounded snapshots of the already posed mesh, without recursively rendering players. */
public final class OmegaEchoes {
    private record Vertex(double x,double y,double z,float u,float v,float nx,float ny,float nz){}
    private record Echo(UUID owner,long tick,Vec3 center,ResourceLocation skin,List<Vertex> mesh){}
    private static final Map<UUID,ArrayDeque<Echo>> HISTORY=new HashMap<>();
    private static Matrix4f inverseView=new Matrix4f();private static Vec3 camera=Vec3.f_82478_;private static boolean capturing;
    static void capture(AbstractClientPlayer p,PlayerModel<AbstractClientPlayer> model,PoseStack pose){
        var mc=Minecraft.m_91087_();if(!capturing||!OmegaConfig.AFTERIMAGES.get()||OmegaConfig.QUALITY.get()==OmegaConfig.Quality.MINIMAL||mc.f_91073_==null||mc.f_91074_==null)return;
        Vec3 center=p.m_20182_();if(center.m_82554_(camera)>48)return;
        int competitors=0;for(var f:OmegaFormClient.FORMS.values())if(f.packet.phase()==1&&f.entity!=null&&f.entity.m_20182_().m_82554_(camera)<center.m_82554_(camera))competitors++;
        if(competitors>=4)return;var history=HISTORY.computeIfAbsent(p.m_20148_(),k->new ArrayDeque<>());long now=mc.f_91073_.m_46467_();
        int interval=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?3:5;
        if(!history.isEmpty()&&(now-history.peekLast().tick<interval||history.peekLast().center.m_82554_(center)<0.15))return;
        Recorder recorder=new Recorder(inverseView,camera);model.m_7695_(pose,recorder,15728880,OverlayTexture.f_118083_,1,1,1,1);
        if(recorder.vertices.isEmpty())return;history.addLast(new Echo(p.m_20148_(),now,center,p.m_108560_(),List.copyOf(recorder.vertices)));
        int max=OmegaConfig.QUALITY.get()==OmegaConfig.Quality.FULL?5:2;while(history.size()>max)history.removeFirst();
    }
    @SubscribeEvent public static void stage(RenderLevelStageEvent e){
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS){inverseView=new Matrix4f(e.getPoseStack().m_85850_().m_252922_()).invert();camera=e.getCamera().m_90583_();capturing=true;return;}
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;capturing=false;
        var mc=Minecraft.m_91087_();if(mc.f_91073_==null||!OmegaConfig.AFTERIMAGES.get()||OmegaConfig.QUALITY.get()==OmegaConfig.Quality.MINIMAL){HISTORY.clear();return;}
        long now=mc.f_91073_.m_46467_();List<Echo> render=new ArrayList<>();
        HISTORY.entrySet().removeIf(entry->{var f=OmegaFormClient.FORMS.get(entry.getKey());if(f==null||f.packet.phase()!=1||f.entity==null||f.entity.m_20145_())return true;
            entry.getValue().removeIf(frame->now<frame.tick||now-frame.tick>=15);render.addAll(entry.getValue());return entry.getValue().isEmpty();});
        render.sort(Comparator.comparingDouble((Echo frame)->frame.center.m_82554_(e.getCamera().m_90583_())).reversed());
        var buffers=mc.m_91269_().m_110104_();Set<RenderType> used=new HashSet<>();Matrix4f view=e.getPoseStack().m_85850_().m_252922_();Vec3 eye=e.getCamera().m_90583_();
        for(Echo frame:render){var owner=OmegaFormClient.FORMS.get(frame.owner);if(owner==null||owner.entity==null||frame.center.m_82554_(owner.entity.m_20182_())<0.12||frame.center.m_82554_(eye)>48)continue;float alpha=(float)(0.30*(1-(now+e.getPartialTick()-frame.tick)/15.0));if(alpha<=0)continue;
            int rgb=java.awt.Color.HSBtoRGB(frame.tick/30f%1,0.75f,1);RenderType type=RenderType.m_110473_(frame.skin);used.add(type);var out=buffers.m_6299_(type);
            for(Vertex v:frame.mesh)out.m_252986_(view,(float)(v.x-eye.f_82479_),(float)(v.y-eye.f_82480_),(float)(v.z-eye.f_82481_)).m_85950_(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,alpha).m_7421_(v.u,v.v).m_86008_(OverlayTexture.f_118083_).m_85969_(15728880).m_252939_(e.getPoseStack().m_85850_().m_252943_(),v.nx,v.ny,v.nz).m_5752_();
        }
        for(RenderType type:used)buffers.m_109912_(type);
    }
    private static final class Recorder implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();final Matrix4f inverse;final Matrix3f normal;final Vec3 camera;
        double x,y,z;float u,v,nx,ny,nz;
        Recorder(Matrix4f m,Vec3 c){inverse=new Matrix4f(m);normal=new Matrix3f(m);camera=c;}
        public VertexConsumer m_5483_(double a,double b,double c){Vector3f p=inverse.transformPosition(new Vector3f((float)a,(float)b,(float)c));x=p.x+camera.f_82479_;y=p.y+camera.f_82480_;z=p.z+camera.f_82481_;return this;}
        public VertexConsumer m_6122_(int r,int g,int b,int a){return this;}
        public VertexConsumer m_7421_(float a,float b){u=a;v=b;return this;}
        public VertexConsumer m_7122_(int a,int b){return this;}
        public VertexConsumer m_7120_(int a,int b){return this;}
        public VertexConsumer m_5601_(float a,float b,float c){Vector3f n=normal.transform(new Vector3f(a,b,c));nx=n.x;ny=n.y;nz=n.z;return this;}
        public void m_5752_(){if(vertices.size()<1024)vertices.add(new Vertex(x,y,z,u,v,nx,ny,nz));}
        public void m_7404_(int r,int g,int b,int a){}
        public void m_141991_(){}
    }
    static void clear(UUID id){HISTORY.remove(id);}
    static void clear(){HISTORY.clear();capturing=false;}
    private OmegaEchoes(){}
}

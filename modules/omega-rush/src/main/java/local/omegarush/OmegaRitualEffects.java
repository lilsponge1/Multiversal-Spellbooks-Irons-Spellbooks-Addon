package local.omegarush;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;

/** Native flower item models, rendered without entities or gameplay loot. */
public final class OmegaRitualEffects {
    public static final List<String> FLOWER_IDS=List.of("poppy","orange_tulip","dandelion","blue_orchid","cornflower","allium");
    private static List<ItemStack> flowers;
    private static final LinkedHashSet<String> SEEN=new LinkedHashSet<>();
    private static long flashTick=-100;
    private record Finish(OmegaRitualEventPacket packet,long tick){}
    private static final ArrayDeque<Finish> FINISHES=new ArrayDeque<>();
    private static List<ItemStack> flowers(){
        if(flowers==null){flowers=new ArrayList<>();for(String name:FLOWER_IDS)flowers.add(new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft",name)))));}
        return flowers;
    }
    static double age(OmegaFormClient.Form f,float partial){return Math.min(f.packet.chargeTicks(),f.packet.age()+Math.min(3,(System.nanoTime()-f.received)/50_000_000.0)+partial);}
    static void event(OmegaRitualEventPacket p){
        var mc=Minecraft.m_91087_();var l=mc.f_91073_;if(l==null||mc.f_91074_==null||!p.dimension().equals(OmegaManager.dimension(mc.f_91074_))||p.index()<0||p.index()>6||!OmegaMovement.finite(p.position())||p.position().m_82554_(mc.f_91074_.m_20182_())>50)return;
        String key=p.caster()+":"+p.session()+":"+p.index();if(!SEEN.add(key))return;while(SEEN.size()>512)SEEN.remove(SEEN.iterator().next());
        if(p.index()<6)l.m_7785_(p.position().f_82479_,p.position().f_82480_,p.position().f_82481_,OmegaSounds.FLOWER.get(),SoundSource.PLAYERS,OmegaConfig.VOLUME.get().floatValue(),OmegaFormRitual.pitch(p.index()),false);
        else {flashTick=l.m_46467_();OmegaVisuals.ritualFlash(l,p);FINISHES.addLast(new Finish(p,flashTick));while(FINISHES.size()>64)FINISHES.removeFirst();}
    }
    static void tick(){
        var mc=Minecraft.m_91087_();var l=mc.f_91073_;if(l==null){clear();return;}
        long now=l.m_46467_();FINISHES.removeIf(f->now<f.tick||now-f.tick>=20);
        for(Finish f:FINISHES){long age=now-f.tick;var owner=OmegaFormClient.FORMS.get(f.packet.caster());Vec3 at=owner!=null&&owner.entity!=null?owner.entity.m_20191_().m_82399_():f.packet.position();
            if(age==4||age==8||age==12)OmegaVisuals.finishPulse(l,f.packet,at,(int)(age/4));
            if(age<16)OmegaVisuals.finishHalo(l,f.packet,at,(int)age);
        }
    }
    @SubscribeEvent public static void stage(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.m_91087_();if(mc.f_91073_==null)return;Vec3 camera=e.getCamera().m_90583_();
        var buffers=mc.m_91269_().m_110104_();boolean drawn=false;
        for(var f:OmegaFormClient.FORMS.values()){
            if(f.packet.phase()!=0||f.entity==null||f.entity.m_20145_()||f.entity.m_20182_().m_82554_(camera)>48)continue;
            double age=age(f,e.getPartialTick());Vec3 center=f.entity.m_20191_().m_82399_();int count=OmegaFormRitual.count(age,f.packet.chargeTicks());
            for(int i=0;i<count;i++){
                double scale=OmegaFormRitual.scale(i,age,f.packet.chargeTicks());if(scale<0.01)continue;
                Vec3 p=center.m_82549_(OmegaFormRitual.offset(i,age,f.packet.chargeTicks(),f.entity.m_146908_()));PoseStack pose=e.getPoseStack();
                pose.m_85836_();pose.m_85837_(p.f_82479_-camera.f_82479_,p.f_82480_-camera.f_82480_,p.f_82481_-camera.f_82481_);
                pose.m_252781_(e.getCamera().m_253121_());pose.m_252781_(new Quaternionf().rotateY((float)Math.PI));
                float size=(float)(1.5*scale);pose.m_85841_(size,size,size);
                mc.m_91291_().m_269128_(flowers().get(i),ItemDisplayContext.FIXED,15728880,OverlayTexture.f_118083_,pose,buffers,mc.f_91073_,i);
                pose.m_85849_();drawn=true;
            }
        }
        if(drawn)buffers.m_109911_();
    }
    @SubscribeEvent public static void flash(RenderGuiEvent.Post e){
        var mc=Minecraft.m_91087_();if(mc.f_91073_==null||!OmegaConfig.FLASH.get())return;
        double age=mc.f_91073_.m_46467_()-flashTick+e.getPartialTick();if(age<0||age>=12)return;
        int alpha=(int)(185*Math.pow(1-age/12,2));
        e.getGuiGraphics().m_285944_(RenderType.m_286086_(),0,0,mc.m_91268_().m_85445_(),mc.m_91268_().m_85446_(),(alpha<<24)|0xffffff);
        e.getGuiGraphics().m_280262_();
    }
    static void clear(){SEEN.clear();FINISHES.clear();flashTick=-100;flowers=null;}
    private OmegaRitualEffects(){}
}

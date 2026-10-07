package local.omegarushtest;
import java.util.*;
import local.omegarush.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.RenderGuiEvent;

/** Evidence only in the disposable test mod; never packaged in the release. */
public final class OmegaRitualClientHarness {
    private static final Set<String> CLOCKS=new HashSet<>();private static long seenFlash=-1;
    public static void register(){MinecraftForge.EVENT_BUS.register(OmegaRitualClientHarness.class);}
    private static Object field(String type,String name)throws Exception{var f=Class.forName(type).getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    private static Object field(Object value,String name)throws Exception{var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    @SubscribeEvent public static void sound(PlaySoundSourceEvent e){
        var sound=e.getSound();if(sound!=null&&sound.m_7904_().toString().equals("irons_omega_rush:flower_appear"))System.out.println("OMEGA_RITUAL_AUDIO_SOURCE pitch="+sound.m_7783_()+" volume="+sound.m_7769_());
    }
    @SubscribeEvent public static void streaming(PlayStreamingSourceEvent e){var sound=e.getSound();if(sound!=null&&sound.m_7904_().toString().equals("irons_omega_rush:car_drive"))System.out.println("OMEGA_RITUAL_CAR_SOURCE looping="+sound.m_7775_());}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.m_91087_();if(mc.f_91073_==null||mc.f_91074_==null){CLOCKS.clear();return;}
        try{for(Object value:((Map<?,?>)field("local.omegarush.OmegaFormClient","FORMS")).values()){
            var p=(OmegaFormPacket)field(value,"packet");if(p.phase()!=0)continue;
            int count=OmegaFormRitual.count(p.age(),p.chargeTicks());String key=p.caster()+":"+p.session()+":"+count;
            if(CLOCKS.add(key))System.out.println("OMEGA_RITUAL_CLIENT caster="+p.caster()+" local="+p.caster().equals(mc.f_91074_.m_20148_())+" age="+p.age()+" flowers="+count+" lift="+(p.position().f_82480_-p.chargeOrigin().f_82480_));
        }}catch(Exception ex){System.out.println("OMEGA_RITUAL_CLIENT_TRACE_ERROR "+ex);}
    }
    @SubscribeEvent public static void gui(RenderGuiEvent.Post e){
        var mc=Minecraft.m_91087_();if(mc.f_91073_==null)return;
        try{long flash=(long)field("local.omegarush.OmegaRitualEffects","flashTick");if(flash>=0&&flash!=seenFlash&&mc.f_91073_.m_46467_()-flash<12){seenFlash=flash;System.out.println("OMEGA_RITUAL_GUI_FLASH tick="+flash+" bright_enabled="+OmegaConfig.FLASH.get());}}catch(Exception ex){System.out.println("OMEGA_RITUAL_CLIENT_TRACE_ERROR "+ex);}
    }
}

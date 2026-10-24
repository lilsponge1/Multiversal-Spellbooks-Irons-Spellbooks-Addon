package local.kischooltest;

import java.io.File;
import java.util.Map;
import local.kischool.*;
import net.minecraft.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;

final class KiClientCheck {
    private int age, passed, failed;
    private boolean done;
    private String capture;
    KiClientCheck() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::render);
    }
    private void check(boolean good,String label) {
        if(good)passed++;else failed++;
        System.out.println((good?"KI_CLIENT_PASS ":"KI_CLIENT_FAIL ")+label);
    }
    private int form() throws Exception {
        var field = SaiyanClient.class.getDeclaredField("STATES");field.setAccessible(true);
        Object visual = ((Map<?,?>)field.get(null)).get(Minecraft.m_91087_().f_91074_.m_20148_());
        if(visual==null)return 0;
        var packet = visual.getClass().getDeclaredField("packet");packet.setAccessible(true);
        return ((SaiyanStatePacket)packet.get(visual)).form();
    }
    private void input(byte action) { KiNetwork.saiyanInput(new SaiyanInputPacket(action,-1)); }
    private void ascend() { input(SaiyanInputPacket.PRESS);input(SaiyanInputPacket.RELEASE); }
    private void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.m_91087_();
        if(mc.f_91073_==null||mc.f_91074_==null||!mc.f_91074_.m_21205_().m_150930_(ModItems.SAIYAN_BOOK.get()))return;
        try {
            age++;
            if(age==20) {
                var glfw=Class.forName("org.lwjgl.glfw.GLFW");
                int visible=(Integer)glfw.getMethod("glfwGetWindowAttrib",long.class,int.class).invoke(null,mc.m_91268_().m_85439_(),0x20004);
                check(visible==0,"native client stays hidden");
                if(visible!=0)throw new IllegalStateException("Unexpected visible test window");
                check(net.minecraftforge.fml.ModList.get().isLoaded("ki_school"),"Ki loaded independently");
                check(net.minecraftforge.fml.ModList.get().isLoaded("irons_omega_rush"),"existing Omega loaded alongside Ki");
                check(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("irons_omega_rush:flowery_scarf")),"Omega scarf still registered");
                check(ModItems.SAIYAN_WIG.getId().toString().equals("ki_school:saiyan_wig"),"wig uses independent item namespace");
                check(java.util.Arrays.stream(io.redspace.ironsspellbooks.player.ClientInputEvents.class.getDeclaredMethods()).anyMatch(m->m.getName().contains("saiyan$consume")),"Ki input mixin applied");
                check(dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry.getAnimation(new ResourceLocation("ki_school:saiyan_charge"))!=null,"charge animation parsed");
                check(top.theillusivec4.curios.api.client.CuriosRendererRegistry.getRenderer(ModItems.SAIYAN_WIG.get()).isPresent(),"Curios wig renderer registered");
                for(String path:new String[]{"textures/gui/spell_icons/saiyan_ascension.png","textures/entity/saiyan_energy.png","models/item/saiyan_wig.json","textures/item/book_of_saiyan_ascension.png"})
                    check(mc.m_91098_().m_213713_(new ResourceLocation("ki_school",path)).isPresent(),"resource "+path);
                for(String name:new String[]{"saiyan_charge","saiyan_charge_2","saiyan_crack","saiyan_burst","saiyan_down","saiyan_yell","saiyan_yell_2","saiyan_charge_3","saiyan_yell_3","saiyan_music_3"})
                    check(mc.m_91106_().m_120384_(new ResourceLocation("ki_school",name))!=null,"sound "+name);
                check(mc.m_91291_().m_174264_(ModItems.SAIYAN_WIG.get().m_7968_(),mc.f_91073_,mc.f_91074_,0)!=mc.m_91304_().m_119409_(),"wig inventory model is not missing");
                mc.f_91066_.m_92157_(CameraType.THIRD_PERSON_FRONT);
                mc.f_91066_.m_231837_().m_231514_(40);
                mc.f_91074_.m_146922_(0);mc.f_91074_.m_146926_(0);
            }
            if(age==40)ascend();
            if(age==110)check(form()==1,"SSJ1 synchronized over independent channel");
            if(age==130)capture="ki-ssj1.png";
            if(age==140)ascend();
            if(age==220)check(form()==2,"SSJ2 synchronized over independent channel");
            if(age==240)capture="ki-ssj2.png";
            if(age==250)ascend();
            if(age==340)capture="ki-ssj3-charge.png";
            if(age==570)check(form()==3,"SSJ3 completes fifteen-second charge alongside Omega");
            if(age==600)capture="ki-ssj3.png";
            if(age==620)input(SaiyanInputPacket.PRESS);
            if(age>620&&age<=685&&age%5==0)input(SaiyanInputPacket.KEEP_HELD);
            if(age==690)input(SaiyanInputPacket.RELEASE);
            if(age==710) {
                check(form()==0,"hold returns to Base");
                done=true;
                System.out.println("KI_CLIENT_DIAGNOSTICS "+passed+" passed, "+failed+" failed");
                mc.execute(mc::m_91395_);
            }
        } catch(Throwable error) {
            failed++;done=true;error.printStackTrace(System.out);
            System.out.println("KI_CLIENT_DIAGNOSTICS "+passed+" passed, "+failed+" failed");
            mc.execute(mc::m_91395_);
        }
    }
    private void render(RenderGuiEvent.Post event) {
        if(capture==null)return;
        String name=capture;capture=null;var mc=Minecraft.m_91087_();
        Screenshot.m_92295_(new File(System.getProperty("user.dir")),name,mc.m_91385_(),message->System.out.println("KI_CAPTURE "+name));
    }
}

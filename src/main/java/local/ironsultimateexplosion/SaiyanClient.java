package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.*;
import net.minecraft.client.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;

public final class SaiyanClient {
    static final Map<UUID, Visual> STATES = new HashMap<>();
    static final class Visual {
        SaiyanStatePacket packet;
        Entity entity;
        int since, visualAge, fade = -1, burst = -1, drop = -1;
        long received = System.nanoTime();
        Vec3 last;
        SaiyanChargeSound sound;
        SaiyanChargeSound yell;
        SaiyanSequenceSound voice, music;
        int thirdVoiceAge;
        Visual(SaiyanStatePacket packet) { this.packet = packet; }
        float charge() { return packet.target() == 0 ? 0 : Math.min(1, (packet.chargeAge() + since) / (float)Math.max(1, packet.chargeDuration())); }
        float calm() { return packet.heldTicks() < 0 ? 1 : 1 - .45f * Math.min(1, (packet.heldTicks() + since) / (float)packet.holdDuration()); }
        int tier() { return Math.max(packet.form(), packet.target()); }
    }
    private static KeyMapping heldKey;
    private static int heldSlot, inputAge;
    private static boolean latched;
    private static int shakeTicks, shakeDuration;
    private static double shakeStrength;
    static void register() { MinecraftForge.EVENT_BUS.register(SaiyanClient.class); MinecraftForge.EVENT_BUS.register(SaiyanVisuals.class); }
    public static boolean keyConsumed(ExtendedKeyMapping key) {
        Minecraft mc = Minecraft.m_91087_(); if (mc.f_91074_ == null || mc.f_91080_ != null) return false;
        SpellSelectionManager manager = ClientMagicData.getSpellSelectionManager(); if (manager == null) return false;
        var selection = key == KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP ? manager.getSelection() : null;
        int quick = KeyMappings.QUICK_CAST_MAPPINGS.indexOf(key);
        if (quick >= 0) selection = manager.getSpellSlot(quick);
        if (selection == null || selection.spellData.getSpell() != ModSpells.SAIYAN_ASCENSION.get()) return false;
        press(key, selection.globalIndex); return true;
    }
    private static void press(KeyMapping key, int slot) {
        if (heldKey != null || latched) return;
        heldKey = key; heldSlot = slot; inputAge = 0; latched = true;
        ExplosionNetwork.saiyanInput(new SaiyanInputPacket(SaiyanInputPacket.PRESS, slot));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void use(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.m_91087_();
        if (!e.isUseItem() || mc.f_91074_ == null || mc.f_91080_ != null) return;
        boolean book = mc.f_91074_.m_21205_().m_150930_(ModItems.SAIYAN_BOOK.get()) || mc.f_91074_.m_21206_().m_150930_(ModItems.SAIYAN_BOOK.get());
        int slot = -1;
        var m = ClientMagicData.getSpellSelectionManager(); var s = m == null ? null : m.getSelection();
        if (!book && s != null && s.spellData.getSpell() == ModSpells.SAIYAN_ASCENSION.get()
                && (SpellSelectionManager.MAINHAND.equals(s.slot) || SpellSelectionManager.OFFHAND.equals(s.slot))) slot = s.globalIndex;
        else if (!book) return;
        e.setCanceled(true); e.setSwingHand(false); press(mc.f_91066_.f_92095_, slot);
    }
    public static void state(SaiyanStatePacket p) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || !p.dimension().equals(mc.f_91073_.m_46472_().m_135782_().toString())
                || p.form() < 0 || p.form() > 3 || p.target() < 0 || p.target() > 3 || !Float.isFinite(p.jumpBonus()) || p.jumpBonus()<0 || p.jumpBonus()>1) return;
        Visual v = STATES.get(p.player());
        if (v != null && (p.revision() < v.packet.revision() || p.revision() == v.packet.revision() && p.tick() < v.packet.tick())) return;
        if (p.cue() == SaiyanStatePacket.RESET) { remove(p.player()); return; }
        long oldRevision = v == null ? -1 : v.packet.revision();
        if (v == null) { if (STATES.size() >= 64) return; v = new Visual(p); STATES.put(p.player(), v); }
        v.packet = p; v.since = 0; v.received = System.nanoTime();
        Entity entity = mc.f_91073_.m_6815_(p.entity());
        if (entity != null && entity.m_20148_().equals(p.player())) v.entity = entity;
        if (p.cue() != SaiyanStatePacket.NONE && p.revision() != oldRevision) {
            if (p.cue() == SaiyanStatePacket.BURST) {
                stopSound(v); SaiyanAnimations.stop(p.player()); v.burst = 0;
                if(p.form()==3) {
                    shake(v,2.1,24);
                    if(v.entity!=null && SaiyanConfig.SSJ3_MUSIC.get()) {
                        v.music=new SaiyanSequenceSound(v.entity,ModSounds.SAIYAN_MUSIC_3.get(),p.volume(),200,false);
                        mc.m_91106_().m_120367_(v.music);
                    }
                }
                shake(v, p.form() == 2 ? 1.45 : .55, p.form() == 2 ? 16 : 10);
                sound(v, ModSounds.SAIYAN_BURST.get(), p.form() == 2 ? .65f : .9f, p.form() == 2 ? 1.75f : 1.1f);
                if (p.form() == 2) sound(v, ModSounds.SAIYAN_CRACK.get(), .85f, 1.4f);
            }
            if (p.cue() == SaiyanStatePacket.DOWN) { stopSound(v); stopMusic(v); SaiyanAnimations.stop(p.player()); v.fade = 0; sound(v, ModSounds.SAIYAN_DOWN.get(), 1f); }
            if (p.cue() == SaiyanStatePacket.DROP) { v.drop = 0; sound(v, ModSounds.SAIYAN_DOWN.get(), .8f); }
        }
        if (p.form() == 0 && p.target() == 0 && v.fade < 0) remove(p.player());
    }
    private static void sound(Visual v, net.minecraft.sounds.SoundEvent sound, float pitch) {
        sound(v, sound, pitch, 1);
    }
    private static void sound(Visual v, net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (v.entity == null) return;
        Vec3 p = v.entity.m_20182_(); Minecraft.m_91087_().f_91073_.m_7785_(p.f_82479_, p.f_82480_, p.f_82481_, sound, SoundSource.PLAYERS, v.packet.volume() * volume, pitch, false);
    }
    private static void shake(Visual v, double amount, int duration) {
        Minecraft mc = Minecraft.m_91087_(); if (!SaiyanConfig.SHAKE.get() || mc.f_91074_ == null || v.entity == null || v.packet.shakeRadius() <= 0) return;
        double d = Math.sqrt(v.entity.m_20182_().m_82554_(mc.f_91074_.m_20182_()));
        double strength = amount * Math.max(0, 1 - d / v.packet.shakeRadius());
        if (strength > shakeStrength || shakeTicks <= 0) { shakeStrength = strength; shakeTicks = shakeDuration = duration; }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.m_91087_(); if (mc.f_91073_ == null || mc.f_91074_ == null) { clear(); return; }
        if (mc.m_91104_()) return;
        if (heldKey != null) {
            boolean cancel = mc.f_91080_ != null || !mc.f_91074_.m_6084_();
            if (cancel || !heldKey.m_90857_()) {
                ExplosionNetwork.saiyanInput(new SaiyanInputPacket(cancel ? SaiyanInputPacket.CANCEL : SaiyanInputPacket.RELEASE, heldSlot));
                heldKey = null; latched = false;
            } else if (++inputAge % 5 == 0) ExplosionNetwork.saiyanInput(new SaiyanInputPacket(SaiyanInputPacket.KEEP_HELD, heldSlot));
        }
        if (shakeTicks > 0) shakeTicks--; else shakeStrength = 0;
        SaiyanVisuals.beginTick();
        List<Visual> visuals = new ArrayList<>(STATES.values());
        visuals.sort(Comparator.comparingDouble(v -> v.entity == null ? Double.MAX_VALUE : v.entity.m_20182_().m_82554_(mc.f_91074_.m_20182_())));
        for (Visual v : visuals) {
            Entity entity = mc.f_91073_.m_6815_(v.packet.entity());
            long elapsed = System.nanoTime() - v.received;
            if (entity == null && elapsed < 750_000_000L) continue;
            if (elapsed > 3_000_000_000L || entity == null || !entity.m_20148_().equals(v.packet.player()) || !entity.m_6084_()) { remove(v.packet.player()); continue; }
            v.entity = entity; v.since++; v.visualAge++;
            if (v.fade >= 0 && ++v.fade >= 12) { remove(v.packet.player()); continue; }
            if (v.burst >= 0 && ++v.burst > (v.tier() == 3 ? 24 : v.tier() == 2 ? 18 : 12)) v.burst = -1;
            if (v.drop >= 0 && ++v.drop > 12) v.drop = -1;
            if (v.packet.target() != 0) {
                if (entity instanceof AbstractClientPlayer p) SaiyanAnimations.start(p);
                if(v.packet.target()==3) {
                    // Electrocute's energy loop contains an additional vocal layer.
                    // SSJ3 plays only the isolated Goku sequence during its charge.
                    if(v.sound!=null) { v.sound.end(); mc.m_91106_().m_120399_(v.sound); v.sound=null; }
                    if(v.yell!=null) { v.yell.end(); mc.m_91106_().m_120399_(v.yell); v.yell=null; }
                    if(v.voice==null) {
                        boolean beginning=entity==mc.f_91074_ || v.packet.cue()==SaiyanStatePacket.CHARGE || v.packet.chargeAge()+v.since<=5;
                        v.voice=new SaiyanSequenceSound(entity,beginning?ModSounds.SAIYAN_CHARGE_3.get():ModSounds.SAIYAN_YELL_3.get(),v.packet.volume(),beginning?300:1200,!beginning);
                        mc.m_91106_().m_120367_(v.voice); v.thirdVoiceAge=0;
                    }
                    if(++v.thirdVoiceAge==300 && v.packet.chargeDuration()>300) {
                        v.voice.end(); mc.m_91106_().m_120399_(v.voice);
                        v.voice=new SaiyanSequenceSound(entity,ModSounds.SAIYAN_YELL_3.get(),v.packet.volume(),1200,true); mc.m_91106_().m_120367_(v.voice);
                    }
                } else {
                    if(v.sound==null) { v.sound=new SaiyanChargeSound(entity,v.packet.volume(),v.packet.target()==2); mc.m_91106_().m_120367_(v.sound); }
                    v.sound.progress(v.charge());
                    if(v.yell==null) {
                        var cry=v.packet.target()==2?ModSounds.SAIYAN_YELL_2.get():ModSounds.SAIYAN_YELL.get();
                        v.yell=new SaiyanChargeSound(entity,v.packet.volume(),v.packet.target()==2,cry,true);
                        mc.m_91106_().m_120367_(v.yell);
                    }
                }
                if (v.yell != null) v.yell.progress(v.charge());
                if (v.visualAge % 6 == 0) shake(v, v.charge() * (v.packet.target() == 3 ? 1.25 : v.packet.target() == 2 ? .8 : .22), 7);
            } else { stopSound(v); SaiyanAnimations.stop(v.packet.player()); }
            SaiyanVisuals.particles(v);
        }
        Visual own = STATES.get(mc.f_91074_.m_20148_());
        if (own != null && SaiyanConfig.HUD.get() && own.visualAge % 20 == 1) {
            String key = own.packet.target() != 0 ? "ui.irons_ultimate_explosion.saiyan.charging" : own.packet.form() == 3 ? "ui.irons_ultimate_explosion.saiyan.ssj3" : own.packet.form() == 2 ? "ui.irons_ultimate_explosion.saiyan.ssj2" : "ui.irons_ultimate_explosion.saiyan.ssj1";
            mc.f_91074_.m_5661_(Component.m_237115_(key), true);
        }
    }
    @SubscribeEvent public static void fov(ComputeFovModifierEvent e) {
        Visual own = STATES.get(e.getPlayer().m_20148_());
        if (own != null && own.packet.target() != 0) e.setNewFovModifier(1);
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (shakeTicks <= 0 || !SaiyanConfig.SHAKE.get()) return;
        double phase = (Minecraft.m_91087_().f_91073_.m_46467_() + e.getPartialTick()) * 2.4;
        float a = (float)(shakeStrength * shakeTicks / Math.max(1, shakeDuration));
        e.setPitch(e.getPitch() + (float)Math.sin(phase) * a);
        e.setYaw(e.getYaw() + (float)Math.cos(phase * 1.3) * a * .65f);
        e.setRoll(e.getRoll() + (float)Math.sin(phase * .8) * a * .35f);
    }
    private static void stopSound(Visual v) {
        if (v.sound != null) { v.sound.end(); Minecraft.m_91087_().m_91106_().m_120399_(v.sound); v.sound = null; }
        if (v.yell != null) { v.yell.end(); Minecraft.m_91087_().m_91106_().m_120399_(v.yell); v.yell = null; }
        if(v.voice!=null) { v.voice.end(); Minecraft.m_91087_().m_91106_().m_120399_(v.voice); v.voice=null; }
    }
    private static void stopMusic(Visual v) { if(v.music!=null) { v.music.end(); Minecraft.m_91087_().m_91106_().m_120399_(v.music); v.music=null; } }
    private static void remove(UUID id) { Visual v = STATES.remove(id); if (v != null) { stopSound(v); stopMusic(v); } SaiyanAnimations.stop(id); }
    @SubscribeEvent public static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        var mc=Minecraft.m_91087_(); if(event.getEntity()!=mc.f_91074_) return;
        var own=STATES.get(event.getEntity().m_20148_());
        if(own!=null && own.packet.form()==3) { var m=event.getEntity().m_20184_(); event.getEntity().m_20334_(m.f_82479_,m.f_82480_*(1+own.packet.jumpBonus()),m.f_82481_); }
    }
    public static void clear() { for (UUID id : new ArrayList<>(STATES.keySet())) remove(id); heldKey = null; latched = false; shakeTicks = 0; shakeStrength = 0; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload e) { if (e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel) clear(); }
    public static void reload(RegisterClientReloadListenersEvent e) { e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> clear()); }
    private SaiyanClient() {}
}

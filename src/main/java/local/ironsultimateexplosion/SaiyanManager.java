package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.*;
import static local.ironsultimateexplosion.SaiyanRules.Form.*;

/** Ephemeral server sessions, never saved. Transient fixed-UUID modifiers are removed at every boundary. */
public final class SaiyanManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static long tick, revision;
    private static final UUID[] MODIFIERS = new UUID[10];
    static { for (int i = 0; i < MODIFIERS.length; i++) MODIFIERS[i] = UUID.nameUUIDFromBytes((GrandExplosionMod.ID + ":saiyan:" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static final class Session {
        final ServerPlayer player;
        final String dimension;
        SaiyanRules.Form form = BASE, target = BASE;
        int age, duration, drainTicks, slot, heldTicks;
        boolean held, consumed;
        long heartbeat, lastPacket = -1, revision;
        int packetCount;
        long lastCosmetic = -10;
        boolean grounded;
        double previousY;
        Session(ServerPlayer player) { this.player = player; dimension = dimension(player); revision = ++SaiyanManager.revision; grounded=player.m_20096_(); previousY=player.m_20186_(); }
    }
    public static SaiyanRules.Form form(ServerPlayer p) { Session s = SESSIONS.get(p.m_20148_()); return s == null ? BASE : s.form; }
    private static String dimension(ServerPlayer p) { return p.m_9236_().m_46472_().m_135782_().toString(); }
    public static boolean hasAccess(ServerPlayer p) {
        if (!ModSpells.SAIYAN_ASCENSION.get().isEnabled()) return false;
        if (heldBook(p)) return true;
        return new SpellSelectionManager(p).getAllSpells().stream().anyMatch(o -> isSpell(o.spellData));
    }
    private static boolean isSpell(SpellData data) { return data != null && data.getSpell() == ModSpells.SAIYAN_ASCENSION.get(); }
    private static boolean bookWithSpell(ItemStack stack) {
        return stack.m_150930_(ModItems.SAIYAN_BOOK.get()) && ISpellContainer.getOrCreate(stack).getIndexForSpell(ModSpells.SAIYAN_ASCENSION.get()) >= 0;
    }
    private static boolean heldBook(ServerPlayer p) { return bookWithSpell(p.m_21205_()) || bookWithSpell(p.m_21206_()); }
    private static boolean validSlot(ServerPlayer p, int slot) {
        if (slot == -1) return heldBook(p);
        var options = new SpellSelectionManager(p).getAllSpells();
        return slot >= 0 && slot < options.size() && isSpell(options.get(slot).spellData);
    }
    public static void nativeActivation(ServerPlayer p) {
        // Native repeated mouse-down packets must not reset a hold or charge twice.
        if (SESSIONS.containsKey(p.m_20148_())) return;
        var selection = new SpellSelectionManager(p).getSelection();
        if (selection != null && isSpell(selection.spellData)) input(p, new SaiyanInputPacket(SaiyanInputPacket.PRESS, selection.globalIndex));
    }
    public static void input(ServerPlayer p, SaiyanInputPacket packet) {
        if (packet.action() < 0 || packet.action() > SaiyanInputPacket.CANCEL || !p.m_6084_() || p.m_5833_()) return;
        Session s = SESSIONS.get(p.m_20148_());
        if (s == null) {
            if (packet.action() != SaiyanInputPacket.PRESS || !hasAccess(p) || !validSlot(p, packet.slot()) ||
                    MagicData.getPlayerMagicData(p).isCasting() || ThundercrashManager.owns(p)) return;
            s = new Session(p); SESSIONS.put(p.m_20148_(), s);
        }
        if (s.lastPacket != tick) { s.lastPacket = tick; s.packetCount = 0; }
        if (++s.packetCount > 6) return;
        if (packet.action() == SaiyanInputPacket.PRESS) {
            if (s.held || s.target != BASE && s.target != SUPER_SAIYAN_3 || !validSlot(p, packet.slot()) || !hasAccess(p)) return;
            s.slot = packet.slot(); s.held = true; s.consumed = false; s.heldTicks = 0; s.heartbeat = tick;
            if (s.form == BASE && !charge(s, SUPER_SAIYAN_1)) reset(p, SaiyanStatePacket.RESET);
            else send(s, SaiyanStatePacket.NONE);
        } else if (s.held && packet.slot() == s.slot) {
            if (packet.action() == SaiyanInputPacket.KEEP_HELD) { s.heartbeat = tick; return; }
            boolean release = packet.action() == SaiyanInputPacket.RELEASE;
            s.held = false;
            if (!release && s.target == SUPER_SAIYAN_3) { reset(p,SaiyanStatePacket.DOWN); return; }
            if (release && !s.consumed && s.target == BASE) {
                switch (SaiyanRules.release(s.form, s.heldTicks, SaiyanConfig.SHORT.get(), SaiyanConfig.HOLD.get())) {
                    case ASCEND -> charge(s, s.form == SUPER_SAIYAN_1 ? SUPER_SAIYAN_2 : SUPER_SAIYAN_3);
                    case POWER_DOWN -> { reset(p, SaiyanStatePacket.DOWN); return; }
                    default -> {}
                }
            }
            s.heldTicks = 0; send(s, SaiyanStatePacket.NONE);
        }
    }
    private static boolean charge(Session s, SaiyanRules.Form target) {
        ServerPlayer p = s.player;
        if (target == SUPER_SAIYAN_3 && s.form != SUPER_SAIYAN_2) return false;
        if (MagicData.getPlayerMagicData(p).isCasting() || ThundercrashManager.owns(p)) return false;
        var result = ModSpells.SAIYAN_ASCENSION.get().canBeCastedBy(1, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(p), p);
        if (!result.isSuccess()) return false;
        if (MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(p, SaiyanSpell.ID.toString(), 1,
                ModSpells.SAIYAN_ASCENSION.get().getSchoolType(), CastSource.SPELLBOOK))) return false;
        if (target != SUPER_SAIYAN_3 && !pay(p, SaiyanConfig.tier(target).cost().get())) {
            p.m_5661_(Component.m_237115_("message.irons_ultimate_explosion.saiyan.mana"), true); return false;
        }
        s.target = target; s.age = 0; s.duration = SaiyanConfig.tier(target).duration().get();
        s.consumed = true; slow(p); change(s, SaiyanStatePacket.CHARGE); return true;
    }
    private static boolean pay(ServerPlayer p, double cost) {
        MagicData data = MagicData.getPlayerMagicData(p); float mana = data.getMana();
        if (!Float.isFinite(mana) || mana < cost) return false;
        data.setMana((float)Math.max(0, mana - cost));
        Session session=SESSIONS.get(p.m_20148_());
        if (p.f_8906_ != null && (session==null || session.form!=SUPER_SAIYAN_3 || tick%10==0)) PacketDistributor.sendToPlayer(p, new SyncManaPacket(data));
        return data.getMana() <= mana - cost + .001;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return; tick++;
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            try {
            ServerPlayer p = s.player;
            if (!p.m_6084_() || p.m_213877_() || p.m_5833_() || !s.dimension.equals(dimension(p)) || !hasAccess(p)) { reset(p, SaiyanStatePacket.RESET); continue; }
            if (s.held) {
                if (tick - s.heartbeat > 15 || !validSlot(p, s.slot)) { s.held = false; s.heldTicks = 0; send(s, SaiyanStatePacket.NONE); }
                else if (!s.consumed && (s.target == BASE || s.target == SUPER_SAIYAN_3) && ++s.heldTicks >= SaiyanConfig.HOLD.get()) { reset(p, SaiyanStatePacket.DOWN); continue; }
            }
            if (s.form == SUPER_SAIYAN_3) {
                capThirdRegen(p);
                var drain=SaiyanRules.drainThird(MagicData.getPlayerMagicData(p).getMana(),SaiyanConfig.SSJ3.drain().get()/20);
                if (drain.form()==BASE || !pay(p,drain.amount()) || MagicData.getPlayerMagicData(p).getMana()<=0) { reset(p,SaiyanStatePacket.DOWN); continue; }
            } else if (s.form != BASE && ++s.drainTicks >= 10) {
                s.drainTicks = 0;
                SaiyanRules.Drain d = SaiyanRules.drain(s.form, MagicData.getPlayerMagicData(p).getMana(), SaiyanConfig.SSJ1.drain().get() / 2, SaiyanConfig.SSJ2.drain().get() / 2);
                if (d.form() == BASE) { reset(p, SaiyanStatePacket.DOWN); continue; }
                if (d.form() != s.form) { s.form = d.form(); s.target = BASE; s.age = 0; apply(s); change(s, SaiyanStatePacket.DROP); }
                if (!pay(p, d.amount())) { reset(p, SaiyanStatePacket.DOWN); continue; }
            }
            if (s.target != BASE && ++s.age >= s.duration) {
                if (s.target==SUPER_SAIYAN_3 && (!pay(p,SaiyanConfig.SSJ3.cost().get()) || MagicData.getPlayerMagicData(p).getMana()<=0)) { reset(p,SaiyanStatePacket.DOWN); continue; }
                s.form = s.target; s.target = BASE; s.age = 0; s.drainTicks = 0;
                apply(s); change(s, SaiyanStatePacket.BURST);
            }
            if (s.form==SUPER_SAIYAN_3 && p.m_20096_()!=s.grounded) {
                if(p.m_20096_()) cosmetic(s,p.m_20182_(),SaiyanCombatPacket.LAND);
                else if(p.m_20186_()>s.previousY) cosmetic(s,p.m_20182_(),SaiyanCombatPacket.JUMP);
            }
            s.grounded=p.m_20096_(); s.previousY=p.m_20186_();
            if (tick % 10 == 0) send(s, SaiyanStatePacket.NONE);
            } catch (RuntimeException error) {
                System.err.println("Saiyan Ascension reset after controller error: " + error);
                reset(s.player, SaiyanStatePacket.RESET);
            }
        }
    }
    public static Attribute criticalAttribute() {
        for(String id:new String[]{"irons_spellbooks:spell_critical_damage","irons_spellbooks:spell_crit_damage","traveloptics:spell_critical_damage","attributeslib:crit_damage"}) {
            Attribute attribute=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation(id));
            if(attribute!=null) return attribute;
        }
        return null;
    }
    private static Attribute[] attributes() { return new Attribute[]{AttributeRegistry.SPELL_POWER.get(), Attributes.f_22281_, Attributes.f_22279_, Attributes.f_22283_, Attributes.f_22278_, AttributeRegistry.MANA_REGEN.get(), Attributes.f_22279_, Attributes.f_22276_, criticalAttribute(), AttributeRegistry.MANA_REGEN.get()}; }
    public static void removeModifiers(ServerPlayer p) {
        Attribute[] attributes = attributes();
        for (int i = 0; i < attributes.length; i++) { AttributeInstance a = attributes[i]==null?null:p.m_21051_(attributes[i]); if (a != null) a.m_22120_(MODIFIERS[i]); }
    }
    private static void modifier(ServerPlayer p, int index, double amount, AttributeModifier.Operation operation) {
        Attribute attribute=attributes()[index]; AttributeInstance a = attribute==null?null:p.m_21051_(attribute);
        if (a != null) { a.m_22120_(MODIFIERS[index]); a.m_22118_(new AttributeModifier(MODIFIERS[index], "Saiyan Ascension", amount, operation)); }
    }
    private static void slow(ServerPlayer p) { modifier(p, 6, -.85, AttributeModifier.Operation.MULTIPLY_TOTAL); }
    private static void apply(Session s) {
        removeModifiers(s.player); if (s.form == BASE) return;
        var t = SaiyanConfig.tier(s.form); double[] bonuses = {t.power().get(), t.melee().get(), t.speed().get(), t.attack().get(), t.knockback().get(), t.regen().get() - 1};
        for (int i = 0; i < bonuses.length; i++) modifier(s.player, i, bonuses[i], i == 4 ? AttributeModifier.Operation.ADDITION : AttributeModifier.Operation.MULTIPLY_TOTAL);
        if(s.form==SUPER_SAIYAN_3) {
            modifier(s.player,7,SaiyanConfig.SSJ3_HEALTH.get(),AttributeModifier.Operation.MULTIPLY_TOTAL);
            modifier(s.player,8,SaiyanConfig.SSJ3_CRIT.get(),AttributeModifier.Operation.MULTIPLY_TOTAL);
            capThirdRegen(s.player);
        }
        if (s.target != BASE) slow(s.player);
    }
    private static void capThirdRegen(ServerPlayer p) {
        var a=p.m_21051_(AttributeRegistry.MANA_REGEN.get());
        double added=a.m_22115_(), base=0, total=1;
        for(var m:a.m_22122_()) if(!m.m_22209_().equals(MODIFIERS[9])) {
            switch(m.m_22217_()) { case ADDITION -> added+=m.m_22218_(); case MULTIPLY_BASE -> base+=m.m_22218_(); case MULTIPLY_TOTAL -> total*=1+m.m_22218_(); }
        }
        double rate=Math.max(0,(int)p.m_21133_(AttributeRegistry.MAX_MANA.get())*a.m_22099_().m_6740_(added*(1+base)*total)*.02*io.redspace.ironsspellbooks.config.ServerConfigs.MANA_REGEN_MULTIPLIER.get());
        double cap=SaiyanConfig.SSJ3.drain().get()*SaiyanConfig.SSJ3_REGEN_CAP.get();
        double amount=rate>cap && SaiyanConfig.SSJ3.drain().get()>0?cap/rate-1:0;
        var old=a.m_22111_(MODIFIERS[9]);
        if(amount==0) { if(old!=null) a.m_22120_(MODIFIERS[9]); }
        else if(old==null || Math.abs(old.m_22218_()-amount)>.000001) modifier(p,9,amount,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if(event.getEntity() instanceof ServerPlayer p && form(p)==SUPER_SAIYAN_3) {
            var motion=p.m_20184_(); p.m_20334_(motion.f_82479_,motion.f_82480_*(1+SaiyanConfig.SSJ3_JUMP.get()),motion.f_82481_);
        }
    }
    private static void cosmetic(Session s,net.minecraft.world.phys.Vec3 position,byte kind) {
        if(s.form!=SUPER_SAIYAN_3 || s.target!=BASE || tick-s.lastCosmetic<4) return;
        s.lastCosmetic=tick; ExplosionNetwork.saiyanCombat(s.player,new SaiyanCombatPacket(s.player.m_20148_(),s.dimension,position,kind));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void impact(LivingHurtEvent event) {
        if(event.getAmount()>0 && event.getSource().m_7639_() instanceof ServerPlayer p) {
            Session s=SESSIONS.get(p.m_20148_());
            if(s!=null) cosmetic(s,event.getEntity().m_20182_().m_82520_(0,.8,0),SaiyanCombatPacket.HIT);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void damage(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Session s = SESSIONS.get(p.m_20148_());
            if (s != null && s.form != BASE && !e.getSource().m_269533_(net.minecraft.tags.DamageTypeTags.f_268630_))
                e.setAmount((float)(e.getAmount() * (1 - SaiyanConfig.tier(s.form).resistance().get())));
        }
    }
    @SubscribeEvent public static void casting(SpellPreCastEvent e) {
        Session s = SESSIONS.get(e.getEntity().m_20148_());
        if (s != null && s.target != BASE) e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void castEffect(SpellPreCastEvent e) {
        Session s=SESSIONS.get(e.getEntity().m_20148_());
        if(s!=null && !e.isCanceled() && !SaiyanSpell.ID.toString().equals(e.getSpellId())) cosmetic(s,s.player.m_20182_().m_82520_(0,1,0),SaiyanCombatPacket.SPELL);
    }
    private static SaiyanStatePacket packet(Session s, byte cue) {
        return new SaiyanStatePacket(s.player.m_20148_(), s.player.m_19879_(), s.dimension, s.revision, tick,
                s.form.ordinal(), s.target.ordinal(), s.age, s.duration, s.held && !s.consumed ? s.heldTicks : -1,
                SaiyanConfig.HOLD.get(), cue, SaiyanConfig.SHAKE_RADIUS.get(), SaiyanConfig.VOLUME.get().floatValue(),s.form==SUPER_SAIYAN_3?SaiyanConfig.SSJ3_JUMP.get().floatValue():0);
    }
    private static void send(Session s, byte cue) { ExplosionNetwork.saiyanState(s.player, packet(s, cue)); }
    private static void change(Session s, byte cue) { s.revision = ++revision; send(s, cue); }
    public static void reset(ServerPlayer p, byte cue) {
        Session s = SESSIONS.remove(p.m_20148_()); removeModifiers(p);
        p.m_21153_(Math.min(p.m_21223_(),(float)p.m_21133_(Attributes.f_22276_)));
        if(s!=null && s.form==SUPER_SAIYAN_3 && p.f_8906_!=null) PacketDistributor.sendToPlayer(p,new SyncManaPacket(MagicData.getPlayerMagicData(p)));
        if (s != null) { s.form = BASE; s.target = BASE; s.held = false; s.age = 0; change(s, cue); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) { if (e.getOriginal() instanceof ServerPlayer p) reset(p, SaiyanStatePacket.RESET); if (e.getEntity() instanceof ServerPlayer p) removeModifiers(p); }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        Session s = SESSIONS.get(e.getTarget().m_20148_());
        if (s != null && e.getEntity() instanceof ServerPlayer viewer) ExplosionNetwork.saiyanTo(viewer, packet(s, SaiyanStatePacket.NONE));
    }
    @SubscribeEvent public static void untracking(PlayerEvent.StopTracking e) {
        Session s = SESSIONS.get(e.getTarget().m_20148_());
        if (s != null && e.getEntity() instanceof ServerPlayer viewer) {
            var p = packet(s, SaiyanStatePacket.RESET);
            ExplosionNetwork.saiyanTo(viewer, new SaiyanStatePacket(p.player(), p.entity(), p.dimension(), p.revision(), p.tick(), 0, 0, 0, 0, -1, p.holdDuration(), p.cue(), p.shakeRadius(), p.volume(),0));
        }
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e) { for (Session s : new ArrayList<>(SESSIONS.values())) reset(s.player, SaiyanStatePacket.RESET); SESSIONS.clear(); }
    private SaiyanManager() {}
}

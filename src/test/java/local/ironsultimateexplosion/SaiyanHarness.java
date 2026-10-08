package local.grandexplosiontest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import local.ironsultimateexplosion.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.util.*;
import static local.ironsultimateexplosion.SaiyanRules.Form.*;

/** Actual Forge-player, attribute, mana and lifecycle checks; opt-in and excluded from releases. */
public final class SaiyanHarness {
    private int passed, failed;
    private boolean block;
    private int inputSlot = -1;
    public SaiyanHarness() {
        if (Boolean.getBoolean("saiyan.diagnostics")) {
            MinecraftForge.EVENT_BUS.addListener(this::started);
            MinecraftForge.EVENT_BUS.addListener(this::preCast);
        }
    }
    private void preCast(SpellPreCastEvent e) { if (block && SaiyanSpell.ID.toString().equals(e.getSpellId())) e.setCanceled(true); }
    private void check(String name, boolean condition) { System.out.println((condition ? "SAIYAN_PASS " : "SAIYAN_FAIL ") + name); if (condition) passed++; else failed++; }
    private void near(String name, double actual, double expected) { check(name, Math.abs(actual - expected) < .001); }
    private void started(ServerStartedEvent e) {
        try { run(e.getServer().m_129783_()); } catch (Throwable t) { failed++; t.printStackTrace(System.out); }
        System.out.println("Saiyan diagnostics: " + passed + " passed, " + failed + " failed");
    }
    private void input(ServerPlayer p, byte action) { SaiyanManager.input(p, new SaiyanInputPacket(action, inputSlot)); }
    private void ticks(ServerPlayer p, int count, boolean hold) {
        for (int i = 0; i < count; i++) { if (hold && i % 5 == 0) input(p, SaiyanInputPacket.KEEP_HELD); SaiyanManager.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, p.m_9236_().m_7654_())); }
    }
    private void first(ServerPlayer p) {
        SaiyanManager.reset(p, SaiyanStatePacket.RESET); MagicData.getPlayerMagicData(p).setMana(1000);
        input(p, SaiyanInputPacket.PRESS); input(p, SaiyanInputPacket.RELEASE); ticks(p, 50, false);
        check("ssj1_completed", SaiyanManager.form(p) == SUPER_SAIYAN_1);
    }
    private void second(ServerPlayer p) { first(p); input(p, SaiyanInputPacket.PRESS); ticks(p, 5, true); input(p, SaiyanInputPacket.RELEASE); ticks(p, 60, false); check("ssj2_completed", SaiyanManager.form(p) == SUPER_SAIYAN_2); }
    private void clean(ServerPlayer p, String reason, double[] baseline) {
        check(reason + "_base", SaiyanManager.form(p) == BASE);
        Attribute[] attrs = attrs(); for (int i = 0; i < attrs.length; i++) near(reason + "_attribute_" + i, p.m_21133_(attrs[i]), baseline[i]);
    }
    private Attribute[] attrs() { return new Attribute[]{AttributeRegistry.SPELL_POWER.get(), Attributes.f_22281_, Attributes.f_22279_, Attributes.f_22283_, Attributes.f_22278_, AttributeRegistry.MANA_REGEN.get(), Attributes.f_22276_, SaiyanManager.criticalAttribute()}; }
    private void run(ServerLevel level) throws Exception {
        SpellConfigManager.onDatapackSync(new net.minecraftforge.event.OnDatapackSyncEvent(level.m_7654_().m_6846_(), null));
        ServerPlayer p = new ServerPlayer(level.m_7654_(), level, new GameProfile(UUID.randomUUID(), "SaiyanTest"));
        p.f_8906_ = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "SaiyanConn")).f_8906_;
        p.m_6034_(0, 90, 0); level.m_7967_(p);
        p.m_21051_(AttributeRegistry.MAX_MANA.get()).m_22100_(2000);
        ItemStack book = new ItemStack(ModItems.SAIYAN_BOOK.get()); ((SaiyanBook)ModItems.SAIYAN_BOOK.get()).initializeSpellContainer(book);
        p.m_8061_(EquipmentSlot.MAINHAND, book);
        var container = ISpellContainer.getOrCreate(book);
        check("one_locked_spell_slot", container.getMaxSpellCount() == 1 && container.getActiveSpellCount() == 1 && container.getSpellAtIndex(0).isLocked());
        check("book_spell_matches", container.getSpellAtIndex(0).getSpell() == ModSpells.SAIYAN_ASCENSION.get());
        check("book_access", SaiyanManager.hasAccess(p));
        double[] base = Arrays.stream(attrs()).mapToDouble(p::m_21133_).toArray();
        MagicData data = MagicData.getPlayerMagicData(p); data.setMana(1000);
        input(p, SaiyanInputPacket.PRESS); near("activation_cost_50", data.getMana(), 950);
        near("charge_slow", p.m_21133_(Attributes.f_22279_), base[2] * .15);
        input(p, SaiyanInputPacket.PRESS); near("repeated_press_no_extra_cost", data.getMana(), 950);
        input(p, SaiyanInputPacket.RELEASE); ticks(p, 49, false); check("not_instant", SaiyanManager.form(p) == BASE);
        ticks(p, 1, false); check("ssj1_after_50_ticks", SaiyanManager.form(p) == SUPER_SAIYAN_1);
        double[] one = {1.20,1.25,1.15,1.15,0.50,0.50};
        for (int i = 0; i < one.length; i++) near("ssj1_buff_" + i, p.m_21133_(attrs()[i]), i == 4 ? base[i] + one[i] : base[i] * one[i]);
        float before = data.getMana(); ticks(p, 20, false); near("separate_drain_10_per_second", before - data.getMana(), 10);
        data.setMana(100); new MagicManager().regenPlayerMana(p, data); float regenOne = data.getMana() - 100;
        SaiyanManager.reset(p, SaiyanStatePacket.RESET); data.setMana(100); new MagicManager().regenPlayerMana(p, data);
        near("native_regen_halved", regenOne, (data.getMana() - 100) * .5);
        first(p); input(p, SaiyanInputPacket.PRESS); float prior = data.getMana(); ticks(p, 5, true);
        check("mousedown_does_not_ascend", SaiyanManager.form(p) == SUPER_SAIYAN_1); near("mousedown_no_upgrade_cost", data.getMana(), prior);
        input(p, SaiyanInputPacket.RELEASE); near("upgrade_cost_75_on_release", data.getMana(), prior - 75);
        ticks(p, 59, false); check("ssj2_still_ssj1_before_three_seconds", SaiyanManager.form(p) == SUPER_SAIYAN_1);
        ticks(p, 1, false); check("ssj2_after_exactly_three_seconds", SaiyanManager.form(p) == SUPER_SAIYAN_2);
        double[] two = {1.35,1.40,1.25,1.25,0.80,0.25};
        for (int i = 0; i < two.length; i++) near("ssj2_replaces_buff_" + i, p.m_21133_(attrs()[i]), i == 4 ? base[i] + two[i] : base[i] * two[i]);
        before = data.getMana(); ticks(p, 20, false); near("ssj2_drain_17_per_second", before - data.getMana(), 17);
        input(p, SaiyanInputPacket.PRESS); input(p, SaiyanInputPacket.RELEASE); near("ssj3_charge_has_no_upfront_cost", data.getMana(), before - 17);
        check("ssj3_charge_retains_ssj2", SaiyanManager.form(p) == SUPER_SAIYAN_2);
        data.setMana(5); ticks(p, 10, false); check("burnout_downgrades", SaiyanManager.form(p) == SUPER_SAIYAN_1); near("fallback_pays_only_ssj1", data.getMana(), 0);
        ticks(p, 10, false); clean(p, "burnout", base); near("never_negative", data.getMana(), 0);
        first(p); input(p, SaiyanInputPacket.PRESS); ticks(p, 30, true); before = data.getMana(); input(p, SaiyanInputPacket.RELEASE);
        check("intermediate_hold_cancels", SaiyanManager.form(p) == SUPER_SAIYAN_1); near("intermediate_no_upgrade_cost", data.getMana(), before);
        first(p); input(p, SaiyanInputPacket.PRESS); ticks(p, 59, true); check("held_59_still_ssj1", SaiyanManager.form(p) == SUPER_SAIYAN_1); ticks(p, 1, true); clean(p, "held_60", base);
        first(p); input(p, SaiyanInputPacket.PRESS); ticks(p, 16, false); input(p, SaiyanInputPacket.RELEASE); ticks(p, 50, false);
        check("lost_heartbeat_cannot_ascend", SaiyanManager.form(p) == SUPER_SAIYAN_1);
        first(p); p.m_8061_(EquipmentSlot.MAINHAND, ItemStack.f_41583_); ticks(p, 1, false); clean(p, "book_removed", base); p.m_8061_(EquipmentSlot.MAINHAND, book);
        first(p); SaiyanManager.logout(new PlayerEvent.PlayerLoggedOutEvent(p)); clean(p, "logout", base);
        first(p); SaiyanManager.dimension(new PlayerEvent.PlayerChangedDimensionEvent(p, net.minecraft.world.level.Level.f_46428_, net.minecraft.world.level.Level.f_46429_)); clean(p, "dimension", base);
        second(p); SaiyanManager.death(new net.minecraftforge.event.entity.living.LivingDeathEvent(p, p.m_269291_().m_269264_())); clean(p, "death", base);
        first(p); SaiyanManager.respawn(new PlayerEvent.PlayerRespawnEvent(p, false)); clean(p, "respawn", base);
        SaiyanManager.reset(p, SaiyanStatePacket.RESET); data.setMana(49); input(p, SaiyanInputPacket.PRESS); clean(p, "insufficient_activation", base); near("failed_cast_no_mana_cost", data.getMana(), 49);
        data.setMana(1000); block = true; input(p, SaiyanInputPacket.PRESS); block = false; clean(p, "canceled_precast", base); near("canceled_precast_free", data.getMana(), 1000);
        for (int i = 0; i < 4; i++) { first(p); SaiyanManager.reset(p, SaiyanStatePacket.RESET); } clean(p, "repeated_cycles", base);
        first(p); LivingHurtEvent hurt = new LivingHurtEvent(p, p.m_269291_().m_269264_(), 20); SaiyanManager.damage(hurt); near("damage_resistance_15_percent", hurt.getAmount(), 17);
        second(p); hurt = new LivingHurtEvent(p, p.m_269291_().m_269264_(), 20); SaiyanManager.damage(hurt); near("damage_resistance_25_percent", hurt.getAmount(), 15);
        double oldDrain = SaiyanConfig.SSJ2.drain().get();
        try { SaiyanConfig.SSJ2.drain().set(0.0); data.setMana(0); input(p, SaiyanInputPacket.PRESS); ticks(p, 60, true); clean(p, "zero_mana_manual_down", base); near("manual_down_free", data.getMana(), 0); }
        finally { SaiyanConfig.SSJ2.drain().set(oldDrain); }
        SaiyanManager.reset(p, SaiyanStatePacket.RESET);
        ItemStack wig = new ItemStack(ModItems.SAIYAN_WIG.get()); ((SaiyanWig)ModItems.SAIYAN_WIG.get()).initializeSpellContainer(wig);
        var wigSpells = ISpellContainer.getOrCreate(wig);
        check("wig_one_locked_spell", wigSpells.getMaxSpellCount() == 1 && wigSpells.getActiveSpellCount() == 1 && wigSpells.getSpellAtIndex(0).isLocked());
        check("wig_requires_equipping", wigSpells.mustEquip());
        check("wig_is_accessory_not_armor", !(ModItems.SAIYAN_WIG.get() instanceof net.minecraft.world.item.ArmorItem));
        var headContext = new top.theillusivec4.curios.api.SlotContext("head", p, 0, false, true);
        check("wig_accepts_curios_head", ((SaiyanWig)ModItems.SAIYAN_WIG.get()).canEquip(headContext, wig));
        check("wig_rejects_spellbook_slot", !((SaiyanWig)ModItems.SAIYAN_WIG.get()).canEquip(new top.theillusivec4.curios.api.SlotContext("spellbook",p,0,false,true), wig));
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        check("pack_has_curios_head_slot", curios.getStacksHandler("head").isPresent());
        p.m_8061_(EquipmentSlot.MAINHAND, wig);
        check("held_wig_grants_no_power", !SaiyanManager.hasAccess(p));
        p.m_8061_(EquipmentSlot.MAINHAND, ItemStack.f_41583_);
        p.m_8061_(EquipmentSlot.HEAD, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("minecraft","diamond_helmet"))));
        ItemStack helmet = p.m_6844_(EquipmentSlot.HEAD);
        curios.setEquippedCurio("head",0,wig);
        check("equipped_wig_access", SaiyanManager.hasAccess(p));
        var options = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(p).getAllSpells();
        var wigOption = options.stream().filter(o -> o.spellData.getSpell() == ModSpells.SAIYAN_ASCENSION.get()).findFirst().orElseThrow();
        inputSlot = wigOption.globalIndex;
        first(p); check("wig_cast_with_empty_hands", SaiyanManager.form(p) == SUPER_SAIYAN_1);
        check("wig_preserves_armor_head_slot", p.m_6844_(EquipmentSlot.HEAD) == helmet);
        curios.setEquippedCurio("head",0,ItemStack.f_41583_); ticks(p,1,false);
        clean(p,"wig_removed",base); check("removed_wig_loses_access", !SaiyanManager.hasAccess(p));
        inputSlot = -1; p.m_8061_(EquipmentSlot.MAINHAND, book);
        thirdChecks(p,base,book);
        SaiyanManager.reset(p, SaiyanStatePacket.RESET); p.m_146870_();
    }

    private void third(ServerPlayer p) {
        second(p); input(p,SaiyanInputPacket.PRESS); input(p,SaiyanInputPacket.RELEASE); ticks(p,300,false);
        check("ssj3_completed",SaiyanManager.form(p)==SUPER_SAIYAN_3);
    }
    private void thirdChecks(ServerPlayer p,double[] base,ItemStack book) throws Exception {
        MagicData data=MagicData.getPlayerMagicData(p);
        near("ssj3_default_duration_15_seconds",SaiyanConfig.SSJ3.duration().get(),300);
        near("ssj3_default_drain_30",SaiyanConfig.SSJ3.drain().get(),30);
        check("native_critical_attribute_present",SaiyanManager.criticalAttribute()!=null);
        second(p); float before=data.getMana();
        input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.RELEASE);
        near("ssj3_no_early_activation_payment",data.getMana(),before);
        ticks(p,299,false); check("ssj3_not_complete_at_299",SaiyanManager.form(p)==SUPER_SAIYAN_2);
        near("ssj3_charge_retains_spell_bonus",p.m_21133_(attrs()[0]),base[0]*1.35);
        near("ssj3_charge_retains_health",p.m_21133_(attrs()[6]),base[6]);
        near("ssj3_charge_retains_crit",p.m_21133_(attrs()[7]),base[7]);
        ticks(p,1,false);check("ssj3_after_exactly_300_ticks",SaiyanManager.form(p)==SUPER_SAIYAN_3);
        near("ssj3_charge_only_pays_ssj2_drain",before-data.getMana(),255);
        double[] third={2,2,1.4,1.5,1,.25,1.4,1.5};
        for(int i=0;i<third.length;i++) near("ssj3_final_bonus_"+i,p.m_21133_(attrs()[i]),i==4?Math.min(1,base[i]+1):base[i]*third[i]);
        before=data.getMana();ticks(p,1,false);near("ssj3_drain_first_tick",before-data.getMana(),1.5);
        ticks(p,19,false);near("ssj3_drain_30_each_second",before-data.getMana(),30);
        ticks(p,100,false); for(int i:new int[]{0,1,2,3,6,7}) near("ssj3_no_stacking_"+i,p.m_21133_(attrs()[i]),base[i]*third[i]);
        var hurt=new LivingHurtEvent(p,p.m_269291_().m_269264_(),20);SaiyanManager.damage(hurt);near("ssj3_resistance_35_percent",hurt.getAmount(),13);
        net.minecraft.world.damagesource.DamageSource bypass=null;
        for(var method:p.m_269291_().getClass().getMethods()) if(method.getParameterCount()==0 && method.getReturnType()==net.minecraft.world.damagesource.DamageSource.class) {
            var source=(net.minecraft.world.damagesource.DamageSource)method.invoke(p.m_269291_());
            if(source.m_269533_(net.minecraft.tags.DamageTypeTags.f_268630_)){bypass=source;break;}
        }
        check("native_resistance_bypass_source_found",bypass!=null);
        hurt=new LivingHurtEvent(p,bypass,20);SaiyanManager.damage(hurt);near("ssj3_resistance_respects_bypass",hurt.getAmount(),20);
        p.m_20334_(0,.42,0);SaiyanManager.jump(new LivingEvent.LivingJumpEvent(p));near("ssj3_jump_strength",p.m_20184_().f_82480_,.42*1.35);
        p.m_21153_(28);SaiyanManager.reset(p,SaiyanStatePacket.RESET);clean(p,"ssj3_reset",base);near("ssj3_health_clamped_on_end",p.m_21223_(),20);
        third(p);data.setMana(100);new MagicManager().regenPlayerMana(p,data);near("ssj3_native_regen_quarter",data.getMana()-100,5);
        double maxMana=p.m_21133_(AttributeRegistry.MAX_MANA.get());double nativeRegen=p.m_21051_(AttributeRegistry.MANA_REGEN.get()).m_22115_();
        p.m_21051_(AttributeRegistry.MAX_MANA.get()).m_22100_(20000);p.m_21051_(AttributeRegistry.MANA_REGEN.get()).m_22100_(5);
        ticks(p,1,false);data.setMana(1000);new MagicManager().regenPlayerMana(p,data);near("ssj3_extreme_native_regen_capped",data.getMana()-1000,7.5);
        before=data.getMana();for(int i=0;i<20;i++){if(i%10==0)new MagicManager().regenPlayerMana(p,data);ticks(p,1,false);}near("ssj3_net_drain_still_15",before-data.getMana(),15);
        data.setMana(0);new MagicManager().regenPlayerMana(p,data);check("ssj3_mana_restoration_can_help",data.getMana()>0);ticks(p,1,false);check("ssj3_refill_keeps_form",SaiyanManager.form(p)==SUPER_SAIYAN_3);
        p.m_21051_(AttributeRegistry.MAX_MANA.get()).m_22100_(maxMana);p.m_21051_(AttributeRegistry.MANA_REGEN.get()).m_22100_(nativeRegen);ticks(p,1,false);near("ssj3_dynamic_regen_cap_removed",p.m_21133_(AttributeRegistry.MANA_REGEN.get()),base[5]*.25);
        data.setMana(1.5f);ticks(p,1,false);clean(p,"ssj3_exact_zero_goes_base",base);near("ssj3_exact_payment_zero",data.getMana(),0);
        third(p);data.setMana(1);ticks(p,1,false);clean(p,"ssj3_insufficient_goes_base",base);near("ssj3_insufficient_never_negative",data.getMana(),1);
        third(p);data.setMana(0);ticks(p,1,false);clean(p,"ssj3_zero_cancels_next_tick",base);
        third(p);input(p,SaiyanInputPacket.PRESS);ticks(p,59,true);check("ssj3_held_59_active",SaiyanManager.form(p)==SUPER_SAIYAN_3);ticks(p,1,true);clean(p,"ssj3_held_60",base);
        second(p);input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.RELEASE);ticks(p,60,false);input(p,SaiyanInputPacket.PRESS);ticks(p,60,true);clean(p,"ssj3_hold_cancels_charge",base);
        second(p);input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.RELEASE);ticks(p,50,false);input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.CANCEL);clean(p,"ssj3_input_cancels_charge",base);
        int oldCost=SaiyanConfig.SSJ3.cost().get();
        try { SaiyanConfig.SSJ3.cost().set(99);second(p);before=data.getMana();input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.RELEASE);near("ssj3_configurable_fee_deferred",data.getMana(),before);ticks(p,300,false);check("ssj3_paid_fee_completes",SaiyanManager.form(p)==SUPER_SAIYAN_3);near("ssj3_fee_paid_at_completion",before-data.getMana(),354); }
        finally { SaiyanConfig.SSJ3.cost().set(oldCost); }
        third(p);SaiyanManager.logout(new PlayerEvent.PlayerLoggedOutEvent(p));clean(p,"ssj3_logout",base);
        third(p);SaiyanManager.dimension(new PlayerEvent.PlayerChangedDimensionEvent(p,net.minecraft.world.level.Level.f_46428_,net.minecraft.world.level.Level.f_46429_));clean(p,"ssj3_dimension",base);
        third(p);SaiyanManager.death(new LivingDeathEvent(p,p.m_269291_().m_269264_()));clean(p,"ssj3_death",base);
        third(p);p.m_8061_(EquipmentSlot.MAINHAND,ItemStack.f_41583_);ticks(p,1,false);clean(p,"ssj3_access_removed",base);p.m_8061_(EquipmentSlot.MAINHAND,book);
        second(p);input(p,SaiyanInputPacket.PRESS);input(p,SaiyanInputPacket.RELEASE);ticks(p,80,false);p.m_8061_(EquipmentSlot.MAINHAND,ItemStack.f_41583_);ticks(p,1,false);clean(p,"ssj3_charge_access_removed",base);p.m_8061_(EquipmentSlot.MAINHAND,book);
        for(int i=0;i<3;i++){third(p);SaiyanManager.reset(p,SaiyanStatePacket.RESET);}clean(p,"ssj3_repeated_cycles",base);
        near("ssj1_drain_preserved",SaiyanConfig.SSJ1.drain().get(),10);near("ssj2_drain_preserved",SaiyanConfig.SSJ2.drain().get(),17);
        passed+=SaiyanPacketCheck.run();
    }
}

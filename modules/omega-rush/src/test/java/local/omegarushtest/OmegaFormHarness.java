package local.omegarushtest;
import local.omegarush.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
public final class OmegaFormHarness {
    private int passed,failed,sequence,poseTicks;private ServerPlayer p,poseActor;private final List<Entity> spawned=new ArrayList<>();
    public OmegaFormHarness(){MinecraftForge.EVENT_BUS.addListener(this::commands);MinecraftForge.EVENT_BUS.addListener(this::poseTick);}
    private void poseTick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END&&poseActor!=null&&--poseTicks<=0){OmegaFormManager.end(poseActor,false);poseActor.m_146870_();poseActor=null;}}
    private void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("omega_form_test").requires(s->s.m_6761_(4)).executes(c->{try{run(c.getSource().m_81372_());return 1;}catch(Throwable error){System.out.println("OMEGA_FORM_TEST_FAIL "+error);error.printStackTrace();return 0;}}));
        e.getDispatcher().register(LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("omega_form_pose").requires(s->s.m_6761_(4)).executes(c->{
            var level=c.getSource().m_81372_();if(poseActor!=null){OmegaFormManager.end(poseActor,false);poseActor.m_146870_();}
            poseActor=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),"OmegaFlexActor"));poseActor.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"FlexConn")).f_8906_;
            poseActor.m_6034_(0.5,-60,4.5);poseActor.m_146922_(180);poseActor.m_146926_(0);
            CuriosApi.getCuriosInventory(poseActor).ifPresent(inv->inv.setEquippedCurio("charm",0,OmegaItems.SCARF.get().m_7968_()));
            var info=new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),List.of(poseActor));
            for(var viewer:level.m_7654_().m_6846_().m_11314_())viewer.f_8906_.m_9829_(info);level.m_7967_(poseActor);OmegaFormManager.charge(poseActor,200);poseTicks=225;
            System.out.println("OMEGA_FORM_POSE render_only_extended_charge=true");return 1;
        }));
    }
    private void check(String name,boolean ok){if(!ok){failed++;System.out.println("OMEGA_FORM_TEST_ASSERT_FAIL "+name);}else{passed++;System.out.println("OMEGA_FORM_TEST_PASS "+name);}}
    private Object field(Object target,String name)throws Exception{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private Object state()throws Exception{var f=OmegaFormManager.class.getDeclaredField("STATES");f.setAccessible(true);return ((Map<?,?>)f.get(null)).get(p.m_20148_());}
    private void equip(boolean on){CuriosApi.getCuriosInventory(p).orElseThrow(()->new AssertionError("curio_inventory")).setEquippedCurio("charm",0,on?OmegaItems.SCARF.get().m_7968_():ItemStack.f_41583_);}
    private void start()throws Exception{
        OmegaManager.abort(p);OmegaFormManager.end(p,false);equip(true);p.m_6034_(32,120,32);p.m_6853_(true);p.m_20242_(false);p.m_7911_(0);
        MagicData.getPlayerMagicData(p).setMana(10000);check("activate_"+passed,OmegaFormManager.activate(p,CastSource.SPELLBOOK));sequence=0;
    }
    private void steps(int count,float forward,float up)throws Exception{
        for(int i=0;i<count;i++){
            if(state()!=null){long session=(long)field(state(),"session");OmegaFormManager.input(p,new OmegaFormInputPacket(session,++sequence,0,0,forward,0,up));}
            OmegaFormManager.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END,()->true,p.f_8924_));
            OmegaManager.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END,()->true,p.f_8924_));
        }
    }
    private void run(ServerLevel level)throws Exception{
        passed=failed=0;level.m_6325_(2,2);level.m_6325_(2,3);level.m_6325_(3,2);
        p=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),"OmegaFormTest"));p.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"FormConn")).f_8906_;
        var spawn=ServerPlayer.class.getDeclaredField("f_8921_");spawn.setAccessible(true);spawn.setInt(p,0);
        p.m_21051_(Attributes.f_22276_).m_22100_(500);p.m_21153_(500);level.m_7967_(p);spawned.add(p);p.m_6034_(32,120,32);p.m_6853_(true);
        p.m_21051_(AttributeRegistry.MAX_MANA.get()).m_22100_(10000);
        MinecraftForge.EVENT_BUS.post(new OnDatapackSyncEvent(level.m_7654_().m_6846_(),null));
        boolean require=OmegaConfig.REQUIRE_FORM.get();double upkeep=OmegaConfig.FORM_UPKEEP.get();
        try{
            OmegaConfig.REQUIRE_FORM.set(true);
            var spell=ModSpells.OMEGA_FORM.get();var rush=ModSpells.OMEGA_RUSH.get();var magic=MagicData.getPlayerMagicData(p);
            check("default_cost_upkeep_cooldown",spell.getManaCost(1)==200&&OmegaConfig.FORM_UPKEEP.get()==30&&spell.getSpellCooldown()==2400&&spell.getCastTime(1)==30);
            check("form_not_craftable_or_lootable",!spell.allowCrafting()&&!spell.allowLooting()&&spell.getMaxLevel()==1);
            check("no_scarf_no_form",!spell.canBeCastedBy(1,CastSource.SWORD,magic,p).isSuccess());
            check("no_form_no_rush",!OmegaManager.start(p,1,12));
            ItemStack scarf=OmegaItems.SCARF.get().m_7968_();check("scarf_unstackable",scarf.m_41741_()==1);
            var container=ISpellContainer.get(scarf);check("bound_equipped_container",container.mustEquip()&&container.isSpellWheel()&&container.getSpellAtIndex(0).getSpell()==spell);
            p.m_8061_(net.minecraft.world.entity.EquipmentSlot.MAINHAND,scarf);check("holding_does_not_unlock",!FloweryScarf.equipped(p));p.m_8061_(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.f_41583_);
            var inventory=CuriosApi.getCuriosInventory(p).orElseThrow(()->new AssertionError("curios_present"));check("charm_slot_present",inventory.getStacksHandler("charm").isPresent());
            var handler=inventory.getStacksHandler("charm").orElseThrow();handler.getCosmeticStacks().setStackInSlot(0,scarf);check("cosmetic_does_not_unlock",!FloweryScarf.equipped(p));handler.getCosmeticStacks().setStackInSlot(0,ItemStack.f_41583_);
            equip(true);check("equipped_scarf_unlocks",FloweryScarf.equipped(p));
            var selection=new SpellSelectionManager(p);check("native_spell_wheel_lists_form",selection.getAllSpells().stream().anyMatch(s->s.spellData.getSpell()==spell));
            var inscription=new io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu(2,p.m_150109_(),net.minecraft.world.inventory.ContainerLevelAccess.m_39289_(level,new net.minecraft.core.BlockPos(32,120,32)));
            check("inscription_rejects_scarf",!inscription.getSpellBookSlot().m_5857_(scarf)&&!inscription.getScrollSlot().m_5857_(scarf));
            magic.setMana(1000);OmegaFormManager.charge(p,30);spell.castSpell(level,1,p,CastSource.SPELLBOOK,false);
            System.out.println("OMEGA_FORM_TEST_ACTIVATION active="+OmegaFormManager.active(p)+" mana="+magic.getMana());
            check("native_activation_charges_once",OmegaFormManager.active(p)&&magic.getMana()==800);check("native_recast_registered",magic.getPlayerRecasts().hasRecastForSpell(spell));
            check("form_status_has_no_time_cap",p.m_21124_(OmegaEffects.FORM.get()).m_267577_());
            steps(20,0,0);check("upkeep_exact_30_per_second",Math.abs(magic.getMana()-770)<0.001);
            check("absorption_once",p.m_6103_()==16);steps(10,0,0);check("absorption_not_refilled",p.m_6103_()==16);
            var nature=p.m_21051_(AttributeRegistry.NATURE_SPELL_POWER.get());check("nature_times_1_25",Math.abs(nature.m_22135_()-1.25)<0.001);
            check("rush_power_buffed_once",Math.abs(rush.getSpellPower(1,p)-15)<0.001);
            check("strength_one",Math.abs(p.m_21051_(Attributes.f_22281_).m_22135_()-4)<0.001);
            float mana=magic.getMana();spell.castSpell(level,1,p,CastSource.SWORD,true);
            check("instant_free_dismissal",!OmegaFormManager.active(p)&&magic.getMana()==mana);check("cooldown_starts_on_end",magic.getPlayerCooldowns().isOnCooldown(spell));
            check("buffs_removed",p.m_6103_()==0&&Math.abs(nature.m_22135_()-1)<0.001&&Math.abs(p.m_21051_(Attributes.f_22281_).m_22135_()-1)<0.001);
            start();p.m_6853_(false);steps(20,1,1);check("hover_moves_and_ascends",p.m_20189_()>36&&p.m_20186_()>123&&OmegaFormManager.owns(p)&&p.m_20068_());
            double altitude=p.m_20186_();steps(10,0,0);check("release_hovers",Math.abs(p.m_20186_()-altitude)<0.01);steps(5,0,-1);check("sneak_descends",p.m_20186_()<altitude-0.9);
            var control=p.m_150110_();check("no_creative_abilities",!control.f_35935_&&!control.f_35936_);
            long session=(long)field(state(),"session");int accepted=(int)field(state(),"sequence");OmegaFormManager.input(p,new OmegaFormInputPacket(session,accepted+1,Float.NaN,0,1,0,1));check("invalid_input_rejected",(int)field(state(),"sequence")==accepted);
            OmegaFormManager.input(p,new OmegaFormInputPacket(session-1,accepted+1,0,0,1,0,1));check("wrong_session_rejected",(int)field(state(),"sequence")==accepted);
            OmegaFormManager.input(p,new OmegaFormInputPacket(session,accepted,0,0,1,0,1));check("old_sequence_rejected",(int)field(state(),"sequence")==accepted);
            check("rush_from_hover",OmegaManager.start(p,1,rush.getSpellPower(1,p)));check("hover_yields_to_rush",!OmegaFormManager.owns(p)&&OmegaManager.owns(p)&&p.m_20068_());
            OmegaFormManager.end(p,true);check("form_end_keeps_committed_rush",!OmegaFormManager.active(p)&&OmegaManager.owns(p)&&p.m_20068_());
            var states=OmegaManager.class.getDeclaredField("STATES");states.setAccessible(true);Object rushState=((Map<?,?>)states.get(null)).get(p.m_20148_());check("rush_keeps_captured_power",Math.abs((float)field(rushState,"damage")-15)<0.001);
            long rushSession=(long)field(rushState,"session");OmegaManager.input(p,new OmegaInputPacket(rushSession,1,0,0,true));check("gravity_restored_after_both_end",!p.m_20068_()&&!OmegaManager.owns(p));
            var fall=new LivingFallEvent(p,30,1);OmegaManager.fall(fall);check("landing_protection",fall.isCanceled());
            start();OmegaManager.charge(p,1,16);float before=magic.getMana();OmegaFormManager.end(p,true);rush.castSpell(level,1,p,CastSource.SPELLBOOK,true);check("form_expiry_cancels_rush_charge_free",magic.getMana()==before&&!OmegaManager.owns(p));
            start();magic.setMana(1);steps(1,0,0);check("insufficient_upkeep_ends_form",!OmegaFormManager.active(p)&&magic.getMana()==1);
            start();equip(false);steps(1,0,0);check("unequip_ends_form",!OmegaFormManager.active(p)&&p.m_6103_()==0);
            start();p.m_7911_(20);var buffs=field(state(),"buffs");p.f_19802_=0;
            var critAttribute=ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_chance"));if(critAttribute!=null)p.m_21051_(critAttribute).m_22100_(0);
            p.m_6469_(rush.getDamageSource(p),10);check("resistance_and_absorption_damage",Math.abs(p.m_6103_()-16)<0.01&&Math.abs((float)field(buffs,"absorption")-12)<0.01);
            OmegaFormManager.end(p,false);check("external_absorption_preserved",Math.abs(p.m_6103_()-4)<0.01);
            start();var strength=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("minecraft","strength"));p.m_7292_(new MobEffectInstance(strength,100,1));steps(1,0,0);check("stronger_strength_wins",Math.abs(p.m_21051_(Attributes.f_22281_).m_22135_()-7)<0.01);
            OmegaFormManager.end(p,false);check("external_strength_preserved",p.m_21023_(strength)&&Math.abs(p.m_21051_(Attributes.f_22281_).m_22135_()-7)<0.01);p.m_21195_(strength);
            start();var resist=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("minecraft","resistance"));p.m_7292_(new MobEffectInstance(resist,100,3));var hurt=new LivingHurtEvent(p,rush.getDamageSource(p),10);OmegaFormManager.hurt(hurt);check("stronger_resistance_no_double_reduction",hurt.getAmount()==10);OmegaFormManager.end(p,false);check("external_resistance_preserved",p.m_21023_(resist));p.m_21195_(resist);
            start();p.m_6853_(false);steps(2,1,1);OmegaFormManager.teleport(p);p.m_6034_(48,125,48);steps(1,0,0);check("teleport_preserves_form_and_resets_hover",OmegaFormManager.active(p)&&p.m_20185_()==48);
            p.m_21195_(OmegaEffects.FORM.get());steps(1,0,0);check("milk_effect_removal_cleans_form",!OmegaFormManager.active(p)&&!p.m_20068_());
            start();p.m_6853_(false);steps(2,0,1);check("rush_transition_active",OmegaManager.start(p,1,rush.getSpellPower(1,p)));
            var rs=((Map<?,?>)states.get(null)).get(p.m_20148_());OmegaManager.input(p,new OmegaInputPacket((long)field(rs,"session"),1,0,0,true));steps(1,0,0);
            check("rush_returns_to_hover",OmegaFormManager.active(p)&&OmegaFormManager.owns(p)&&p.m_20068_());OmegaFormManager.end(p,false);check("hover_end_restores_original_gravity",!p.m_20068_());
            start();p.m_6853_(false);steps(1,0,1);OmegaManager.start(p,1,rush.getSpellPower(1,p));magic.setMana(1);steps(1,0,0);
            check("depletion_during_rush_finishes_rush",!OmegaFormManager.active(p)&&OmegaManager.owns(p)&&p.m_20068_());OmegaManager.abort(p);check("depletion_rush_cleanup_gravity",!p.m_20068_());
            start();p.m_6853_(false);steps(1,0,1);OmegaManager.start(p,1,rush.getSpellPower(1,p));equip(false);steps(1,0,0);check("scarf_removed_during_rush_keeps_rush",!OmegaFormManager.active(p)&&OmegaManager.owns(p));OmegaManager.abort(p);
            start();float savedAbs=p.m_6103_();equip(true);steps(1,0,0);check("scarf_swap_does_not_refill",OmegaFormManager.active(p)&&p.m_6103_()==savedAbs);
            check("active_recast_ignores_cooldown",spell.canBeCastedBy(1,CastSource.SWORD,magic,p).isSuccess());
            var block=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(p,"irons_ultimate_explosion:thundercrash",1,SchoolRegistry.LIGHTNING.get(),CastSource.SPELLBOOK);OmegaFormManager.otherCast(block);check("form_blocks_thundercrash",block.isCanceled());
            var ordinary=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(p,rush.getSpellId(),1,SchoolRegistry.NATURE.get(),CastSource.SPELLBOOK);OmegaFormManager.otherCast(ordinary);check("form_allows_other_casting",!ordinary.isCanceled());
            OmegaFormManager.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));check("logout_cleans_buffs",!OmegaFormManager.active(p)&&p.m_6103_()==0);
            start();OmegaFormManager.dimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,level.m_46472_(),net.minecraft.world.level.Level.f_46429_));check("dimension_cleans_buffs",!OmegaFormManager.active(p));
            start();OmegaFormManager.death(new LivingDeathEvent(p,rush.getDamageSource(p)));check("death_cleans_buffs",!OmegaFormManager.active(p));
            protocolCases();
            magic.getPlayerCooldowns().clearCooldowns();equip(true);magic.setMana(1000);OmegaFormManager.charge(p,30);spell.castSpell(level,1,p,CastSource.SWORD,false);
            check("equipped_sword_source_cost",OmegaFormManager.active(p)&&magic.getMana()==800);OmegaFormManager.end(p,true);check("equipped_source_cooldown",magic.getPlayerCooldowns().isOnCooldown(spell));
            magic.getPlayerCooldowns().clearCooldowns();magic.setMana(199);OmegaFormManager.charge(p,30);spell.castSpell(level,1,p,CastSource.SWORD,false);check("mana_drop_during_charge_rejected",!OmegaFormManager.active(p)&&magic.getMana()==199);
            var flex=OmegaChargeAnimation.create(30);check("charge_flex_animation_valid",flex.isInfinite()&&flex.getPart("rightArm").bend.isEnabled()&&flex.getPart("leftArm").bend.isEnabled());
            lootCases(level);
            if(failed>0)throw new AssertionError("failed="+failed+" passed="+passed);
            System.out.println("OMEGA_FORM_TEST_COMPLETE assertions="+passed);
        }finally{OmegaConfig.REQUIRE_FORM.set(require);OmegaConfig.FORM_UPKEEP.set(upkeep);if(p!=null){OmegaManager.abort(p);OmegaFormManager.end(p,false);}for(Entity e:spawned)e.m_146870_();spawned.clear();}
    }
    private void protocolCases()throws Exception{
        var type=OmegaFormInputPacket.class;var encode=type.getDeclaredMethod("encode",type,net.minecraft.network.FriendlyByteBuf.class);var decode=type.getDeclaredMethod("decode",net.minecraft.network.FriendlyByteBuf.class);encode.setAccessible(true);decode.setAccessible(true);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{var original=new OmegaFormInputPacket(7,4,50,20,0.5f,-1,1);encode.invoke(null,original,buffer);check("form_input_32_bytes",buffer.readableBytes()==32);check("form_input_round_trip",original.equals(decode.invoke(null,buffer)));buffer.clear();buffer.writeByte(1);boolean rejected=false;try{decode.invoke(null,buffer);}catch(java.lang.reflect.InvocationTargetException e){rejected=e.getCause() instanceof IllegalArgumentException;}check("malformed_form_input_rejected",rejected);}finally{buffer.release();}
    }
    private void lootCases(ServerLevel level)throws Exception{
        check("drop_default_rates",OmegaConfig.BOSS_CHANCE.get()==0.05&&OmegaConfig.CHEST_CHANCE.get()==0.002);
        for(String id:List.of("irons_spellbooks:dead_king","irons_spellbooks:fire_boss","cataclysm:ender_guardian","cataclysm:the_harbinger","cataclysm:the_leviathan","cataclysm:ancient_remnant","cataclysm:maledictus","cataclysm:scylla"))check("eligible_boss_"+id,OmegaLoot.eligibleBoss(new ResourceLocation(id)));
        for(String id:List.of("cataclysm:ignis","cataclysm:netherite_monstrosity","minecraft:zombie"))check("excluded_boss_"+id,!OmegaLoot.eligibleBoss(new ResourceLocation(id)));
        double prior=OmegaConfig.CHEST_CHANCE.get();try{
            OmegaConfig.CHEST_CHANCE.set(1.0);var modifier=new OmegaLoot.Chest(new LootItemCondition[0]);
            var params=new LootParams.Builder(level).m_287286_(LootContextParams.f_81460_,new Vec3(32,120,32)).m_287235_(LootContextParamSets.m_81431_(new ResourceLocation("chest")));
            var context=new LootContext.Builder(params).withQueriedLootTableId(new ResourceLocation("minecraft","chests/simple_dungeon")).m_287259_(null);
            var loot=new ObjectArrayList<ItemStack>();loot.add(new ItemStack(Items.f_42516_));modifier.apply(loot,context);check("chest_appends_preserves_existing",loot.size()==2&&loot.get(0).m_150930_(Items.f_42516_)&&loot.get(1).m_150930_(OmegaItems.SCARF.get()));
            modifier.apply(loot,context);check("nested_context_roll_once",loot.size()==2);
            var entityContext=new LootContext.Builder(params).withQueriedLootTableId(new ResourceLocation("minecraft","entities/zombie")).m_287259_(null);var none=new ObjectArrayList<ItemStack>();modifier.apply(none,entityContext);check("entity_loot_excluded",none.isEmpty());
            net.minecraft.core.BlockPos position=new net.minecraft.core.BlockPos(35,120,35);var chestBlock=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft","chest"));level.m_7731_(position,chestBlock.m_49966_(),3);
            try{var chest=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)level.m_7702_(position);chest.m_59626_(new ResourceLocation("minecraft","chests/simple_dungeon"),123);chest.m_59640_(p);int count=0,total=0;for(int slot=0;slot<27;slot++){var item=chest.m_8020_(slot);if(!item.m_41619_())total++;if(item.m_150930_(OmegaItems.SCARF.get()))count+=item.m_41613_();}check("natural_chest_generation_one_scarf",count==1&&total>1);chest.m_59640_(p);int again=0;for(int slot=0;slot<27;slot++)if(chest.m_8020_(slot).m_150930_(OmegaItems.SCARF.get()))again+=chest.m_8020_(slot).m_41613_();check("reopen_does_not_reroll",again==1);}finally{level.m_7731_(position,net.minecraft.world.level.block.Blocks.f_50016_.m_49966_(),3);}
        }finally{OmegaConfig.CHEST_CHANCE.set(prior);}
        double bossChance=OmegaConfig.BOSS_CHANCE.get();try{
            OmegaConfig.BOSS_CHANCE.set(1.0);var source=ModSpells.OMEGA_RUSH.get().getDamageSource(p);
            LivingEntity boss=(LivingEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("irons_spellbooks","dead_king")).m_20615_(level);spawned.add(boss);var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();var event=new LivingDropsEvent(boss,source,drops,100,true);OmegaLoot.drop(event);check("boss_drop_fixed_chance_not_looting",drops.size()==1&&drops.get(0).m_32055_().m_150930_(OmegaItems.SCARF.get()));OmegaLoot.drop(event);check("boss_coop_event_not_duplicated",drops.size()==1);
            LivingEntity noPlayer=(LivingEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("irons_spellbooks","dead_king")).m_20615_(level);spawned.add(noPlayer);var unclaimed=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();OmegaLoot.drop(new LivingDropsEvent(noPlayer,new net.minecraft.world.damagesource.DamageSource(source.m_269150_()),unclaimed,0,false));check("boss_requires_player_credit",unclaimed.isEmpty());
            LivingEntity excluded=(LivingEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft","zombie")).m_20615_(level);spawned.add(excluded);var wrong=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();OmegaLoot.drop(new LivingDropsEvent(excluded,source,wrong,0,true));check("ordinary_mob_no_scarf",wrong.isEmpty());
        }finally{OmegaConfig.BOSS_CHANCE.set(bossChance);}
    }
}

package local.grandexplosiontest;
import local.ironsultimateexplosion.*;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Runs only when explicitly invoked in a disposable world. Not packaged in the release. */
public final class ThundercrashHarness {
    private int passed;
    public ThundercrashHarness() { MinecraftForge.EVENT_BUS.addListener(this::commands); }
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("thundercrash_test")
                .requires(s -> s.m_6761_(4)).executes(c -> {
                    try { run(c.getSource().m_81372_()); return 1; }
                    catch(Throwable t) { System.out.println("THUNDER_TEST_FAIL " + t); t.printStackTrace(System.out); return 0; }
                }));
    }
    private void check(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        passed++; System.out.println("THUNDER_TEST_PASS " + name);
    }
    private void steps(int count) { for(int i=0;i<count;i++) ThundercrashManager.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer())); }
    @SuppressWarnings("unchecked") private Object state(ServerPlayer p) throws Exception {
        var field = ThundercrashManager.class.getDeclaredField("STATES"); field.setAccessible(true);
        return ((Map<UUID,Object>)field.get(null)).get(p.m_20148_());
    }
    private void place(ServerLevel level, BlockPos p, boolean solid) { level.m_7731_(p,(solid?Blocks.f_50069_:Blocks.f_50016_).m_49966_(),3); }
    private void discharge(ServerPlayer caster, Vec3 point) throws Exception {
        if (!ThundercrashManager.start(caster, 80)) throw new AssertionError("discharge activation");
        Object active=state(caster);
        var finish=ThundercrashManager.class.getDeclaredMethod("finish",active.getClass(),ThundercrashCollision.Hit.class,boolean.class);
        finish.setAccessible(true); finish.invoke(null,active,new ThundercrashCollision.Hit(0,point,null),true);
    }
    private ServerPlayer player(ServerLevel level, String name, double x, double z) throws Exception {
        ServerPlayer p=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),name));
        p.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),name+"Conn")).f_8906_;
        p.m_6034_(x,80,z); p.m_146922_(0); p.m_146926_(0);
        var spawn=ServerPlayer.class.getDeclaredField("f_8921_"); spawn.setAccessible(true); spawn.setInt(p,0);
        p.m_21051_(Attributes.f_22276_).m_22100_(500); p.m_21153_(500); level.m_7967_(p);
        return p;
    }
    private void run(ServerLevel level) throws Exception {
        passed=0;
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(new net.minecraftforge.event.OnDatapackSyncEvent(level.m_7654_().m_6846_(), null));
        ServerPlayer caster=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.fromString("74c5a4d1-7b1d-4a99-a862-740c58c00101"),"ThunderTest"));
        caster.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"ThunderConn")).f_8906_;
        caster.m_6034_(0,80,0); level.m_7967_(caster);
        try {
        AbstractSpell spell=ModSpells.THUNDERCRASH.get();
        check("lightning_school",spell.getSchoolType()==SchoolRegistry.LIGHTNING.get());
        check("scroll_forge_craftable",spell.allowCrafting() && spell.canBeCraftedBy(caster));
        check("legendary_five_levels",spell.getMinRarity()==SpellRarity.LEGENDARY.getValue() && spell.getMaxLevel()==5);
        check("level_scaling",spell.getSpellPower(5,caster)>spell.getSpellPower(1,caster));
        int oldBase=ThundercrashConfig.BASE_POWER.get(), oldPerLevel=ThundercrashConfig.POWER_PER_LEVEL.get();
        float defaultOne=spell.getSpellPower(1,caster), defaultFive=spell.getSpellPower(5,caster);
        try {
            ThundercrashConfig.BASE_POWER.set(60); ThundercrashConfig.POWER_PER_LEVEL.set(20);
            check("configurable_base_power",Math.abs(spell.getSpellPower(1,caster)-defaultOne*1.5)<0.001);
            check("configurable_level_progression",Math.abs(spell.getSpellPower(5,caster)-defaultFive*1.75)<0.001);
        } finally { ThundercrashConfig.BASE_POWER.set(oldBase); ThundercrashConfig.POWER_PER_LEVEL.set(oldPerLevel); }
        AABB body=new AABB(0,0,0,0.6,1.8,0.6);
        check("thin_wall_sweep",Math.abs(ThundercrashCollision.sweep(body,new Vec3(4,0,0),new AABB(2,0,0,2.05,2,1))-0.35)<0.0001);
        check("separating_floor",!Double.isFinite(ThundercrashCollision.sweep(body,new Vec3(0,0.35,0),new AABB(-1,-1,-1,1,0,1))));
        check("descending_floor",ThundercrashCollision.sweep(body,new Vec3(0,-1,0),new AABB(-1,-1,-1,1,0,1))==0);
        check("shoulder_collision",Double.isFinite(ThundercrashCollision.sweep(body,new Vec3(4,0,0),new AABB(2,1.7,0.5,3,2,1))));
        Vec3 v=new Vec3(0,0,0.85);
        for(int i=0;i<30;i++)v=ThundercrashMovement.step(v,new Vec3(1,0,0),0.85,0.18,false,0.35);
        check("steering_turn",v.f_82479_>0.8 && Math.abs(v.f_82481_)<0.01 && v.m_82553_()<=0.85);
        check("finite_guard",ThundercrashMovement.step(new Vec3(Double.NaN,0,0),v,0.85,0.18,false,0.35).equals(Vec3.f_82478_));
        Vec3 defaultLaunch=ThundercrashMovement.step(Vec3.f_82478_,new Vec3(0,0,1),0.85,0.18,true,0.35);
        check("default_launch_unchanged",defaultLaunch.equals(new Vec3(0,0.35,0.125)));
        Vec3 fastLaunch=ThundercrashMovement.step(Vec3.f_82478_,new Vec3(0,0,1),0.2,0.18,true,1.5);
        check("configured_launch_speed_cap",fastLaunch.m_82553_()<=0.2+1e-10 && fastLaunch.f_82480_>0);
        check("nonfinite_launch_lift_stops",ThundercrashMovement.step(Vec3.f_82478_,v,0.85,0.18,true,Double.NaN).equals(Vec3.f_82478_));
        check("invalid_speed_stops",ThundercrashMovement.step(v,v,-1,0.18,false,0.35).equals(Vec3.f_82478_));
        check("zero_look_stays_finite",ThundercrashMovement.finite(ThundercrashMovement.step(v,Vec3.f_82478_,0.85,0.18,false,0.35)));
        Vec3 reversal=ThundercrashMovement.step(new Vec3(0,0,0.85),new Vec3(0,0,-1),0.85,0.18,false,0.35);
        check("reversal_preserves_initial_momentum",reversal.f_82481_>0 && reversal.f_82481_<0.85);
        ThundercrashPrediction prediction=new ThundercrashPrediction();
        for(int i=1;i<=4;i++) { check("prediction_step_"+i,prediction.beginStep()); prediction.sentInput(i,0,0); }
        check("prediction_replay_bounded",prediction.acknowledge(0).size()==4 && !prediction.beginStep());
        prediction.sentInput(100,90,0);
        check("frozen_input_does_not_replay",prediction.acknowledge(0).size()==4 && !prediction.beginStep());
        check("ack_releases_only_consumed_steps",prediction.acknowledge(2).size()==2);
        for(int i=5;i<=6;i++) { check("prediction_after_ack_"+i,prediction.beginStep()); prediction.sentInput(i,90,0); }
        check("repeated_snapshot_cannot_extend_prediction",prediction.acknowledge(2).size()==4 && !prediction.beginStep());
        prediction.reset(); check("correction_resets_prediction",prediction.acknowledge(0).isEmpty() && prediction.beginStep());
        // Clear a loaded test corridor far above terrain, without touching the existing test world.
        for(int x=-3;x<=3;x++)for(int y=78;y<=90;y++)for(int z=-3;z<=85;z++) place(level,new BlockPos(x,y,z),false);
        caster.m_6034_(0,80,-3);
        collisionCases(level);
        lifecycleCases(level);
        packetCases();
        forgeCases(level,caster,spell);
        caster.m_6034_(0,80,0); caster.m_146922_(0); caster.m_146926_(0); caster.m_20242_(false);
        ThundercrashManager.charge(caster,5);
        spell.onCast(level,5,caster,CastSource.COMMAND,MagicData.getPlayerMagicData(caster));
        var capturedLevel=state(caster).getClass().getDeclaredField("spellLevel"); capturedLevel.setAccessible(true);
        check("spell_level_captured_at_activation",capturedLevel.getInt(state(caster))==5);
        ThundercrashManager.abort(caster);
        check("start_flight",ThundercrashManager.start(caster,80));
        var sessionField=state(caster).getClass().getDeclaredField("session"); sessionField.setAccessible(true);
        var sequenceField=state(caster).getClass().getDeclaredField("inputSequence"); sequenceField.setAccessible(true);
        long session=sessionField.getLong(state(caster));
        ThundercrashManager.input(caster,new ThundercrashInputPacket(session,1,Float.NaN,0));
        check("reject_nan_input",sequenceField.getInt(state(caster))==-1);
        ThundercrashManager.input(caster,new ThundercrashInputPacket(session+1,1,90,0));
        check("reject_wrong_session",sequenceField.getInt(state(caster))==-1);
        ThundercrashManager.input(caster,new ThundercrashInputPacket(session,1,0,0));
        steps(20);
        check("horizontal_propulsion",caster.m_20189_()>8 && caster.m_20068_());
        ThundercrashManager.input(caster,new ThundercrashInputPacket(session,2,-90,0)); steps(15);
        check("live_camera_steering",caster.m_20185_()>5);
        ThundercrashManager.abort(caster);
        check("abort_cleanup",!ThundercrashManager.owns(caster) && !caster.m_20068_());
        LivingFallEvent fall=new LivingFallEvent(caster,30,1); ThundercrashManager.fall(fall);
        check("landing_protection",fall.isCanceled());
        LivingFallEvent second=new LivingFallEvent(caster,30,1); ThundercrashManager.fall(second);
        check("one_landing_only",!second.isCanceled());
        caster.m_6034_(0,80,0); caster.m_146922_(0); ThundercrashManager.start(caster,80); steps(90);
        check("timeout_cleanup",!ThundercrashManager.owns(caster) && !caster.m_20068_());
        // A wall is never destroyed, and detonation occurs before the player's body crosses it.
        for(int x=-2;x<=2;x++)for(int y=79;y<=85;y++)place(level,new BlockPos(x,y,7),true);
        caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80); steps(25);
        check("wall_collision",!ThundercrashManager.owns(caster) && caster.m_20189_()<6.71);
        check("terrain_preserved",!level.m_8055_(new BlockPos(0,81,7)).m_60795_());
        check("idempotent_abort",!ThundercrashManager.owns(caster)); ThundercrashManager.abort(caster);
        for(int x=-2;x<=2;x++)for(int y=79;y<=85;y++)place(level,new BlockPos(x,y,7),false);
        LivingEntity target=EntityType.f_20503_.m_20615_(level);
        target.m_6034_(0,81.4,6); target.m_21051_(Attributes.f_22276_).m_22100_(500); target.m_21153_(500); level.m_7967_(target);
        caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80); steps(25);
        check("entity_collision_damage",!ThundercrashManager.owns(caster) && target.m_21223_()<500);
        target.m_146870_();
        caster.m_20242_(true); caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80); ThundercrashManager.abort(caster);
        check("restore_existing_gravity",caster.m_20068_()); caster.m_20242_(false);
        caster.getPersistentData().m_128379_("irons_ultimate_explosion.thundercrash.originalGravity",false); caster.m_20242_(true);
        ThundercrashManager.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(caster));
        check("crash_recovery",!caster.m_20068_());
        double oldEdge=ThundercrashConfig.EDGE_DAMAGE.get();
        try {
            ThundercrashConfig.EDGE_DAMAGE.set(0.6); caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80);
            var edge=state(caster).getClass().getDeclaredField("edgeDamage"); edge.setAccessible(true);
            ThundercrashConfig.EDGE_DAMAGE.set(0.1);
            check("falloff_captured_at_activation",Math.abs(edge.getDouble(state(caster))-0.6)<1e-9);
            ThundercrashManager.abort(caster);
        } finally { ThundercrashConfig.EDGE_DAMAGE.set(oldEdge); }
        var lightning=caster.m_21051_(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get());
        double oldLightning=lightning.m_22115_();
        float originalPower=spell.getSpellPower(1,caster);
        try {
            lightning.m_22100_(oldLightning*2);
            float boostedPower=spell.getSpellPower(1,caster);
            check("lightning_power_scaling",boostedPower>originalPower);
            caster.m_6034_(0,80,0); ThundercrashManager.start(caster,boostedPower);
            var damage=state(caster).getClass().getDeclaredField("damage"); damage.setAccessible(true);
            check("lightning_power_applied_once",Math.abs(damage.getDouble(state(caster))-boostedPower*ThundercrashConfig.DAMAGE.get())<1e-5);
            lightning.m_22100_(oldLightning);
            check("damage_power_captured_at_activation",Math.abs(damage.getDouble(state(caster))-boostedPower*ThundercrashConfig.DAMAGE.get())<1e-5);
            ThundercrashManager.abort(caster);
        } finally { lightning.m_22100_(oldLightning); }
        var mana=MagicData.getPlayerMagicData(caster);
        caster.m_21051_(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()).m_22100_(1000);
        mana.setMana(500); mana.getPlayerCooldowns().clearCooldowns();
        spell.onServerPreCast(level,1,caster,mana); spell.castSpell(level,1,caster,CastSource.SPELLBOOK,true);
        spell.onServerCastComplete(level,1,caster,mana,false);
        check("native_mana_deducted_once",Math.abs(mana.getMana()-(500-spell.getManaCost(1)))<0.01 && ThundercrashManager.owns(caster));
        check("native_cooldown_applied",mana.getPlayerCooldowns().isOnCooldown(spell));
        ThundercrashManager.abort(caster); mana.getPlayerCooldowns().clearCooldowns(); mana.setMana(0);
        check("zero_mana_rejected",spell.canBeCastedBy(1,CastSource.SPELLBOOK,mana,caster).type==CastResult.Type.FAILURE);
        caster.m_6034_(0,80,0); ThundercrashManager.charge(caster);
        spell.onServerCastComplete(level,1,caster,MagicData.getPlayerMagicData(caster),true);
        check("interrupted_charge",state(caster)==null && !caster.m_20068_());
        ThundercrashManager.charge(caster); spell.onCast(level,1,caster,CastSource.COMMAND,MagicData.getPlayerMagicData(caster));
        spell.onServerCastComplete(level,1,caster,MagicData.getPlayerMagicData(caster),false);
        check("successful_complete_keeps_flight",ThundercrashManager.owns(caster));
        spell.onServerCastComplete(level,1,caster,MagicData.getPlayerMagicData(caster),true);
        check("interruption_only_removes_pending_charge",ThundercrashManager.owns(caster));
        ThundercrashManager.externalTeleport(caster); check("external_teleport_cleanup",!ThundercrashManager.owns(caster));
        ThundercrashManager.start(caster,80); ThundercrashManager.abort(caster); steps(101);
        LivingFallEvent expired=new LivingFallEvent(caster,30,1); ThundercrashManager.fall(expired);
        check("landing_expiry",!expired.isCanceled());
        check("invalid_bounds",!ThundercrashCollision.safe(level,new AABB(0,Double.NaN,0,1,2,1)));
        check("unloaded_boundary",!ThundercrashCollision.safe(level,new AABB(100000,80,100000,100001,82,100001)));
        check("unsafe_height",!ThundercrashCollision.safe(level,new AABB(0,level.m_151558_(),0,1,level.m_151558_()+2,1)));
        ServerPlayer victim=player(level,"ThunderVictim",0,6); victim.m_6034_(0,81.4,6);
        boolean oldPvp=ThundercrashConfig.PVP.get(), serverPvp=level.m_7654_().m_129799_();
        try {
            check("caster_excluded",!ThundercrashManager.canHit(caster,caster));
            ThundercrashConfig.PVP.set(false); check("pvp_config_filter",!ThundercrashManager.canHit(caster,victim));
            ThundercrashConfig.PVP.set(true); level.m_7654_().m_129997_(false);
            check("server_pvp_filter",!ThundercrashManager.canHit(caster,victim)); level.m_7654_().m_129997_(true);
            check("pvp_enabled_filter",ThundercrashManager.canHit(caster,victim));
            caster.m_6034_(0,80,0); caster.m_146922_(0); ThundercrashManager.start(caster,80); steps(25);
            check("pvp_collision_damage",victim.m_21223_()<500 && !ThundercrashManager.owns(caster));
            victim.m_21153_(500); victim.f_19802_=0; victim.m_6034_(0,81.4,6);
            ThundercrashConfig.PVP.set(false); caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80); steps(90);
            check("pvp_disabled_no_damage",victim.m_21223_()==500);
        } finally { ThundercrashConfig.PVP.set(oldPvp); level.m_7654_().m_129997_(serverPvp); victim.m_146870_(); }
        LivingEntity canceledTarget=EntityType.f_20503_.m_20615_(level);
        canceledTarget.m_6034_(0,81.4,6); canceledTarget.m_21051_(Attributes.f_22276_).m_22100_(500); canceledTarget.m_21153_(500); level.m_7967_(canceledTarget);
        int[] damageEvents={0};
        java.util.function.Consumer<io.redspace.ironsspellbooks.api.events.SpellDamageEvent> cancel=e -> { if(e.getEntity()==canceledTarget) { damageEvents[0]++; e.setCanceled(true); } };
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try {
            caster.m_6034_(0,80,0); ThundercrashManager.start(caster,80); steps(25);
            check("cancellable_spell_damage",canceledTarget.m_21223_()==500 && !ThundercrashManager.owns(caster));
            check("canceled_damage_has_no_knockback",canceledTarget.m_20184_().m_82556_()<1e-10);
            check("single_damage_pass",damageEvents[0]==1);
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); canceledTarget.m_146870_(); }
        damageRules(level,caster);
        boolean guided=ThundercrashConfig.GUIDED.get();
        try {
            ThundercrashConfig.GUIDED.set(false); caster.m_6034_(0,80,0); caster.m_146922_(0); ThundercrashManager.start(caster,80);
            Object fixedState=state(caster); long fixedSession=sessionField.getLong(fixedState);
            ThundercrashManager.input(caster,new ThundercrashInputPacket(fixedSession,1,-90,0)); steps(70);
            var distanceField=fixedState.getClass().getDeclaredField("travelled"); distanceField.setAccessible(true);
            double distance=distanceField.getDouble(fixedState);
            System.out.println("THUNDER_FALLBACK distance="+distance+" position="+caster.m_20182_());
            check("directional_fallback",!ThundercrashManager.owns(caster) && Math.abs(caster.m_20185_())<0.1 && Math.abs(distance-40)<1e-6 && caster.m_20189_()>37);
        } finally { ThundercrashConfig.GUIDED.set(guided); }
        for(int size:new int[]{1,4,8}) {
            List<ServerPlayer> group=new ArrayList<>();
            try {
                for(int i=0;i<size;i++) { ServerPlayer p=player(level,"Thunder"+i,4+i*4,0); group.add(p); ThundercrashManager.start(p,80); }
                long begin=System.nanoTime(); steps(30); double millis=(System.nanoTime()-begin)/1_000_000.0/30;
                check(size+"_simultaneous_casters",group.stream().allMatch(p -> ThundercrashManager.owns(p) && p.m_20189_()>15));
                System.out.println("THUNDER_PERFORMANCE casters="+size+" mean_controller_tick_ms="+millis+" samples=30 synthetic_players=true");
            } finally { for(ServerPlayer p:group) { ThundercrashManager.abort(p); p.m_146870_(); } }
        }
        System.out.println("THUNDER_TEST_COMPLETE passed="+passed);
        } finally { ThundercrashManager.abort(caster); caster.m_146870_(); }
    }
    private void collisionCases(ServerLevel level) throws Exception {
        ServerPlayer p=player(level,"ThunderGeometry",0.5,0.5);
        try {
            p.m_6034_(0.5,80,0.5);
            level.m_7731_(new BlockPos(0,80,2),Blocks.f_50404_.m_49966_(),3);
            var slab=ThundercrashCollision.find(level,p,new Vec3(0,0,3),false);
            check("native_slab_shape_contact",slab!=null && slab.fraction()<0.5 && slab.point().f_82480_<=80.5+1e-6);
            place(level,new BlockPos(0,80,2),false);
            level.m_7731_(new BlockPos(0,80,2),Blocks.f_50132_.m_49966_(),3);
            var fence=ThundercrashCollision.find(level,p,new Vec3(0,0,3),false);
            check("native_fence_shape_contact",fence!=null && fence.fraction()<1);
            place(level,new BlockPos(0,80,2),false);
            place(level,new BlockPos(0,82,0),true);
            var ceiling=ThundercrashCollision.find(level,p,new Vec3(0,1,0),false);
            check("ceiling_contact",ceiling!=null && Math.abs(ceiling.fraction()-0.2)<0.001);
            ThundercrashManager.start(p,80); steps(1);
            check("obstructed_launch_cleans_up",!ThundercrashManager.owns(p) && p.m_20186_()<80.21 && !p.m_20068_());
            place(level,new BlockPos(0,82,0),false);
            p.m_6034_(0.5,80,0.5); place(level,new BlockPos(1,80,1),true);
            var corner=ThundercrashCollision.find(level,p,new Vec3(2,0,2),false);
            check("diagonal_corner_contact",corner!=null && Math.abs(corner.fraction()-0.1)<0.001);
            place(level,new BlockPos(1,80,1),false);
            place(level,new BlockPos(0,80,2),true);
            LivingEntity tied=EntityType.f_20503_.m_20615_(level);
            try {
                tied.m_6034_(0.5,80,2+tied.m_20205_()/2.0); level.m_7967_(tied);
                var tie=ThundercrashCollision.find(level,p,new Vec3(0,0,3),true);
                check("block_wins_entity_tie",tie!=null && tie.target()==null);
            } finally { tied.m_146870_(); }
        } finally {
            for(BlockPos pos:new BlockPos[]{new BlockPos(0,80,2),new BlockPos(0,82,0),new BlockPos(1,80,1)})place(level,pos,false);
            ThundercrashManager.abort(p); p.m_146870_();
        }
    }
    private void lifecycleCases(ServerLevel level) throws Exception {
        ServerPlayer p=player(level,"ThunderLifecycle",0.5,0.5);
        LivingEntity mount=EntityType.f_20503_.m_20615_(level);
        var fluidUpdate=Entity.class.getDeclaredMethod("m_20073_"); fluidUpdate.setAccessible(true);
        var flag=Entity.class.getDeclaredMethod("m_20115_",int.class,boolean.class); flag.setAccessible(true);
        final String marker="irons_ultimate_explosion.thundercrash.originalGravity";
        try {
            // Vanilla suppresses lava occupancy on an entity's first tick.
            p.m_6075_();
            mount.m_6034_(1,80,0.5); level.m_7967_(mount); p.m_7998_(mount,true);
            check("mounted_cast_rejected",p.m_20159_() && !ThundercrashManager.eligible(p));
            p.m_8127_(); p.m_6034_(0.5,80,0.5); ThundercrashManager.start(p,80); p.m_7998_(mount,true); steps(1);
            check("mounting_aborts_active_flight",!ThundercrashManager.owns(p) && !p.m_20068_());
            p.m_8127_(); mount.m_146870_(); p.m_6034_(0.5,80,0.5);
            p.m_36320_(); check("elytra_cast_rejected",p.m_21255_() && !ThundercrashManager.eligible(p));
            flag.invoke(p,7,false); ThundercrashManager.start(p,80); p.m_36320_(); steps(1);
            check("elytra_activation_aborts_flight",!ThundercrashManager.owns(p) && !p.m_20068_()); flag.invoke(p,7,false);
            p.m_143403_(net.minecraft.world.level.GameType.SPECTATOR);
            check("spectator_cast_rejected",!ThundercrashManager.eligible(p));
            p.m_143403_(net.minecraft.world.level.GameType.CREATIVE);
            boolean flying=p.m_150110_().f_35935_, mayFly=p.m_150110_().f_35936_;
            p.m_6034_(0.5,80,0.5); ThundercrashManager.start(p,80); steps(8); ThundercrashManager.abort(p);
            check("creative_permissions_unchanged",p.m_150110_().f_35935_==flying && p.m_150110_().f_35936_==mayFly && !p.m_20068_());
            p.m_143403_(net.minecraft.world.level.GameType.SURVIVAL);
            for(boolean lava:new boolean[]{false,true}) {
                p.m_6034_(0.5,80,0.5); fluidUpdate.invoke(p); ThundercrashManager.start(p,80);
                level.m_7731_(new BlockPos(0,80,0),(lava?Blocks.f_49991_:Blocks.f_49990_).m_49966_(),3); fluidUpdate.invoke(p);
                check((lava?"lava":"water")+"_entry_detected",lava?p.m_20077_():p.m_20069_()); steps(1);
                check((lava?"lava":"water")+"_entry_aborts",!ThundercrashManager.owns(p) && !p.m_20068_());
                if(lava) { float health=p.m_21223_(); p.m_6075_(); check("lava_keeps_ordinary_damage",p.m_21223_()<health); }
                place(level,new BlockPos(0,80,0),false); fluidUpdate.invoke(p);
            }
            p.m_6034_(0.5,80,0.5); ThundercrashManager.start(p,80);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            check("logout_event_cleanup",state(p)==null && !p.m_20068_() && !p.getPersistentData().m_128441_(marker));
            ThundercrashManager.start(p,80);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,level.m_46472_(),net.minecraft.world.level.Level.f_46429_));
            check("dimension_event_cleanup",state(p)==null && !p.m_20068_() && !p.getPersistentData().m_128441_(marker));
            ThundercrashManager.start(p,80);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDeathEvent(p,p.m_269291_().m_268989_()));
            check("death_event_cleanup",state(p)==null && !p.m_20068_() && !p.getPersistentData().m_128441_(marker));
            p.m_5802_(new BlockPos(0,80,0)); check("sleeping_cast_rejected",p.m_5803_() && !ThundercrashManager.eligible(p)); p.m_5796_();
        } finally {
            place(level,new BlockPos(0,80,0),false); flag.invoke(p,7,false); ThundercrashManager.abort(p);
            p.m_8127_(); p.m_146870_(); mount.m_146870_();
        }
    }
    private void packetCases() throws Exception {
        var encode=ThundercrashInputPacket.class.getDeclaredMethod("encode",ThundercrashInputPacket.class,net.minecraft.network.FriendlyByteBuf.class);
        var decode=ThundercrashInputPacket.class.getDeclaredMethod("decode",net.minecraft.network.FriendlyByteBuf.class);
        encode.setAccessible(true); decode.setAccessible(true);
        var b=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var input=new ThundercrashInputPacket(123,4,90,-45); encode.invoke(null,input,b);
            check("input_payload_is_twenty_bytes",b.readableBytes()==20);
            check("input_codec_roundtrip",input.equals(decode.invoke(null,b)));
            b.clear(); encode.invoke(null,input,b); b.writeByte(0);
            boolean rejected=false;
            try { decode.invoke(null,b); } catch(java.lang.reflect.InvocationTargetException e) { rejected=e.getCause() instanceof IllegalArgumentException; }
            check("oversized_input_rejected",rejected);
            b.clear(); b.writeLong(123);
            rejected=false;
            try { decode.invoke(null,b); } catch(java.lang.reflect.InvocationTargetException e) { rejected=e.getCause() instanceof IllegalArgumentException; }
            check("truncated_input_rejected",rejected);
        } finally { b.release(); }
    }
    private void forgeCases(ServerLevel level, ServerPlayer player, AbstractSpell spell) throws Exception {
        var lightning=SchoolRegistry.LIGHTNING.get();
        check("native_lightning_school_listing",io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpellsForSchool(lightning).contains(spell));
        var focusItem=net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(lightning.getFocus()).iterator().next();
        var focus=new net.minecraft.world.item.ItemStack(focusItem);
        var forgePos=new BlockPos(3,80,-3);
        level.m_7731_(forgePos,io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get().m_49966_(),3);
        var tile=(io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile)level.m_7702_(forgePos);
        try {
        var menu=(io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu)tile.m_7208_(7,player.m_150109_(),player);
        var ink=new net.minecraft.world.item.ItemStack(io.redspace.ironsspellbooks.item.InkItem.getInkForRarity(SpellRarity.LEGENDARY));
        var inkId=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ink.m_41720_());
        var paper=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.f_42516_);
        check("native_forge_accepts_ingredients",menu.getInkSlot().m_5857_(ink) && menu.getBlankScrollSlot().m_5857_(paper) && menu.getFocusSlot().m_5857_(focus));
        tile.getItemHandler().setStackInSlot(0,ink); tile.getItemHandler().setStackInSlot(1,paper); tile.getItemHandler().setStackInSlot(2,focus);
        menu.setRecipeSpell(spell);
        var scroll=menu.getResultSlot().m_7993_().m_41777_();
        var container=ISpellContainer.get(scroll);
        check("native_forge_produces_thundercrash_scroll",!scroll.m_41619_() && container.getSpellAtIndex(0).getSpell()==spell
                && container.getSpellAtIndex(0).getLevel()==spell.getMinLevelForRarity(SpellRarity.LEGENDARY));
        menu.getResultSlot().m_6201_(1); menu.getResultSlot().m_142406_(player,scroll);
        check("native_forge_consumes_one_of_each",tile.getStackInSlot(0).m_41619_() && tile.getStackInSlot(1).m_41619_() && tile.getStackInSlot(2).m_41619_());
        System.out.println("THUNDER_FORGE ink="+inkId
                +" focus="+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(focusItem)+" level="+container.getSpellAtIndex(0).getLevel());
        inscriptionCases(level,player,spell,scroll);
        } finally { place(level,forgePos,false); }
    }
    private void inscriptionCases(ServerLevel level, ServerPlayer player, AbstractSpell spell, net.minecraft.world.item.ItemStack scroll) {
        var tablePos=new BlockPos(3,80,-2);
        level.m_7731_(tablePos,io.redspace.ironsspellbooks.registries.BlockRegistry.INSCRIPTION_TABLE_BLOCK.get().m_49966_(),3);
        try {
            var menu=new io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu(8,player.m_150109_(),
                    net.minecraft.world.inventory.ContainerLevelAccess.m_39289_(level,tablePos));
            var book=new net.minecraft.world.item.ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.IRON_SPELL_BOOK.get());
            check("native_inscription_accepts_forged_scroll",menu.getSpellBookSlot().m_5857_(book) && menu.getScrollSlot().m_5857_(scroll));
            int forgedLevel=ISpellContainer.get(scroll).getSpellAtIndex(0).getLevel();
            menu.getSpellBookSlot().m_5852_(book); menu.getScrollSlot().m_5852_(scroll);
            menu.m_6366_(player,0); menu.m_6366_(player,-1);
            var inscribed=ISpellContainer.get(menu.getSpellBookSlot().m_7993_());
            check("native_inscription_preserves_spell_and_level",inscribed.getSpellAtIndex(0).getSpell()==spell && inscribed.getSpellAtIndex(0).getLevel()==forgedLevel);
            check("native_inscription_consumes_scroll",menu.getScrollSlot().m_7993_().m_41619_());
            check("native_inscription_preserves_spellbook",menu.getSpellBookSlot().m_7993_().m_41720_()==io.redspace.ironsspellbooks.registries.ItemRegistry.IRON_SPELL_BOOK.get()
                    && inscribed.getActiveSpellCount()==1 && inscribed.getMaxSpellCount()>1);
            System.out.println("THUNDER_INSCRIPTION book=irons_spellbooks:iron_spell_book level="+forgedLevel+" consumed_scroll=true");
        } finally { place(level,tablePos,false); }
    }
    private void damageRules(ServerLevel level, ServerPlayer caster) throws Exception {
        boolean configPvp=ThundercrashConfig.PVP.get(), serverPvp=level.m_7654_().m_129799_();
        List<Entity> entities=new ArrayList<>();
        net.minecraft.world.scores.PlayerTeam team=null;
        try {
            ThundercrashConfig.PVP.set(true); level.m_7654_().m_129997_(true);
            ServerPlayer mate=player(level,"ThunderMate",0,0); entities.add(mate);
            team=level.m_6188_().m_83492_("thunderTest"); team.m_83355_(false);
            level.m_6188_().m_6546_(caster.m_6302_(),team); level.m_6188_().m_6546_(mate.m_6302_(),team);
            check("team_friendly_fire_filter",!ThundercrashManager.canHit(caster,mate));
            caster.m_6034_(0,80,-3); discharge(caster,new Vec3(0,80.9,0));
            check("teammate_spared_by_discharge",mate.m_21223_()==500);
            team.m_83355_(true); check("team_friendly_fire_enabled",ThundercrashManager.canHit(caster,mate));
            level.m_6188_().m_83475_(team); team=null; mate.m_146870_();

            var summon=new io.redspace.ironsspellbooks.entity.mobs.SummonedZombie(level,caster,false);
            summon.m_6034_(0,80,0); level.m_7967_(summon); entities.add(summon);
            check("own_iron_summon_filter",!ThundercrashManager.canHit(caster,summon));
            float summonHealth=summon.m_21223_(); discharge(caster,new Vec3(0,80.9,0));
            check("own_iron_summon_spared",summon.m_21223_()==summonHealth); summon.m_146870_();

            ServerPlayer normal=player(level,"ThunderNormal",-2,0), resistant=player(level,"ThunderResist",2,0);
            entities.add(normal); entities.add(resistant);
            resistant.m_21051_(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_MAGIC_RESIST.get()).m_22100_(2);
            normal.m_21051_(Attributes.f_22278_).m_22100_(0); resistant.m_21051_(Attributes.f_22278_).m_22100_(1);
            normal.m_20256_(Vec3.f_82478_); resistant.m_20256_(Vec3.f_82478_);
            discharge(caster,new Vec3(0,80.9,0));
            double normalLoss=500-normal.m_21223_(), resistLoss=500-resistant.m_21223_();
            check("lightning_resistance_reduces_damage",normalLoss>0 && resistLoss>0 && resistLoss<normalLoss);
            check("radial_knockback_outward",normal.m_20184_().f_82479_<-0.1);
            check("knockback_resistance_respected",resistant.m_20184_().m_82556_()<1e-10);
            normal.m_146870_(); resistant.m_146870_();

            ServerPlayer exposed=player(level,"ThunderOpen",-2,0), occluded=player(level,"ThunderHidden",2,0), outside=player(level,"ThunderOutside",7,0);
            entities.add(exposed); entities.add(occluded); entities.add(outside);
            for(int y=79;y<=84;y++)for(int z=-1;z<=1;z++)place(level,new BlockPos(1,y,z),true);
            float casterHealth=caster.m_21223_();
            discharge(caster,new Vec3(0,80.9,0));
            check("visible_target_damaged",exposed.m_21223_()<500);
            check("block_line_of_sight_occludes_damage",occluded.m_21223_()==500);
            check("spherical_radius_excludes_outside",outside.m_21223_()==500);
            check("caster_spared_by_discharge",caster.m_21223_()==casterHealth);
        } finally {
            if(team!=null) level.m_6188_().m_83475_(team);
            for(Entity e:entities)e.m_146870_();
            for(int y=79;y<=84;y++)for(int z=-1;z<=1;z++)place(level,new BlockPos(1,y,z),false);
            ThundercrashManager.abort(caster); ThundercrashConfig.PVP.set(configPvp); level.m_7654_().m_129997_(serverPvp);
        }
    }
}

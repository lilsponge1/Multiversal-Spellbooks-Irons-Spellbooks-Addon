package local.omegarushtest;

import local.omegarush.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit operator command; only for the isolated, disposable validation world. */
@Mod("omega_rush_test")
public final class OmegaHarness {
    private int passed;
    private final List<Entity> spawned=new ArrayList<>();
    private ServerPlayer actor;
    private int poseTicks,posePulse;
    public OmegaHarness() { MinecraftForge.EVENT_BUS.addListener(this::commands); MinecraftForge.EVENT_BUS.addListener(this::poseTick); new OmegaNetworkHarness();new OmegaFormHarness(); }
    private void poseTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||poseTicks--<=0||actor==null||!OmegaManager.owns(actor)||poseTicks%3!=0) return;
        try {
            Object state=state(actor); long session=(long)field(state,"session");
            var burst=OmegaNetwork.class.getDeclaredMethod("burst",ServerLevel.class,OmegaBurstPacket.class); burst.setAccessible(true);
            Vec3 point=new Vec3(0.5,-57.1,28.5-(posePulse%3)*2);
            burst.invoke(null,actor.m_284548_(),new OmegaBurstPacket(actor.m_20148_(),actor.m_9236_().m_46472_().m_135782_().toString(),session,posePulse++,point,new Vec3(0,0,1),3,false));
        } catch(Exception error) { poseTicks=0; error.printStackTrace(); }
    }
    private void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("omega_pose").requires(s->s.m_6761_(4)).executes(c->{
            try {
                e.getDispatcher().execute("omega_actor",c.getSource());
                Object s=state(actor); Vec3 position=new Vec3(0.5,-58,32.5); actor.m_6034_(position.f_82479_,position.f_82480_,position.f_82481_);
                for(String name:List.of("position","previous","velocity","speed","duration")) {
                    var f=s.getClass().getDeclaredField(name); f.setAccessible(true);
                    switch(name) { case "speed"->f.setDouble(s,0); case "duration"->f.setInt(s,800); case "velocity"->f.set(s,Vec3.f_82478_); default->f.set(s,position); }
                }
                poseTicks=760; posePulse=0; System.out.println("OMEGA_POSE_FIXTURE render_only_stationary_preview=true"); return 1;
            } catch(Exception error) { error.printStackTrace(); return 0; }
        }));
        e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("omega_actor").requires(s->s.m_6761_(4)).executes(c->{
            try {
                var level=c.getSource().m_81372_();
                if(actor!=null) { OmegaManager.abort(actor); actor.m_146870_(); }
                actor=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),"OmegaActor"));
                actor.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"ActorConn")).f_8906_;
                actor.m_6034_(0.5,-60,0.5); actor.m_146922_(0); actor.m_146926_(0);
                var info=new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(java.util.EnumSet.of(
                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),List.of(actor));
                for(ServerPlayer viewer:level.m_7654_().m_6846_().m_11314_()) viewer.f_8906_.m_9829_(info);
                level.m_7967_(actor); OmegaManager.start(actor,1,12);
                System.out.println("OMEGA_ACTOR_START uuid="+actor.m_20148_()); return 1;
            } catch(Exception error) { error.printStackTrace(); return 0; }
        }));
        e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("omega_test").requires(s->s.m_6761_(4)).executes(c->{
            try { run(c.getSource().m_81372_()); return 1; } catch(Throwable t) { System.out.println("OMEGA_TEST_FAIL "+t); t.printStackTrace(); return 0; }
        }));
    }
    private void check(String name,boolean ok) { if(!ok) throw new AssertionError(name); passed++; System.out.println("OMEGA_TEST_PASS "+name); }
    private void steps(int count) {
        for(int i=0;i<count;i++) {
            OmegaManager.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END,()->true,net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()));
            for(Entity e:spawned) if(e instanceof LivingEntity&&!(e instanceof ServerPlayer)&&e.m_6084_()) e.m_8119_();
        }
    }
    private Object field(Object object,String name) throws Exception { var f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    private Map<?,?> map(String name) throws Exception { var f=OmegaManager.class.getDeclaredField(name); f.setAccessible(true); return (Map<?,?>)f.get(null); }
    private Object state(ServerPlayer p) throws Exception { return map("STATES").get(p.m_20148_()); }
    private long session(ServerPlayer p) throws Exception { return (long)field(state(p),"session"); }
    private ServerPlayer player(ServerLevel level,String name,double x,double z) throws Exception {
        ServerPlayer p=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),name));
        p.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),name+"Conn")).f_8906_;
        p.m_6034_(x,120,z); p.m_146922_(0); p.m_146926_(0);
        var spawn=ServerPlayer.class.getDeclaredField("f_8921_"); spawn.setAccessible(true); spawn.setInt(p,0);
        p.m_21051_(Attributes.f_22276_).m_22100_(500); p.m_21153_(500); level.m_7967_(p); spawned.add(p); return p;
    }
    private LivingEntity target(ServerLevel level,double x,double z) {
        Mob mob=(Mob)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft","cow")).m_20615_(level); mob.m_6034_(x,120,z); mob.m_20242_(true);
        mob.m_21557_(true);
        mob.m_21051_(Attributes.f_22276_).m_22100_(500); mob.m_21153_(500); level.m_7967_(mob); spawned.add(mob); return mob;
    }
    private void place(ServerLevel l,int x,int y,int z,boolean solid) { l.m_7731_(new BlockPos(x,y,z),(solid?Blocks.f_50069_:Blocks.f_50016_).m_49966_(),3); }
    private void reset(ServerPlayer p) { OmegaManager.abort(p); p.m_6034_(0,120,0); p.m_146922_(0); p.m_146926_(0); p.m_20242_(false); }
    private void run(ServerLevel level) throws Exception {
        passed=0;
        boolean requireForm=OmegaConfig.REQUIRE_FORM.get();OmegaConfig.REQUIRE_FORM.set(false);
        for(int x=-4;x<=9;x++) for(int z=-4;z<=9;z++) level.m_6325_(x,z);
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(level.m_7654_().m_6846_(),null));
        ServerPlayer p=player(level,"OmegaTest",0,0);
        try {
            AbstractSpell spell=ModSpells.OMEGA_RUSH.get();
            check("nature_school",spell.getSchoolType()==SchoolRegistry.NATURE.get());
            check("custom_sounds_registered",ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(OmegaMod.ID,"car_drive"))==OmegaSounds.DRIVE.get()
                &&ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(OmegaMod.ID,"bomb"))==OmegaSounds.BOMB.get());
            check("legendary_five_levels",spell.getMinRarity()==SpellRarity.LEGENDARY.getValue()&&spell.getMaxLevel()==5);
            check("defaults",spell.getManaCost(1)==200&&spell.getSpellCooldown()==2400&&OmegaConfig.DURATION.get()==140&&OmegaConfig.SPEED.get()==0.8);
            check("charge_base_ticks",spell.getCastTime(1)==16);
            check("damage_progression",Math.abs(spell.getSpellPower(5,p)/spell.getSpellPower(1,p)-2)<0.001);
            mathCases(); resourceCases(); forgeCases(level,p,spell); movementCases(level,p); lifecycleCases(level,p); extraCases(level,p); damageCases(level,p); paidCast(level,p,spell); coexistenceCases(level,p);
            System.out.println("OMEGA_TEST_COMPLETE assertions="+passed);
        } finally {
            OmegaConfig.REQUIRE_FORM.set(requireForm);
            for(Entity entity:spawned) { if(entity instanceof ServerPlayer player) OmegaManager.abort(player); entity.m_146870_(); }
            spawned.clear();
            for(int x=-3;x<=3;x++) for(int y=119;y<=124;y++) place(level,x,y,6,false);
        }
    }
    private void mathCases() {
        Vec3 v=new Vec3(0,0,0.8);
        Vec3 turn=OmegaMovement.step(v,new Vec3(1,0,0),0.8,0.18);
        check("steering_exact_response",Math.abs(turn.f_82479_-0.144)<1e-10&&Math.abs(turn.f_82481_-0.656)<1e-10);
        check("turn_preserves_slowdown",turn.m_82553_()<0.8);
        check("yaw_wraparound",OmegaMovement.look(179,0).m_82554_(OmegaMovement.look(-181,0))<1e-15);
        check("upward_aim",OmegaMovement.look(0,-90).f_82480_>0.999);
        check("downward_aim",OmegaMovement.look(0,90).f_82480_<-0.999);
        for(int i=0;i<30;i++) v=OmegaMovement.step(v,new Vec3(1,0,0),0.8,0.18);
        check("turn_converges",v.f_82479_>0.79&&Math.abs(v.f_82481_)<0.003&&v.m_82553_()<=0.8);
        check("invalid_vector_stops",OmegaMovement.step(new Vec3(Double.NaN,0,0),v,0.8,0.18).equals(Vec3.f_82478_));
        check("speed_cap",OmegaMovement.step(new Vec3(9,0,0),new Vec3(1,0,0),0.8,0.18).m_82553_()<=0.8);
        var sampler=new OmegaPath();
        check("short_segment_no_node",sampler.sample(new Vec3(0,0,0),new Vec3(0,0,1.2),2).isEmpty());
        var corners=sampler.sample(new Vec3(0,0,1.2),new Vec3(1.2,0,1.2),2);
        check("arc_remainder_at_corner",corners.size()==1&&corners.get(0).m_82554_(new Vec3(0.8,0,1.2))<1e-10&&Math.abs(sampler.remainder()-0.4)<1e-9);
        var up=sampler.sample(new Vec3(1.2,0,1.2),new Vec3(1.2,2,1.2),2);
        check("vertical_trail_sampling",up.size()==1&&Math.abs(up.get(0).f_82480_-1.6)<1e-9);
        var cone=OmegaCone.points(Vec3.f_82478_,new Vec3(0,0,1),0,6);
        check("cone_all_bursts_behind_node",cone.size()==6&&cone.stream().allMatch(point->point.f_82481_<-1));
        check("cone_has_wide_lateral_spread",cone.stream().mapToDouble(point->point.f_82479_).max().orElse(0)-cone.stream().mapToDouble(point->point.f_82479_).min().orElse(0)>3);
        check("cone_rises_above_node",OmegaCone.points(Vec3.f_82478_,new Vec3(0,0,1),2,6).stream().anyMatch(point->point.f_82480_>2));
        check("vertical_cone_is_finite_and_trails",OmegaCone.points(Vec3.f_82478_,new Vec3(0,1,0),0,6).stream().allMatch(point->OmegaMovement.finite(point)&&point.f_82480_<-1));
        var prediction=new OmegaPrediction();
        for(int i=1;i<=4;i++) { check("prediction_step_"+i,prediction.beginStep()); prediction.sentInput(i,0,0); }
        check("prediction_freezes_at_four",!prediction.beginStep()); prediction.sentInput(10,0,0);
        check("frozen_input_not_replayed",prediction.acknowledge(0).size()==4);
        check("ack_releases_budget",prediction.acknowledge(2).size()==2&&prediction.beginStep());
        prediction.reset(); check("prediction_reset",prediction.acknowledge(0).isEmpty());
        AABB body=new AABB(0,0,0,0.6,1.8,0.6);
        check("thin_wall_sweep",Math.abs(OmegaCollision.sweep(body,new Vec3(4,0,0),new AABB(2,0,0,2.05,2,1))-0.35)<1e-10);
        check("separating_floor",!Double.isFinite(OmegaCollision.sweep(body,new Vec3(0,1,0),new AABB(-1,-1,-1,1,0,1))));
    }
    private void movementCases(ServerLevel level,ServerPlayer p) throws Exception {
        reset(p); check("launch",OmegaManager.start(p,1,12)); long sid=session(p);
        OmegaManager.input(p,new OmegaInputPacket(sid+1,1,90,0,false));
        check("wrong_session_rejected",(int)field(state(p),"inputSequence")==-1);
        OmegaManager.input(p,new OmegaInputPacket(sid,1,Float.NaN,0,false));
        check("nan_input_rejected",(int)field(state(p),"inputSequence")==-1);
        steps(10); check("cruise_16_blocks_per_second",Math.abs(p.m_20189_()-8)<0.01&&Math.abs(p.m_20186_()-120)<0.01);
        OmegaManager.input(p,new OmegaInputPacket(sid,1,-90,0,false)); steps(20);
        check("live_steering",p.m_20185_()>10);
        OmegaManager.input(p,new OmegaInputPacket(sid,1,0,0,false)); check("old_sequence_rejected",(int)field(state(p),"inputSequence")==1);
        OmegaManager.input(p,new OmegaInputPacket(sid,2,-90,-60,false)); steps(10); check("live_upward_steering",p.m_20186_()>123);
        OmegaManager.input(p,new OmegaInputPacket(sid,3,-90,60,false)); steps(20); check("live_downward_steering",p.m_20186_()<125);
        OmegaManager.input(p,new OmegaInputPacket(sid,4,0,0,true)); check("sneak_cancel",!OmegaManager.owns(p)&&!p.m_20068_());
        LivingFallEvent first=new LivingFallEvent(p,20,1); OmegaManager.fall(first); check("first_landing_protected",first.isCanceled());
        LivingFallEvent second=new LivingFallEvent(p,20,1); OmegaManager.fall(second); check("second_landing_unprotected",!second.isCanceled());
        reset(p); OmegaManager.start(p,1,12); steps(139); check("active_before_140",OmegaManager.owns(p)); steps(1);
        check("timeout_exactly_140",!OmegaManager.owns(p)&&Math.abs(p.m_20189_()-112)<0.01&&!p.m_20068_());
        reset(p); for(int x=-3;x<=3;x++) for(int y=119;y<=124;y++) place(level,x,y,6,true);
        OmegaManager.start(p,1,12); steps(15); check("wall_stops_body",!OmegaManager.owns(p)&&p.m_20189_()<5.71);
        check("wall_not_destroyed",!level.m_8055_(new BlockPos(0,121,6)).m_60795_());
        for(int x=-3;x<=3;x++) for(int y=119;y<=124;y++) place(level,x,y,6,false);
        reset(p); LivingEntity target=target(level,0,1); OmegaManager.start(p,1,12); steps(2);
        check("creature_contact_does_not_end_flight",OmegaManager.owns(p)); check("no_direct_contact_damage",target.m_21223_()==500); target.m_146870_();
    }
    private void resourceCases() throws Exception {
        try(var stream=OmegaMod.class.getResourceAsStream("/assets/irons_omega_rush/player_animation/omega_rush_flight.json")) {
            check("flight_pose_resource_present",stream!=null);
            var data=dev.kosmx.playerAnim.core.data.gson.AnimationSerializing.deserializeAnimation(stream);
            check("native_animation_parser_accepts_pose",data.size()==1&&data.get(0).isInfinite());
            check("pose_has_six_bones",data.get(0).getBodyParts().size()>=6);
            check("renderer_arm_and_leg_channels_enabled",List.of("rightArm","leftArm","rightLeg","leftLeg").stream()
                .allMatch(name->data.get(0).getPart(name)!=null&&data.get(0).getPart(name).pitch.isEnabled()));
        }
        var encode=OmegaInputPacket.class.getDeclaredMethod("encode",OmegaInputPacket.class,net.minecraft.network.FriendlyByteBuf.class);
        var decode=OmegaInputPacket.class.getDeclaredMethod("decode",net.minecraft.network.FriendlyByteBuf.class);
        encode.setAccessible(true); decode.setAccessible(true);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var input=new OmegaInputPacket(123,4,90,-45,true); encode.invoke(null,input,buffer);
            check("input_payload_size",buffer.readableBytes()==21); check("input_codec_roundtrip_with_cancel",input.equals(decode.invoke(null,buffer)));
            buffer.clear(); encode.invoke(null,input,buffer); buffer.writeByte(0); boolean rejected=false;
            try { decode.invoke(null,buffer); } catch(java.lang.reflect.InvocationTargetException error) { rejected=error.getCause() instanceof IllegalArgumentException; }
            check("oversized_input_rejected",rejected);
            buffer.clear(); buffer.writeLong(123); rejected=false;
            try { decode.invoke(null,buffer); } catch(java.lang.reflect.InvocationTargetException error) { rejected=error.getCause() instanceof IllegalArgumentException; }
            check("truncated_input_rejected",rejected);
        } finally { buffer.release(); }
    }
    private void lifecycleCases(ServerLevel level,ServerPlayer p) throws Exception {
        reset(p); OmegaManager.charge(p,1,16); OmegaManager.cancelCharge(p); check("interrupted_charge_no_flight_or_trail",state(p)==null&&map("TRAILS").isEmpty());
        reset(p); OmegaManager.start(p,1,12); steps(3); check("pending_trail_exists",!map("TRAILS").isEmpty()); OmegaManager.externalTeleport(p);
        check("teleport_purges_state_and_nodes",!OmegaManager.owns(p)&&map("TRAILS").isEmpty()&&!p.m_20068_());
        reset(p); p.m_20242_(true); OmegaManager.start(p,1,12); OmegaManager.abort(p); check("preexisting_gravity_restored",p.m_20068_());
        reset(p); OmegaManager.start(p,1,12); steps(3); OmegaManager.logout(new PlayerEvent.PlayerLoggedOutEvent(p)); check("logout_purges",state(p)==null&&map("TRAILS").isEmpty());
        reset(p); OmegaManager.start(p,1,12); OmegaManager.death(new LivingDeathEvent(p,ModSpells.OMEGA_RUSH.get().getDamageSource(p))); check("death_purges",state(p)==null&&!p.m_20068_());
        p.getPersistentData().m_128379_("irons_omega_rush.originalGravity",false); p.m_20242_(true); OmegaManager.login(new PlayerEvent.PlayerLoggedInEvent(p));
        check("login_recovers_gravity",!p.m_20068_()&&!p.getPersistentData().m_128441_("irons_omega_rush.originalGravity"));
        reset(p); OmegaManager.start(p,1,12); p.m_6034_(20,120,0); steps(1); check("unannounced_teleport_aborts",!OmegaManager.owns(p));
        reset(p); check("unsafe_build_height",!OmegaCollision.safe(level,new AABB(0,1000,0,1,1002,1)));
        check("unloaded_boundary",!OmegaCollision.safe(level,new AABB(100000,120,100000,100001,122,100001)));
        OmegaManager.start(p,1,12); steps(140); steps(101); LivingFallEvent late=new LivingFallEvent(p,20,1); OmegaManager.fall(late); check("landing_expiry",!late.isCanceled());
        List<ServerPlayer> group=new ArrayList<>();
        for(int i=0;i<8;i++) { ServerPlayer other=player(level,"OmegaMulti"+i,i*5,0); group.add(other); check("simultaneous_start_"+i,OmegaManager.start(other,1,12)); }
        long before=System.nanoTime(); steps(30); double millis=(System.nanoTime()-before)/1e6/30;
        check("eight_sessions_advance",group.stream().allMatch(OmegaManager::owns));
        System.out.println("OMEGA_TEST_PERF eight_caster_explicit_tick_ms="+millis);
        for(ServerPlayer other:group) OmegaManager.abort(other);
    }
    private void damageCases(ServerLevel level,ServerPlayer p) throws Exception {
        reset(p); LivingEntity wider=target(level,4.35,2); LivingEntity outside=target(level,4.6,2);
        OmegaManager.start(p,1,12); steps(8);
        check("wider_radius_hits_beyond_old_radius",wider.m_21223_()<500);
        check("wider_radius_excludes_beyond_4_05",outside.m_21223_()==500);
        wider.m_146870_(); outside.m_146870_();
        reset(p); LivingEntity diagonal=target(level,4.0,6.0); OmegaManager.start(p,1,12); steps(8);
        check("spherical_radius_excludes_box_corner",diagonal.m_21223_()==500); diagonal.m_146870_();
        reset(p); LivingEntity target=target(level,0,4); float casterHealth=p.m_21223_(); OmegaManager.start(p,1,12);
        steps(7); check("five_tick_delay_no_early_damage",target.m_21223_()==500); steps(1);
        check("trail_detonation_damage",target.m_21223_()<500); float after=target.m_21223_(); steps(3);
        check("overlap_hit_cooldown",target.m_21223_()==after); check("caster_excluded",p.m_21223_()==casterHealth);
        check("terrain_intact_under_trail",level.m_8055_(new BlockPos(0,120,4)).m_60795_()); target.m_146870_();
        reset(p); ServerPlayer victim=player(level,"OmegaVictim",0,4);
        boolean prior=OmegaConfig.PVP.get(); OmegaConfig.PVP.set(false); OmegaManager.start(p,1,12); steps(15); check("pvp_config_off",victim.m_21223_()==500); OmegaConfig.PVP.set(prior); victim.m_146870_();
        reset(p); LivingEntity protectedTarget=target(level,0,4);
        java.util.function.Consumer<LivingHurtEvent> cancel=e->{if(e.getEntity()==protectedTarget)e.setCanceled(true);};
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try { OmegaManager.start(p,1,12); steps(15); check("damage_protection_event_respected",protectedTarget.m_21223_()==500); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); protectedTarget.m_146870_(); }
        reset(p); double oldSpeed=OmegaConfig.SPEED.get(),oldSpacing=OmegaConfig.SPACING.get();
        // Apothic Attributes can randomly multiply any spell hit by 1.5 in the copied pack.
        // Only this fixture's synthetic caster suppresses critical hits for exact-health assertions.
        var critAttribute=ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_chance"));
        var crit=critAttribute==null?null:p.m_21051_(critAttribute);
        double oldCrit=crit==null?0:crit.m_22115_();
        LivingEntity repeat=target(level,-1,1); repeat.m_21051_(Attributes.f_22278_).m_22100_(1);
        try {
            if(crit!=null) crit.m_22100_(0);
            OmegaConfig.SPEED.set(0.1); OmegaConfig.SPACING.set(1.0); OmegaManager.start(p,1,12); long sid=session(p);
            for(int i=0;i<100;i++) { OmegaManager.input(p,new OmegaInputPacket(sid,i+1,i*6,0,false)); steps(1); }
            Object repeatHits=((Map<?,?>)field(map("TRAILS").get(sid),"hits")).get(repeat.m_20148_());
            System.out.println("OMEGA_TEST_REPEAT health="+repeat.m_21223_()+" hits="+(repeatHits==null?0:field(repeatHits,"count"))+" position="+repeat.m_20182_());
            check("three_hits_per_cast_cap",repeatHits!=null&&(int)field(repeatHits,"count")==3&&Math.abs(repeat.m_21223_()-464)<0.01);
            float health=repeat.m_21223_();
            for(int i=100;i<130;i++) { OmegaManager.input(p,new OmegaInputPacket(sid,i+1,i*6,0,false)); steps(1); }
            check("later_nodes_cannot_exceed_hit_cap",repeat.m_21223_()==health);
        } finally { OmegaManager.abort(p); OmegaConfig.SPEED.set(oldSpeed); OmegaConfig.SPACING.set(oldSpacing); if(crit!=null) crit.m_22100_(oldCrit); repeat.m_146870_(); }
    }
    private void extraCases(ServerLevel level,ServerPlayer p) throws Exception {
        reset(p); p.m_6075_();
        var fluidUpdate=Entity.class.getDeclaredMethod("m_20073_"); fluidUpdate.setAccessible(true);
        for(boolean lava:new boolean[]{false,true}) {
            p.m_6034_(0.5,120,0.5); fluidUpdate.invoke(p); OmegaManager.start(p,1,12);
            level.m_7731_(new BlockPos(0,120,0),(lava?Blocks.f_49991_:Blocks.f_49990_).m_49966_(),3); fluidUpdate.invoke(p);
            check((lava?"lava":"water")+"_occupancy",lava?p.m_20077_():p.m_20069_()); steps(1);
            check((lava?"lava":"water")+"_aborts",!OmegaManager.owns(p)&&!p.m_20068_()&&map("TRAILS").isEmpty());
            place(level,0,120,0,false); fluidUpdate.invoke(p);
        }
        reset(p); p.m_146926_(90); place(level,0,119,0,true); OmegaManager.start(p,1,12); steps(1);
        check("floor_contact_stops_without_blast",!OmegaManager.owns(p)&&map("TRAILS").isEmpty()); place(level,0,119,0,false);
        reset(p); p.m_146926_(-90); place(level,0,123,0,true); OmegaManager.start(p,1,12); steps(3);
        check("ceiling_stops_body",!OmegaManager.owns(p)&&p.m_20186_()<121.21); place(level,0,123,0,false);
        reset(p); OmegaManager.start(p,1,12);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(p,level.m_46472_(),net.minecraft.world.level.Level.f_46429_));
        check("dimension_event_purges",state(p)==null&&map("TRAILS").isEmpty()&&!p.m_20068_());
        reset(p); var reduction=p.m_21051_(AttributeRegistry.CAST_TIME_REDUCTION.get()); double priorReduction=reduction.m_22115_();
        try { int original=ModSpells.OMEGA_RUSH.get().getEffectiveCastTime(1,p); reduction.m_22100_(priorReduction+0.5); check("native_charge_attribute_scaling",ModSpells.OMEGA_RUSH.get().getEffectiveCastTime(1,p)<original); }
        finally { reduction.m_22100_(priorReduction); }
        var nature=p.m_21051_(AttributeRegistry.NATURE_SPELL_POWER.get()); double oldNature=nature.m_22115_();
        try { float original=ModSpells.OMEGA_RUSH.get().getSpellPower(1,p); nature.m_22100_(oldNature+1); float power=ModSpells.OMEGA_RUSH.get().getSpellPower(1,p); OmegaManager.start(p,1,power); check("nature_power_captured_once",power>original&&Math.abs((float)field(state(p),"damage")-power)<0.001); }
        finally { OmegaManager.abort(p); nature.m_22100_(oldNature); }
    }
    private void paidCast(ServerLevel level,ServerPlayer p,AbstractSpell spell) {
        reset(p); p.m_21051_(AttributeRegistry.MAX_MANA.get()).m_22100_(1000); var magic=MagicData.getPlayerMagicData(p); magic.setMana(500); magic.getPlayerCooldowns().clearCooldowns();
        spell.onServerPreCast(level,1,p,magic); spell.castSpell(level,1,p,CastSource.SPELLBOOK,true); spell.onServerCastComplete(level,1,p,magic,false);
        System.out.println("OMEGA_TEST_PAID mana="+magic.getMana()+" configuredCost="+spell.getManaCost(1)+" owns="+OmegaManager.owns(p));
        check("native_mana_deducted_once",Math.abs(magic.getMana()-300)<0.01&&OmegaManager.owns(p)); check("native_cooldown",magic.getPlayerCooldowns().isOnCooldown(spell));
        OmegaManager.abort(p); magic.setMana(0); magic.getPlayerCooldowns().clearCooldowns(); check("zero_mana_rejected",spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).type==CastResult.Type.FAILURE);
    }
    private void coexistenceCases(ServerLevel level,ServerPlayer p) throws Exception {
        reset(p); p.getPersistentData().m_128379_("irons_ultimate_explosion.thundercrash.originalGravity",false);
        check("thundercrash_ownership_rejected",!OmegaManager.eligible(p)); p.getPersistentData().m_128473_("irons_ultimate_explosion.thundercrash.originalGravity");
        if(net.minecraftforge.fml.ModList.get().isLoaded("irons_ultimate_explosion")) {
            Class<?> movement=Class.forName("local.ironsultimateexplosion.ThundercrashMovement");
            var step=movement.getMethod("step",Vec3.class,Vec3.class,double.class,double.class,boolean.class,double.class);
            Vec3 ours=new Vec3(0,0,0.8),theirs=ours;
            for(int i=0;i<100;i++) { Vec3 aim=OmegaMovement.look(i*7,i%40-20); ours=OmegaMovement.step(ours,aim,0.8,0.18); theirs=(Vec3)step.invoke(null,theirs,aim,0.8,0.18,false,0.35); }
            check("actual_thundercrash_handling_parity",ours.m_82554_(theirs)<1e-20);
            AbstractSpell thunder=SpellRegistry.getSpell("irons_ultimate_explosion:thundercrash");
            reset(p); OmegaManager.start(p,1,12);
            var event=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(p,thunder.getSpellId(),1,thunder.getSchoolType(),CastSource.SPELLBOOK);
            check("omega_blocks_thundercrash_native_precast",MinecraftForge.EVENT_BUS.post(event)); OmegaManager.abort(p);
            Class<?> manager=Class.forName("local.ironsultimateexplosion.ThundercrashManager");
            var start=manager.getMethod("start",ServerPlayer.class,int.class,float.class); var abort=manager.getMethod("abort",ServerPlayer.class);
            try {
                check("actual_thundercrash_start",(boolean)start.invoke(null,p,1,40f));
                check("actual_thundercrash_blocks_omega",!OmegaManager.start(p,1,12));
                event=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(p,OmegaSpell.ID.toString(),1,SchoolRegistry.NATURE.get(),CastSource.SPELLBOOK);
                check("thundercrash_blocks_omega_native_precast",MinecraftForge.EVENT_BUS.post(event));
            } finally { abort.invoke(null,p); }
        } else System.out.println("OMEGA_TEST_PENDING actual_thundercrash_coexistence");
    }
    private void forgeCases(ServerLevel level,ServerPlayer player,AbstractSpell spell) {
        check("native_nature_listing",SpellRegistry.getSpellsForSchool(SchoolRegistry.NATURE.get()).contains(spell));
        Item potato=ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft","poisonous_potato"));
        ItemStack focus=new ItemStack(potato);
        check("poisonous_potato_focus",SchoolRegistry.getSchoolsFromFocus(focus).contains(SchoolRegistry.NATURE.get()));
        BlockPos forgePos=new BlockPos(3,120,-3),tablePos=new BlockPos(3,120,-2);
        level.m_7731_(forgePos,io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get().m_49966_(),3);
        try {
            var tile=(io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile)level.m_7702_(forgePos);
            var menu=(io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu)tile.m_7208_(7,player.m_150109_(),player);
            ItemStack ink=new ItemStack(io.redspace.ironsspellbooks.item.InkItem.getInkForRarity(SpellRarity.LEGENDARY)),paper=new ItemStack(Items.f_42516_);
            check("native_forge_accepts_ingredients",menu.getInkSlot().m_5857_(ink)&&menu.getBlankScrollSlot().m_5857_(paper)&&menu.getFocusSlot().m_5857_(focus));
            tile.getItemHandler().setStackInSlot(0,ink); tile.getItemHandler().setStackInSlot(1,paper); tile.getItemHandler().setStackInSlot(2,focus); menu.setRecipeSpell(spell);
            ItemStack scroll=menu.getResultSlot().m_7993_().m_41777_(); var data=ISpellContainer.get(scroll);
            check("native_forge_spell_and_level",!scroll.m_41619_()&&data.getSpellAtIndex(0).getSpell()==spell&&data.getSpellAtIndex(0).getLevel()==spell.getMinLevelForRarity(SpellRarity.LEGENDARY));
            menu.getResultSlot().m_6201_(1); menu.getResultSlot().m_142406_(player,scroll);
            check("native_forge_consumes_ingredients",tile.getStackInSlot(0).m_41619_()&&tile.getStackInSlot(1).m_41619_()&&tile.getStackInSlot(2).m_41619_());
            level.m_7731_(tablePos,io.redspace.ironsspellbooks.registries.BlockRegistry.INSCRIPTION_TABLE_BLOCK.get().m_49966_(),3);
            var table=new io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu(8,player.m_150109_(),net.minecraft.world.inventory.ContainerLevelAccess.m_39289_(level,tablePos));
            ItemStack book=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.IRON_SPELL_BOOK.get());
            check("native_inscription_accepts",table.getSpellBookSlot().m_5857_(book)&&table.getScrollSlot().m_5857_(scroll));
            int rank=data.getSpellAtIndex(0).getLevel(); table.getSpellBookSlot().m_5852_(book); table.getScrollSlot().m_5852_(scroll); table.m_6366_(player,0); table.m_6366_(player,-1);
            var inscribed=ISpellContainer.get(table.getSpellBookSlot().m_7993_());
            check("native_inscription_spell_and_level",inscribed.getSpellAtIndex(0).getSpell()==spell&&inscribed.getSpellAtIndex(0).getLevel()==rank);
            check("native_inscription_consumes_scroll",table.getScrollSlot().m_7993_().m_41619_());
        } finally { level.m_7731_(forgePos,Blocks.f_50016_.m_49966_(),3); level.m_7731_(tablePos,Blocks.f_50016_.m_49966_(),3); }
    }
}

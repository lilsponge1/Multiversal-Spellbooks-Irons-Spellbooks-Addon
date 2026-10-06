package local.omegarush;
import java.util.*;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.*;
public final class OmegaFormManager {
    public static final class DamageFrame {public final float before;public float nested;public DamageFrame(float before){this.before=before;}}
    static final class State {
        final ServerPlayer p; final long session; final String dimension; final int chargeTicks;
        boolean active,hover; int age,sequence=-1,inputTick,packets;long receivedTick;
        float yaw,pitch,forward,strafe,vertical;Vec3 position,velocity=Vec3.f_82478_;OmegaFormBuffs buffs;CastSource source=CastSource.SPELLBOOK;
        State(ServerPlayer p,long s,int c){this.p=p;session=s;chargeTicks=c;dimension=OmegaManager.dimension(p);position=p.m_20182_();yaw=p.m_146908_();}
        OmegaFormPacket packet(byte phase,boolean reset){return new OmegaFormPacket(p.m_20148_(),p.m_19879_(),dimension,session,tick,phase,hover,age,chargeTicks,sequence,p.m_20182_(),velocity,OmegaConfig.HOVER_SPEED.get()/20,OmegaConfig.HOVER_VERTICAL.get()/20,OmegaGravity.original(p),reset,phase==2&&!STATES.containsKey(p.m_20148_()));}
    }
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final Map<UUID,Long> LANDINGS=new HashMap<>();
    private static long next=1,tick;
    public static boolean active(Entity p){
        if(p.m_9236_().f_46443_)return Boolean.TRUE.equals(net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->OmegaFormClient.active(p)));
        State s=STATES.get(p.m_20148_());return s!=null&&s.active;
    }
    public static boolean owns(Entity p){State s=STATES.get(p.m_20148_());return s!=null&&s.active&&s.hover&&!OmegaManager.owns(p);}
    public static boolean eligible(ServerPlayer p){return FloweryScarf.equipped(p)&&!STATES.containsKey(p.m_20148_())&&p.m_6084_()&&!p.m_5833_()&&!OmegaManager.owns(p)&&!p.m_21023_(MobEffectRegistry.ASCENSION.get())&&!p.getPersistentData().m_128441_("irons_ultimate_explosion.thundercrash.originalGravity");}
    public static boolean canActivate(ServerPlayer p){State s=STATES.get(p.m_20148_());return FloweryScarf.equipped(p)&&p.m_6084_()&&(s!=null&&!s.active||eligible(p));}
    public static void charge(ServerPlayer p,int c){if(eligible(p)){State s=new State(p,next++,c);STATES.put(p.m_20148_(),s);send(s,(byte)0,false);}}
    public static boolean activate(ServerPlayer p,CastSource source){
        if(!canActivate(p))return false;State s=STATES.get(p.m_20148_());if(s==null){s=new State(p,next++,1);STATES.put(p.m_20148_(),s);}
        s.active=true;s.age=0;s.source=source;s.buffs=new OmegaFormBuffs(p);s.receivedTick=tick;
        p.m_7292_(new net.minecraft.world.effect.MobEffectInstance(OmegaEffects.FORM.get(),-1,0,false,false,true));
        LANDINGS.remove(p.m_20148_());send(s,(byte)1,false);return true;
    }
    public static boolean pay(ServerPlayer p,float amount){
        if(!Float.isFinite(amount)||amount<0)return false;
        var data=MagicData.getPlayerMagicData(p);if(!Float.isFinite(data.getMana())||data.getMana()+0.0001f<amount)return false;
        data.setMana(Math.max(0,data.getMana()-amount));return true;
    }
    private static void syncMana(ServerPlayer p){PacketDistributor.sendToPlayer(p,new SyncManaPacket(MagicData.getPlayerMagicData(p)));}
    public static void cancelCharge(ServerPlayer p){State s=STATES.get(p.m_20148_());if(s!=null&&!s.active)end(p,false);}
    public static void end(ServerPlayer p,boolean cooldown){
        State s=STATES.remove(p.m_20148_());if(s==null)return;
        if(s.buffs!=null)s.buffs.close();OmegaGravity.release(p,"form");
        p.m_21195_(OmegaEffects.FORM.get());
        if(s.hover&&!OmegaManager.owns(p))LANDINGS.put(p.m_20148_(),tick+100);
        s.hover=false;send(s,(byte)2,true);
        var magic=MagicData.getPlayerMagicData(p);
        if(!OmegaManager.owns(p)){OmegaManager.cancelCharge(p);if(ModSpells.OMEGA_RUSH.get().getSpellId().equals(magic.getCastingSpellId()))magic.resetCastingState();}
        if(s.active){
            var recasts=magic.getPlayerRecasts();if(recasts.hasRecastForSpell(ModSpells.OMEGA_FORM.get()))recasts.removeRecast(recasts.getRecastInstance(ModSpells.OMEGA_FORM.get().getSpellId()),RecastResult.USER_CANCEL);
            if(cooldown)MagicHelper.MAGIC_MANAGER.addCooldown(p,ModSpells.OMEGA_FORM.get(),s.source);
        }
        syncMana(p);
    }
    public static void pauseForRush(ServerPlayer p){State s=STATES.get(p.m_20148_());if(s!=null&&s.active){s.hover=false;OmegaGravity.release(p,"form");send(s,(byte)1,true);}}
    public static void resumeAfterRush(ServerPlayer p){State s=STATES.get(p.m_20148_());if(s!=null&&s.active){s.position=p.m_20182_();s.velocity=Vec3.f_82478_;s.receivedTick=tick;send(s,(byte)1,true);}}
    public static void input(ServerPlayer p,OmegaFormInputPacket a){
        State s=STATES.get(p.m_20148_());if(s==null||!s.active||s.session!=a.session()||a.sequence()<=s.sequence||a.sequence()<0)return;
        if(!Float.isFinite(a.yaw())||!Float.isFinite(a.pitch())||!Float.isFinite(a.forward())||!Float.isFinite(a.strafe())||!Float.isFinite(a.vertical())||Math.abs(a.yaw())>1e7||Math.abs(a.pitch())>90||Math.abs(a.forward())>1||Math.abs(a.strafe())>1||Math.abs(a.vertical())>1)return;
        if(s.inputTick!=(int)tick){s.inputTick=(int)tick;s.packets=0;}if(++s.packets>2)return;
        s.sequence=a.sequence();s.receivedTick=tick;s.yaw=a.yaw()%360;s.pitch=a.pitch();s.forward=a.forward();s.strafe=a.strafe();s.vertical=a.vertical();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;tick++;
        for(State s:new ArrayList<>(STATES.values())){
            try{advance(s);}catch(RuntimeException error){System.err.println("Omega Form stopped: "+error);end(s.p,true);}
        }
        LANDINGS.entrySet().removeIf(e->e.getValue()<tick);
    }
    private static void advance(State s){
        ServerPlayer p=s.p;
        if(!p.m_6084_()||p.m_213877_()||p.m_5833_()||!s.dimension.equals(OmegaManager.dimension(p))||!FloweryScarf.equipped(p)){end(p,true);return;}
        if(!s.active){if(++s.age>s.chargeTicks+20)end(p,false);else if(tick%4==0)send(s,(byte)0,false);return;}
        if(p.m_150110_().f_35935_||p.m_21023_(MobEffectRegistry.ASCENSION.get())||p.getPersistentData().m_128441_("irons_ultimate_explosion.thundercrash.originalGravity")){end(p,true);return;}
        if(!p.m_21023_(OmegaEffects.FORM.get())){end(p,true);return;}
        if(!pay(p,(float)(OmegaConfig.FORM_UPKEEP.get()/20))){end(p,true);return;}
        s.buffs.tick();s.age++;if(tick%5==0)syncMana(p);
        if(OmegaManager.owns(p)){s.position=p.m_20182_();if(tick%10==0)send(s,(byte)1,false);return;}
        boolean blocked=p.m_20159_()||p.m_20069_()||p.m_20077_()||p.m_6147_()||p.m_5803_()||p.m_5833_();
        boolean ground=p.m_20096_()||!p.m_9236_().m_45756_(p,p.m_20191_().m_82386_(0,-0.05,0));
        boolean hover=!blocked&&(!ground||s.vertical>0);
        boolean changed=hover!=s.hover;s.hover=hover;
        if(hover){
            if(tick-s.receivedTick>40){end(p,true);return;}if(tick-s.receivedTick>20){s.forward=s.strafe=s.vertical=0;}
            OmegaGravity.acquire(p,"form");
            if(p.m_20182_().m_82554_(s.position)>4){s.position=p.m_20182_();s.velocity=Vec3.f_82478_;}
            s.velocity=OmegaHover.step(s.velocity,s.yaw,s.forward,s.strafe,s.vertical,OmegaConfig.HOVER_SPEED.get()/20,OmegaConfig.HOVER_VERTICAL.get()/20);
            if(!OmegaCollision.safe(p.m_9236_(),p.m_20191_().m_82369_(s.velocity))){end(p,true);return;}
            p.m_146922_(s.yaw);p.m_146926_(s.pitch);p.m_20256_(s.velocity);p.m_6478_(MoverType.SELF,s.velocity);p.m_183634_();
            s.position=p.m_20182_();p.m_284548_().m_7726_().m_8385_(p);
        }else{OmegaGravity.release(p,"form");s.velocity=Vec3.f_82478_;s.position=p.m_20182_();}
        if(changed||tick%2==0)send(s,(byte)1,changed);
    }
    public static void teleport(ServerPlayer p){State s=STATES.get(p.m_20148_());if(s!=null&&s.active){s.hover=false;s.velocity=Vec3.f_82478_;s.receivedTick=tick;OmegaGravity.release(p,"form");send(s,(byte)1,true);}}
    private static void send(State s,byte phase,boolean reset){OmegaNetwork.form(s.p,s.packet(phase,reset));}
    public static void absorptionConsumed(LivingEntity p,float n){State s=STATES.get(p.m_20148_());if(s!=null&&s.buffs!=null)s.buffs.consumed(n);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent e){if(active(e.getEntity()))OmegaFormBuffs.resistance(e);}
    @SubscribeEvent public static void fall(LivingFallEvent e){if(owns(e.getEntity())||LANDINGS.remove(e.getEntity().m_20148_())!=null)e.setCanceled(true);}
    @SubscribeEvent public static void otherCast(SpellPreCastEvent e){
        if(active(e.getEntity())&&(e.getSpellId().contains("thundercrash")||e.getSpellId().contains("ascension")))e.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)end(p,true);}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)end(p,true);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)end(p,true);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)OmegaGravity.recover(p);}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){State s=STATES.get(e.getTarget().m_20148_());if(s!=null&&e.getEntity() instanceof ServerPlayer p)OmegaNetwork.formTo(p,s.packet(s.active?(byte)1:(byte)0,false));}
    @SubscribeEvent public static void untracking(PlayerEvent.StopTracking e){State s=STATES.get(e.getTarget().m_20148_());if(s!=null&&e.getEntity() instanceof ServerPlayer p)OmegaNetwork.formTo(p,s.packet((byte)2,true));}
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){for(State s:new ArrayList<>(STATES.values()))end(s.p,true);LANDINGS.clear();}
    private OmegaFormManager(){}
}

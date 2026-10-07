package local.omegarush;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.*;
import java.util.*;
public final class OmegaFormSpell extends AbstractSpell {
    private static final DefaultConfig DEFAULT=new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY).setSchoolResource(SchoolRegistry.NATURE_RESOURCE).setMaxLevel(1).setCooldownSeconds(120).setAllowCrafting(false).build();
    public OmegaFormSpell(){baseManaCost=200;castTime=128;}
    @Override public ResourceLocation getSpellResource(){return new ResourceLocation(OmegaMod.ID,"omega_form");}
    @Override public DefaultConfig getDefaultConfig(){return DEFAULT;}
    @Override public boolean allowLooting(){return false;}
    @Override public boolean allowCrafting(){return false;}
    @Override public boolean canBeCraftedBy(Player p){return false;}
    @Override public CastType getCastType(){return CastType.LONG;}
    @Override public int getManaCost(int rank){return OmegaConfig.FORM_MANA.get();}
    @Override public int getSpellCooldown(){return OmegaConfig.FORM_COOLDOWN.get()*20;}
    @Override public int getCastTime(int rank){return Math.max(1,(int)Math.round(OmegaConfig.FORM_CHARGE.get()*0.8));}
    @Override public int getEffectiveCastTime(int rank,LivingEntity e){return OmegaFormManager.active(e)?0:Math.max(OmegaFormRitual.MIN_TICKS,super.getEffectiveCastTime(rank,e));}
    @Override public AnimationHolder getCastStartAnimation(){return AnimationHolder.none();}
    @Override public CastResult canBeCastedBy(int rank,CastSource source,MagicData data,Player p){
        if(!FloweryScarf.equipped(p))return failure("message.irons_omega_rush.scarf_required");
        if(OmegaFormManager.active(p))return new CastResult(CastResult.Type.SUCCESS);
        if(p instanceof ServerPlayer s&&!OmegaFormManager.eligible(s))return failure("message.irons_omega_rush.unavailable");
        return super.canBeCastedBy(rank,source,data,p);
    }
    private CastResult failure(String key){return new CastResult(CastResult.Type.FAILURE,Component.m_237115_(key));}
    @Override public boolean checkPreCastConditions(Level l,int rank,LivingEntity e,MagicData data){
        return FloweryScarf.equipped(e)&&(OmegaFormManager.active(e)||(l.f_46443_||e instanceof ServerPlayer p&&OmegaFormManager.eligible(p))&&super.checkPreCastConditions(l,rank,e,data));
    }
    @Override public void onServerPreCast(Level l,int rank,LivingEntity e,MagicData data){
        if(e instanceof ServerPlayer p&&!OmegaFormManager.active(p))OmegaFormManager.charge(p,getEffectiveCastTime(rank,p));
        super.onServerPreCast(l,rank,e,data);
    }
    @Override public void castSpell(Level l,int rank,ServerPlayer p,CastSource source,boolean applyCooldown){
        if(OmegaFormManager.active(p)){OmegaFormManager.end(p,true);return;}
        var data=MagicData.getPlayerMagicData(p);
        if(data.getPlayerCooldowns().isOnCooldown(this)||!FloweryScarf.equipped(p)||!OmegaFormManager.canActivate(p))return;
        if(data.getMana()+0.0001f<getManaCost(rank)&&(!p.m_7500_()||io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_MANA_COST.get())){OmegaFormManager.cancelCharge(p);return;}
        if(!source.consumesMana()&&!OmegaFormManager.pay(p,getManaCost(rank)))return;
        super.castSpell(l,rank,p,source,false);
    }
    @Override public void onCast(Level l,int rank,LivingEntity e,CastSource source,MagicData data){
        if(e instanceof ServerPlayer p&&OmegaFormManager.activate(p,source))
            data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),1,2,Integer.MAX_VALUE,source,null),data);
        super.onCast(l,rank,e,source,data);
    }
    @Override public void onRecastFinished(ServerPlayer p,RecastInstance i,RecastResult r,ICastDataSerializable data){OmegaFormManager.end(p,true);}
    @Override public void onServerCastComplete(Level l,int rank,LivingEntity e,MagicData data,boolean interrupted){
        if(interrupted&&e instanceof ServerPlayer p)OmegaFormManager.cancelCharge(p);
        super.onServerCastComplete(l,rank,e,data,interrupted);
    }
    @Override public List<MutableComponent> getUniqueInfo(int rank,LivingEntity e){return List.of(
        Component.m_237110_("ui.irons_omega_rush.form_cost",getManaCost(rank),OmegaConfig.FORM_UPKEEP.get()),
        Component.m_237110_("ui.irons_omega_rush.form_buffs",OmegaConfig.FORM_RESISTANCE.get(),OmegaConfig.FORM_HEARTS.get(),OmegaConfig.FORM_STRENGTH.get(),Math.round(OmegaConfig.NATURE_BONUS.get()*100)));
    }
}

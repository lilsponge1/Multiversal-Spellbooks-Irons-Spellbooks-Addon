package local.omegarush;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.*;
import java.util.List;

public final class OmegaSpell extends AbstractSpell {
    public static final ResourceLocation ID=new ResourceLocation(OmegaMod.ID,"omega_rush");
    private static final DefaultConfig CONFIG=new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(SchoolRegistry.NATURE_RESOURCE).setMaxLevel(5).setCooldownSeconds(120).setAllowCrafting(true).build();
    public OmegaSpell() { baseManaCost=200; baseSpellPower=12; spellPowerPerLevel=3; castTime=16; }
    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return CONFIG; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getManaCost(int level) { return OmegaConfig.MANA.get(); }
    @Override public int getSpellCooldown() { return OmegaConfig.COOLDOWN.get()*20; }
    @Override public int getCastTime(int level) { return OmegaConfig.CHARGE.get(); }
    @Override public float getSpellPower(int level,Entity caster) {
        int rank=Math.max(1,level);
        return (float)(super.getSpellPower(rank,caster)*(OmegaConfig.BASE_POWER.get()+(double)OmegaConfig.POWER_PER_LEVEL.get()*(rank-1))/(12.0+3.0*(rank-1)));
    }
    @Override public AnimationHolder getCastStartAnimation() { return SpellAnimations.PREPARE_CROSS_ARMS; }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        return List.of(Component.m_237110_("ui.irons_omega_rush.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster))),
            Component.m_237110_("ui.irons_omega_rush.duration",OmegaConfig.DURATION.get()/20.0),
            Component.m_237110_("ui.irons_omega_rush.radius",OmegaConfig.RADIUS.get()));
    }
    @Override public CastResult canBeCastedBy(int rank,CastSource source,MagicData data,net.minecraft.world.entity.player.Player player) {
        CastResult normal=super.canBeCastedBy(rank,source,data,player);
        if(player instanceof ServerPlayer p&&!OmegaManager.eligible(p)) return new CastResult(CastResult.Type.FAILURE,Component.m_237115_("message.irons_omega_rush.unavailable"));
        return normal;
    }
    @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData data) {
        return level.f_46443_?caster instanceof net.minecraft.world.entity.player.Player:
            caster instanceof ServerPlayer p&&OmegaManager.eligible(p)&&super.checkPreCastConditions(level,rank,caster,data);
    }
    @Override public void onServerPreCast(Level level,int rank,LivingEntity caster,MagicData data) {
        if(caster instanceof ServerPlayer p) OmegaManager.charge(p,rank,getEffectiveCastTime(rank,caster));
        super.onServerPreCast(level,rank,caster,data);
    }
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data) {
        if(caster instanceof ServerPlayer p) OmegaManager.start(p,rank,getSpellPower(rank,caster));
        super.onCast(level,rank,caster,source,data);
    }
    @Override public void onServerCastComplete(Level level,int rank,LivingEntity caster,MagicData data,boolean interrupted) {
        if(interrupted&&caster instanceof ServerPlayer p) OmegaManager.cancelCharge(p);
        super.onServerCastComplete(level,rank,caster,data,interrupted);
    }
}

package local.omegarush;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
final class OmegaFormBuffs {
    static final UUID NATURE=UUID.fromString("8c8e3469-3b15-41a0-9d57-ac2d3cc5cb1e"),STRENGTH=UUID.fromString("bd2fe0d6-7d62-4c36-b9f6-ae97a0f98114");
    static final MobEffect RESISTANCE=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("minecraft","resistance")),STRONG=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("minecraft","strength"));
    static final TagKey<DamageType> BYPASS=TagKey.m_203882_(ResourceKey.m_135788_(new ResourceLocation("minecraft","damage_type")),new ResourceLocation("minecraft","bypasses_resistance"));
    static final TagKey<DamageType> BYPASS_EFFECTS=TagKey.m_203882_(ResourceKey.m_135788_(new ResourceLocation("minecraft","damage_type")),new ResourceLocation("minecraft","bypasses_effects"));
    private final LivingEntity p;
    float absorption;
    OmegaFormBuffs(LivingEntity p){this.p=p;absorption=OmegaConfig.FORM_HEARTS.get()*2f;p.m_7911_(p.m_6103_()+absorption);tick();}
    static void modifier(AttributeInstance a,UUID id,double value,int operation){
        if(a==null)return;var current=a.m_22111_(id);
        if(current!=null&&Math.abs(current.m_22218_()-value)<1e-9)return;
        a.m_22120_(id);if(value!=0)a.m_22118_(new AttributeModifier(id,"Omega Form",value,AttributeModifier.Operation.values()[operation]));
    }
    void tick(){
        absorption=Math.min(absorption,p.m_6103_());
        var existing=p.m_21124_(STRONG);int level=existing==null?0:existing.m_19564_()+1;
        modifier(p.m_21051_(Attributes.f_22281_),STRENGTH,Math.max(0,OmegaConfig.FORM_STRENGTH.get()-level)*3,0);
        modifier(p.m_21051_(AttributeRegistry.NATURE_SPELL_POWER.get()),NATURE,OmegaConfig.NATURE_BONUS.get(),2);
    }
    void consumed(float amount){absorption=Math.max(0,absorption-Math.max(0,amount));}
    void close(){
        p.m_7911_(Math.max(0,p.m_6103_()-Math.min(absorption,p.m_6103_())));absorption=0;
        modifier(p.m_21051_(Attributes.f_22281_),STRENGTH,0,0);modifier(p.m_21051_(AttributeRegistry.NATURE_SPELL_POWER.get()),NATURE,0,2);
    }
    static void resistance(LivingHurtEvent e){
        if(e.getSource().m_269533_(BYPASS)||e.getSource().m_269533_(BYPASS_EFFECTS))return;
        var nativeEffect=e.getEntity().m_21124_(RESISTANCE);
        double nativeReduction=nativeEffect==null?0:Math.min(1,(nativeEffect.m_19564_()+1)*0.2);
        double wanted=OmegaConfig.FORM_RESISTANCE.get()*0.2;
        if(wanted>nativeReduction&&nativeReduction<1)e.setAmount((float)(e.getAmount()*(1-wanted)/(1-nativeReduction)));
    }
}

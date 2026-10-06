package local.omegarush.mixin;
import local.omegarush.OmegaFormManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
@Mixin(LivingEntity.class)
public abstract class OmegaAbsorptionMixin {
    @Unique private final Deque<OmegaFormManager.DamageFrame> omegaAbsorptionFrames=new ArrayDeque<>();
    @Inject(method="m_6469_",at=@At("HEAD"),remap=false)
    private void omegaAbsorptionStart(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        LivingEntity e=(LivingEntity)(Object)this;if(!e.m_9236_().f_46443_&&(!omegaAbsorptionFrames.isEmpty()||OmegaFormManager.active(e)))omegaAbsorptionFrames.push(new OmegaFormManager.DamageFrame(e.m_6103_()));
    }
    @Inject(method="m_6469_",at=@At("RETURN"),remap=false)
    private void omegaAbsorptionEnd(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        if(omegaAbsorptionFrames.isEmpty())return;LivingEntity e=(LivingEntity)(Object)this;var frame=omegaAbsorptionFrames.pop();
        float loss=Math.max(0,frame.before-e.m_6103_());OmegaFormManager.absorptionConsumed(e,Math.max(0,loss-frame.nested));
        if(!omegaAbsorptionFrames.isEmpty())omegaAbsorptionFrames.peek().nested+=loss;
    }
}

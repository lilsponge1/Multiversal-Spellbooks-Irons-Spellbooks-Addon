package local.omegarush.mixin;
import local.omegarush.OmegaPresentation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerRenderer.class)
public abstract class OmegaArmMixin {
    @Inject(method="m_117770_",at=@At("RETURN"),remap=false)
    private void omegaRightArm(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,CallbackInfo ci){OmegaPresentation.arm((PlayerRenderer)(Object)this,pose,buffers,p,true);}
    @Inject(method="m_117813_",at=@At("RETURN"),remap=false)
    private void omegaLeftArm(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,CallbackInfo ci){OmegaPresentation.arm((PlayerRenderer)(Object)this,pose,buffers,p,false);}
}

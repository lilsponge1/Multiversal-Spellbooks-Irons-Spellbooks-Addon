package local.omegarush.mixin;
import local.omegarush.OmegaManager;
import local.omegarush.OmegaFormManager;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.RelativeMovement;
import java.util.Set;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class OmegaConnectionMixin {
    @Shadow(remap=false) public ServerPlayer f_9743_;
    @Shadow(remap=false) private boolean f_9736_;
    @Shadow(remap=false) private int f_9737_;
    @Inject(method="m_7185_", at=@At(value="INVOKE", target="Lnet/minecraft/network/protocol/PacketUtils;m_131359_(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift=At.Shift.AFTER, remap=false), cancellable=true, remap=false)
    private void omegaMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (OmegaManager.owns(f_9743_)||OmegaFormManager.owns(f_9743_)) ci.cancel();
    }
    @Inject(method="m_9933_", at=@At("HEAD"), remap=false)
    private void omegaFloating(CallbackInfo ci) {
        if (OmegaManager.owns(f_9743_)||OmegaFormManager.owns(f_9743_)) { f_9736_ = false; f_9737_ = 0; }
    }
    @Inject(method="m_9780_", at=@At("HEAD"), remap=false)
    private void omegaTeleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> flags, CallbackInfo ci) {
        OmegaManager.externalTeleport(f_9743_);
        OmegaFormManager.teleport(f_9743_);
    }
}

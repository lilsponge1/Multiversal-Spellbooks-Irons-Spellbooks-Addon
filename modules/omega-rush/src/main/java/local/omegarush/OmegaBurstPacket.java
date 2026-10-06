package local.omegarush;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
public record OmegaBurstPacket(UUID caster,String dimension,long session,int index,Vec3 position,Vec3 direction,float radius,boolean clear) {
    static void encode(OmegaBurstPacket p,FriendlyByteBuf b) {
        b.m_130077_(p.caster); b.m_130070_(p.dimension); b.writeLong(p.session).writeInt(p.index);
        OmegaStatePacket.vector(b,p.position); OmegaStatePacket.vector(b,p.direction); b.writeFloat(p.radius).writeBoolean(p.clear);
    }
    static OmegaBurstPacket decode(FriendlyByteBuf b) {
        return new OmegaBurstPacket(b.m_130259_(),b.m_130136_(128),b.readLong(),b.readInt(),OmegaStatePacket.vector(b),OmegaStatePacket.vector(b),b.readFloat(),b.readBoolean());
    }
    static void handle(OmegaBurstPacket p,Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get(); c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->OmegaClient.burst(p))); c.setPacketHandled(true);
    }
}

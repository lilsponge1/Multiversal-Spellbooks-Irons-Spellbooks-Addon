package local.omegarush;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
public record OmegaInputPacket(long session,int sequence,float yaw,float pitch,boolean cancel) {
    static void encode(OmegaInputPacket p,FriendlyByteBuf b) { b.writeLong(p.session).writeInt(p.sequence).writeFloat(p.yaw).writeFloat(p.pitch).writeBoolean(p.cancel); }
    static OmegaInputPacket decode(FriendlyByteBuf b) {
        if(b.readableBytes()!=21) throw new IllegalArgumentException("Omega input must be exactly 21 bytes");
        return new OmegaInputPacket(b.readLong(),b.readInt(),b.readFloat(),b.readFloat(),b.readBoolean());
    }
    static void handle(OmegaInputPacket p,Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get(); c.enqueueWork(()->{if(c.getSender()!=null) OmegaManager.input(c.getSender(),p);}); c.setPacketHandled(true);
    }
}

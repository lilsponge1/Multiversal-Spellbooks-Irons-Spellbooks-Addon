package local.omegarush;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record OmegaFormInputPacket(long session,int sequence,float yaw,float pitch,float forward,float strafe,float vertical){
    static void encode(OmegaFormInputPacket p,FriendlyByteBuf b){b.writeLong(p.session).writeInt(p.sequence).writeFloat(p.yaw).writeFloat(p.pitch).writeFloat(p.forward).writeFloat(p.strafe).writeFloat(p.vertical);}
    static OmegaFormInputPacket decode(FriendlyByteBuf b){if(b.readableBytes()!=32)throw new IllegalArgumentException("Invalid Form input size");return new OmegaFormInputPacket(b.readLong(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
    static void handle(OmegaFormInputPacket p,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->{if(c.getSender()!=null)OmegaFormManager.input(c.getSender(),p);});c.setPacketHandled(true);}
}

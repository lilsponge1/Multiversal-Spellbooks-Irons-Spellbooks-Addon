package local.ironsultimateexplosion;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ThundercrashInputPacket(long session, int sequence, float yaw, float pitch) {
    static void encode(ThundercrashInputPacket p, FriendlyByteBuf b) { b.writeLong(p.session).writeInt(p.sequence).writeFloat(p.yaw).writeFloat(p.pitch); }
    static ThundercrashInputPacket decode(FriendlyByteBuf b) {
        if (b.readableBytes() != 20) throw new IllegalArgumentException("Thundercrash look input must contain exactly 20 bytes");
        return new ThundercrashInputPacket(b.readLong(), b.readInt(), b.readFloat(), b.readFloat());
    }
    static void handle(ThundercrashInputPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get();
        c.enqueueWork(() -> { if (c.getSender() != null) ThundercrashManager.input(c.getSender(), p); });
        c.setPacketHandled(true);
    }
}

package local.ironsultimateexplosion;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SaiyanInputPacket(byte action, int slot) {
    public static final byte PRESS = 0, RELEASE = 1, KEEP_HELD = 2, CANCEL = 3;
    static void encode(SaiyanInputPacket p, FriendlyByteBuf b) { b.writeByte(p.action).writeInt(p.slot); }
    static SaiyanInputPacket decode(FriendlyByteBuf b) {
        if (b.readableBytes() != 5) throw new IllegalArgumentException("Invalid Saiyan input size");
        return new SaiyanInputPacket(b.readByte(), b.readInt());
    }
    static void handle(SaiyanInputPacket p, Supplier<NetworkEvent.Context> context) {
        var c = context.get(); c.enqueueWork(() -> { if (c.getSender() != null) SaiyanManager.input(c.getSender(), p); });
        c.setPacketHandled(true);
    }
}

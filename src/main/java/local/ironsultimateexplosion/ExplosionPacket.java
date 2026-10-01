package local.ironsultimateexplosion;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ExplosionPacket(String dimension, double x, double y, double z, int radius, long seed,
                              byte phase, int duration) {
    static final byte CHARGE = 0;
    static final byte DETONATE = 1;
    static final byte CANCEL = 2;

    static void encode(ExplosionPacket packet, FriendlyByteBuf buffer) {
        buffer.m_130070_(packet.dimension);
        buffer.writeDouble(packet.x).writeDouble(packet.y).writeDouble(packet.z);
        buffer.writeInt(packet.radius).writeLong(packet.seed);
        buffer.writeByte(packet.phase).writeInt(packet.duration);
    }

    static ExplosionPacket decode(FriendlyByteBuf buffer) {
        return new ExplosionPacket(buffer.m_130277_(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(), buffer.readLong(), buffer.readByte(), buffer.readInt());
    }

    static void handle(ExplosionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.add(packet)));
        context.setPacketHandled(true);
    }
}

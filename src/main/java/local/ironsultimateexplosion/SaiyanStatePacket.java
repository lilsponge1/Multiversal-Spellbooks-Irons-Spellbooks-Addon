package local.ironsultimateexplosion;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SaiyanStatePacket(UUID player, int entity, String dimension, long revision, long tick,
        int form, int target, int chargeAge, int chargeDuration, int heldTicks, int holdDuration,
        byte cue, double shakeRadius, float volume,float jumpBonus) {
    public static final byte NONE = 0, CHARGE = 1, BURST = 2, DOWN = 3, DROP = 4, RESET = 5;
    static void encode(SaiyanStatePacket p, FriendlyByteBuf b) {
        b.m_130077_(p.player); b.writeInt(p.entity); b.m_130070_(p.dimension);
        b.writeLong(p.revision).writeLong(p.tick).writeInt(p.form).writeInt(p.target)
                .writeInt(p.chargeAge).writeInt(p.chargeDuration).writeInt(p.heldTicks).writeInt(p.holdDuration)
                .writeByte(p.cue).writeDouble(p.shakeRadius).writeFloat(p.volume).writeFloat(p.jumpBonus);
    }
    static SaiyanStatePacket decode(FriendlyByteBuf b) {
        return new SaiyanStatePacket(b.m_130259_(), b.readInt(), b.m_130136_(128), b.readLong(), b.readLong(),
                b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readByte(), b.readDouble(), b.readFloat(),b.readFloat());
    }
    static void handle(SaiyanStatePacket p, Supplier<NetworkEvent.Context> context) {
        var c = context.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SaiyanClient.state(p)));
        c.setPacketHandled(true);
    }
}

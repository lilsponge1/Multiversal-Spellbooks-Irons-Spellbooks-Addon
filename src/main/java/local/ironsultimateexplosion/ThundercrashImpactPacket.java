package local.ironsultimateexplosion;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ThundercrashImpactPacket(UUID caster, String dimension, long session, Vec3 position, float radius) {
    static void encode(ThundercrashImpactPacket p, FriendlyByteBuf b) {
        b.m_130077_(p.caster); b.m_130070_(p.dimension); b.writeLong(p.session);
        ThundercrashStatePacket.vector(b, p.position); b.writeFloat(p.radius);
    }
    static ThundercrashImpactPacket decode(FriendlyByteBuf b) {
        return new ThundercrashImpactPacket(b.m_130259_(), b.m_130136_(128), b.readLong(), ThundercrashStatePacket.vector(b), b.readFloat());
    }
    static void handle(ThundercrashImpactPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ThundercrashClient.impact(p)));
        c.setPacketHandled(true);
    }
}
